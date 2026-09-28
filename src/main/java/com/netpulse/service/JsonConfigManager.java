package com.netpulse.service;

import com.netpulse.model.Endpoint;
import com.netpulse.model.EndpointType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


    public List<Endpoint> importConfig(Path jsonFile) throws IOException {
        String content = Files.readString(jsonFile, StandardCharsets.UTF_8);
        JSONArray array = new JSONArray(content);
        List<Endpoint> endpoints = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            Endpoint ep = new Endpoint(
                    obj.getString("name"),
                    obj.getString("host"),
                    obj.optInt("port", 80),
                    EndpointType.valueOf(obj.optString("type", "WEBSITE").toUpperCase()));
            ep.setIntervalSeconds(obj.optInt("intervalSeconds", 10));
            endpoints.add(ep);
        }
        return endpoints;
    }

    /** Writes the current list of endpoints out as a JSON array, e.g. for backup/sharing. */
    public void exportConfig(List<Endpoint> endpoints, Path jsonFile) throws IOException {
        JSONArray array = new JSONArray();
        for (Endpoint ep : endpoints) {
            JSONObject obj = new JSONObject();
            obj.put("name", ep.getName());
            obj.put("host", ep.getHost());
            obj.put("port", ep.getPort());
            obj.put("type", ep.getType().name());
            obj.put("intervalSeconds", ep.getIntervalSeconds());
            array.put(obj);
        }
        Files.writeString(jsonFile, array.toString(2), StandardCharsets.UTF_8);
    }

    /**
     * Calls a public JSON API endpoint (e.g. https://worldtimeapi.org/api/ip
     * or any REST health endpoint the user configures) and returns a small,
     * application-friendly summary parsed out of the raw JSON response.
     * Demonstrates URL response handling + structured JSON parsing.
     */
    public ApiProbeSummary probeJsonApi(String url) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = (System.nanoTime() - start) / 1_000_000;

            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            String snippet = "";
            try {
                // Not every API returns a JSON object at the top level (some return an array),
                // so parsing is attempted defensively.
                JSONObject json = new JSONObject(response.body());
                snippet = json.toString().substring(0, Math.min(120, json.toString().length()));
            } catch (Exception notAnObject) {
                snippet = response.body().substring(0, Math.min(120, response.body().length()));
            }

            return new ApiProbeSummary(ok, response.statusCode(), latency, snippet);
        } catch (Exception e) {
            long latency = (System.nanoTime() - start) / 1_000_000;
            return new ApiProbeSummary(false, -1, latency, "Error: " + e.getMessage());
        }
    }

    /** Small immutable holder for a parsed API probe outcome (Java record, Week 1 syntax). */
    public record ApiProbeSummary(boolean success, int statusCode, long latencyMs, String bodySnippet) { }
}
