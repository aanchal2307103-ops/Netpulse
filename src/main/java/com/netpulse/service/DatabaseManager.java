package com.netpulse.service;

import com.netpulse.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Week 6 (Relational Database with SQLite and JavaFX):
 * Handles the JDBC connection to a local SQLite file, table creation, and
 * CRUD-style operations (insert, update, delete, query) for endpoints and
 * their probe history.
 *
 * All public methods are synchronized because they are called concurrently
 * from multiple background probe threads (Week 4: shared-resource safety).
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:netpulse.db";
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Connection connection;

    public DatabaseManager() {
        connect();
        createTables();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            throw new RuntimeException("Could not open SQLite database: " + e.getMessage(), e);
        }
    }

    private void createTables() {
        String endpoints = """
            CREATE TABLE IF NOT EXISTS endpoints (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                host TEXT NOT NULL,
                port INTEGER NOT NULL,
                type TEXT NOT NULL,
                interval_seconds INTEGER NOT NULL DEFAULT 10
            );
            """;
        String logs = """
            CREATE TABLE IF NOT EXISTS health_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                endpoint_id INTEGER NOT NULL,
                status TEXT NOT NULL,
                latency_ms INTEGER NOT NULL,
                message TEXT,
                checked_at TEXT NOT NULL,
                FOREIGN KEY (endpoint_id) REFERENCES endpoints(id) ON DELETE CASCADE
            );
            """;
        try (Statement st = connection.createStatement()) {
            st.execute(endpoints);
            st.execute(logs);
        } catch (SQLException e) {
            throw new RuntimeException("Could not create tables: " + e.getMessage(), e);
        }
    }

    /** INSERT a new endpoint and return the generated id. */
    public synchronized int insertEndpoint(Endpoint ep) {
        String sql = "INSERT INTO endpoints(name, host, port, type, interval_seconds) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ep.getName());
            ps.setString(2, ep.getHost());
            ps.setInt(3, ep.getPort());
            ps.setString(4, ep.getType().name());
            ps.setInt(5, ep.getIntervalSeconds());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    ep.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Insert endpoint failed: " + e.getMessage(), e);
        }
        return -1;
    }

    /** DELETE an endpoint and its history (cascades). */
    public synchronized void deleteEndpoint(int endpointId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM endpoints WHERE id = ?")) {
            ps.setInt(1, endpointId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Delete endpoint failed: " + e.getMessage(), e);
        }
    }

    /** UPDATE the stored interval for an endpoint. */
    public synchronized void updateInterval(int endpointId, int seconds) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE endpoints SET interval_seconds = ? WHERE id = ?")) {
            ps.setInt(1, seconds);
            ps.setInt(2, endpointId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update interval failed: " + e.getMessage(), e);
        }
    }

    /** SELECT all saved endpoints (used at startup to restore the dashboard). */
    public synchronized List<Endpoint> loadEndpoints() {
        List<Endpoint> result = new ArrayList<>();
        String sql = "SELECT id, name, host, port, type, interval_seconds FROM endpoints";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Endpoint ep = new Endpoint(
                        rs.getString("name"),
                        rs.getString("host"),
                        rs.getInt("port"),
                        EndpointType.valueOf(rs.getString("type")));
                ep.setId(rs.getInt("id"));
                ep.setIntervalSeconds(rs.getInt("interval_seconds"));
                result.add(ep);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Load endpoints failed: " + e.getMessage(), e);
        }
        return result;
    }

    /** INSERT one probe result into the history log. */
    public synchronized void logResult(ProbeResult result) {
        String sql = "INSERT INTO health_logs(endpoint_id, status, latency_ms, message, checked_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, result.getEndpointId());
            ps.setString(2, result.getStatus().name());
            ps.setLong(3, result.getLatencyMs());
            ps.setString(4, result.getMessage());
            ps.setString(5, result.getTimestamp());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Log result failed: " + e.getMessage(), e);
        }
    }

    /** SELECT the most recent N latency readings for one endpoint (feeds the trend chart). */
    public synchronized List<Long> recentLatencies(int endpointId, int limit) {
        List<Long> values = new ArrayList<>();
        String sql = "SELECT latency_ms FROM health_logs WHERE endpoint_id = ? " +
                     "ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, endpointId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) values.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fetch latencies failed: " + e.getMessage(), e);
        }
        java.util.Collections.reverse(values); // oldest first, for left-to-right chart plotting
        return values;
    }

    public static String nowFormatted() {
        return LocalDateTime.now().format(TS_FORMAT);
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) { }
    }
}
