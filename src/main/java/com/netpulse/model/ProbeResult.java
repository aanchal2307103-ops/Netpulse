package com.netpulse.model;


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
