package ai;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HealthCheck {
    public static void main(String[] args) {
        File file = new File("tmp/heartbeat");
        
        // 1. If the bot is actively running and updating the file locally, we are healthy.
        if (file.exists() && (System.currentTimeMillis() - file.lastModified() < 60000)) {
            System.exit(0);
        }
        
        // 2. Heartbeat is stale. Prepare the HTTP client to check downstream cloud dependencies.
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // 3. Evaluate Discord status
        if (isUpstreamProviderDown(client, "https://discordstatus.com/api/v2/status.json", "Discord")) {
            System.exit(0); // Fake health success to allow internal reconnection loops to persist
        }

        // 4. Evaluate MongoDB Cloud status
        if (isUpstreamProviderDown(client, "https://status.mongodb.com/api/v2/status.json", "MongoDB Cloud")) {
            System.exit(0); // Fake health success to prevent hammering auth endpoints
        }

        // 5. Upstream dependencies are operational, confirming the bot stalled locally.
        System.out.println("All upstream dependencies operational, but local bot heartbeat is stale. Restarting container.");
        System.exit(1);
    }

    /**
     * Queries a public Statuspage API to verify if a provider is experiencing an outage.
     */
    private static boolean isUpstreamProviderDown(HttpClient client, String apiUrl, String serviceName) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // Intercept major or critical platform outages reported by the service status page
            if (body.contains("\"indicator\":\"major\"") || body.contains("\"indicator\":\"critical\"")) {
                System.out.println(serviceName + " is experiencing a widespread outage. Bypassing container restart loop.");
                return true;
            }
            
        } catch (Exception e) {
            // Fallback: If the API times out or throws connection issues, assume an external 
            // ISP or network routing event rather than a localized code crash.
            System.out.println("Unable to reach " + serviceName + " status endpoint. Assuming connection outage; skipping restart.");
            return true;
        }
        return false;
    }
}