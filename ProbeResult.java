package com.netpulse.model;

/**
 * Week 1 (OOP basics): an immutable data-carrier object (POJO) produced by a
 * background probe thread and later persisted as a row in the SQLite
 * health_logs table (Week 6).
 */
public class ProbeResult {

    private final int endpointId;
    private final String endpointName;
    private final EndpointStatus status;
    private final long latencyMs;
    private final String timestamp;
    private final String message;

    public ProbeResult(int endpointId, String endpointName, EndpointStatus status,
                        long latencyMs, String timestamp, String message) {
        this.endpointId = endpointId;
        this.endpointName = endpointName;
        this.status = status;
        this.latencyMs = latencyMs;
        this.timestamp = timestamp;
        this.message = message;
    }

    public int getEndpointId() { return endpointId; }
    public String getEndpointName() { return endpointName; }
    public EndpointStatus getStatus() { return status; }
    public long getLatencyMs() { return latencyMs; }
    public String getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
}
