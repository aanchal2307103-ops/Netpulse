package com.netpulse.model;

import javafx.beans.property.*;

/**
 * Week 1 (Java Syntax / OOP): a plain-old Java object showing fields, a
 * constructor, encapsulated access via getters/setters, and core OOP design.
 *
 * JavaFX "Property" wrappers are used instead of plain fields so that a
 * TableView can bind directly to this object and refresh automatically
 * whenever a background probe thread updates the status/latency
 * (Week 3: JavaFX controls & data binding).
 */
public class Endpoint {

    private final IntegerProperty id = new SimpleIntegerProperty(this, "id", -1);
    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final StringProperty host = new SimpleStringProperty(this, "host");
    private final IntegerProperty port = new SimpleIntegerProperty(this, "port");
    private final ObjectProperty<EndpointType> type = new SimpleObjectProperty<>(this, "type");
    private final ObjectProperty<EndpointStatus> status = new SimpleObjectProperty<>(this, "status", EndpointStatus.UNKNOWN);
    private final LongProperty lastLatencyMs = new SimpleLongProperty(this, "lastLatencyMs", -1);
    private final StringProperty lastChecked = new SimpleStringProperty(this, "lastChecked", "-");
    private final IntegerProperty intervalSeconds = new SimpleIntegerProperty(this, "intervalSeconds", 10);

    public Endpoint(String name, String host, int port, EndpointType type) {
        this.name.set(name);
        this.host.set(host);
        this.port.set(port);
        this.type.set(type);
    }

    // ---- id ----
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    // ---- name ----
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    // ---- host ----
    public String getHost() { return host.get(); }
    public void setHost(String value) { host.set(value); }
    public StringProperty hostProperty() { return host; }

    // ---- port ----
    public int getPort() { return port.get(); }
    public void setPort(int value) { port.set(value); }
    public IntegerProperty portProperty() { return port; }

    // ---- type ----
    public EndpointType getType() { return type.get(); }
    public void setType(EndpointType value) { type.set(value); }
    public ObjectProperty<EndpointType> typeProperty() { return type; }

    // ---- status ----
    public EndpointStatus getStatus() { return status.get(); }
    public void setStatus(EndpointStatus value) { status.set(value); }
    public ObjectProperty<EndpointStatus> statusProperty() { return status; }

    // ---- lastLatencyMs ----
    public long getLastLatencyMs() { return lastLatencyMs.get(); }
    public void setLastLatencyMs(long value) { lastLatencyMs.set(value); }
    public LongProperty lastLatencyMsProperty() { return lastLatencyMs; }

    // ---- lastChecked ----
    public String getLastChecked() { return lastChecked.get(); }
    public void setLastChecked(String value) { lastChecked.set(value); }
    public StringProperty lastCheckedProperty() { return lastChecked; }

    // ---- intervalSeconds ----
    public int getIntervalSeconds() { return intervalSeconds.get(); }
    public void setIntervalSeconds(int value) { intervalSeconds.set(value); }
    public IntegerProperty intervalSecondsProperty() { return intervalSeconds; }

    @Override
    public String toString() {
        return name.get() + " (" + host.get() + ":" + port.get() + ")";
    }
}
