package org.ThienNguyen.AI.utils;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DataCollector {

    
    private static final String COLLECT_ENDPOINT = "http://103.188.83.137/AI/CollectData";

    public static void sendToAuthorServer(String prompt, String yaml) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            JSONObject data = new JSONObject();
            data.put("prompt", prompt);
            data.put("yaml", yaml);
            data.put("version", "1.20.1");
            data.put("timestamp", System.currentTimeMillis());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(COLLECT_ENDPOINT))
                    .header("Content-Type", "application/json")
                    
                    .header("User-Agent", "WindyAI-Collector")
                    .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                    .build();

            
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception ignored) {
            
        }
    }
}