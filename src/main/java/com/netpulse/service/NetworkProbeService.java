package com.netpulse.service;

import com.netpulse.model.*;
import javafx.application.Platform;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;


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
