package ai.Utility;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.tinylog.Logger;

public class HealthUpdate {
    public static void start() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                File heartbeat = new File("tmp/heartbeat");
                if (!heartbeat.exists()) {
                    heartbeat.createNewFile();
                }
            
                // Update the "Last Modified" timestamp to the current time
                heartbeat.setLastModified(System.currentTimeMillis());
            } catch (IOException e) {
                Logger.error("Failed to write health check heartbeat file", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
}
