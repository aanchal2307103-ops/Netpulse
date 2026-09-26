package com.netpulse.service;

import com.netpulse.model.*;
import javafx.application.Platform;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Week 4 (Java Multithreading and Concurrency Package):
 *
 * Every monitored endpoint gets its own repeating background task, submitted
 * to a shared ScheduledExecutorService (the "Java Concurrency Package"
 * executors, rather than raw Thread/Runnable management). Each task:
 *   1. Opens a TCP socket to (host, port) and times the connection —
 *      the actual "health probe" — entirely off the JavaFX Application Thread,
 *      so the GUI never freezes even while many probes run in parallel.
 *   2. Classifies the result into an EndpointStatus.
 *   3. Hands the ProbeResult back to the UI safely via Platform.runLater,
 *      which is the correct way to touch JavaFX nodes from a background
 *      thread (Week 4: safe background-to-UI communication).
 *
 * A ConcurrentHashMap tracks each endpoint's scheduled future so probing can
 * be started/stopped/rescheduled per endpoint without interfering with the
 * others — demonstrating safe access to a resource shared across threads.
 */
public class NetworkProbeService {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final long DEGRADED_THRESHOLD_MS = 800;

    private final ScheduledExecutorService scheduler;
    private final Map<Integer, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();
    private final Consumer<ProbeResult> onResult;

    /**
     * @param poolSize   number of worker threads available to run probes in parallel
     * @param onResult   callback invoked (already marshalled onto the JavaFX thread)
     *                   whenever a probe completes
     */
    public NetworkProbeService(int poolSize, Consumer<ProbeResult> onResult) {
        this.scheduler = Executors.newScheduledThreadPool(poolSize, runnable -> {
            Thread t = new Thread(runnable, "netpulse-probe-worker");
            t.setDaemon(true);
            return t;
        });
        this.onResult = onResult;
    }

    /** Starts (or restarts) repeating background probing for one endpoint. */
    public void startMonitoring(Endpoint endpoint) {
        stopMonitoring(endpoint.getId());

        Runnable probeTask = () -> {
            ProbeResult result = probeOnce(endpoint);
            // Marshal the result back onto the JavaFX Application Thread.
            Platform.runLater(() -> onResult.accept(result));
        };

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                probeTask, 0, Math.max(2, endpoint.getIntervalSeconds()), TimeUnit.SECONDS);
        activeTasks.put(endpoint.getId(), future);
    }

    /** Cancels the repeating task for one endpoint (e.g. on delete, or before rescheduling). */
    public void stopMonitoring(int endpointId) {
        ScheduledFuture<?> existing = activeTasks.remove(endpointId);
        if (existing != null) existing.cancel(false);
    }

    /** Runs a single, one-off probe immediately (used by a manual "Check Now" button). */
    public Future<ProbeResult> probeOnceAsync(Endpoint endpoint) {
        return scheduler.submit(() -> probeOnce(endpoint));
    }

    /** The actual network I/O: attempt a TCP connection and time it. Runs on a worker thread. */
    private ProbeResult probeOnce(Endpoint endpoint) {
        long start = System.nanoTime();
        EndpointStatus status;
        String message;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()), CONNECT_TIMEOUT_MS);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            status = elapsedMs > DEGRADED_THRESHOLD_MS ? EndpointStatus.DEGRADED : EndpointStatus.ONLINE;
            message = "Connected in " + elapsedMs + " ms";
            return new ProbeResult(endpoint.getId(), endpoint.getName(), status, elapsedMs,
                    DatabaseManager.nowFormatted(), message);
        } catch (IOException timeoutOrRefused) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            return new ProbeResult(endpoint.getId(), endpoint.getName(), EndpointStatus.OFFLINE, elapsedMs,
                    DatabaseManager.nowFormatted(), "Unreachable: " + timeoutOrRefused.getMessage());
        }
    }

    /** Clean shutdown of the whole thread pool, called when the application closes. */
    public void shutdown() {
        activeTasks.values().forEach(f -> f.cancel(false));
        activeTasks.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
