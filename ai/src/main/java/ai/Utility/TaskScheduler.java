package ai.Utility;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;

import ai.App;
import ai.Database.DocumentUnavailableException;
import ai.ServerSettings;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static HashMap<String, ScheduledTask> tasks = new HashMap<String, ScheduledTask>();
    private static Runnable checkTask(String key) {
        return () -> {
            ScheduledTask scheduledTask = tasks.get(key);
            if (scheduledTask != null) {
                if (scheduledTask.endTime <= Instant.now().getEpochSecond()+3) { // +3 for some tolerance
                    scheduledTask.task.run();
                    tasks.remove(key);
                }
            }
        };
    }

    private static class ScheduledTask {
        public long endTime; // future epoch second
        public Runnable task;

        public ScheduledTask(long endTime, Runnable task) {
            this.endTime = endTime;
            this.task = task;
        }
    }

    public static void scheduleTask(String key, Runnable task, long delay, TimeUnit timeUnit) {
        long endTime = Instant.now().getEpochSecond() + timeUnit.toSeconds(delay);
        tasks.put(key, new ScheduledTask(endTime, task));
        scheduler.schedule(checkTask(key), delay, timeUnit);
    }

    /**
     * Removes task from queue.
     * @param key
     * @return Task or null if key doesn't exist.
     */
    public static ScheduledTask removeTask(String key) {
        return tasks.remove(key);
    }

    /**
     * Attempts to send an error message to either the server log channel or system channel. Will not do anything if fails.
     * @param serverID server to send error message to
     * @param content content
     */
    public static void sendErrorMessage(long serverID, String content) {
        App.api.getServerById(serverID).ifPresent(server -> {
            long errorChannelID;
            try {
                errorChannelID = new ServerSettings(serverID).getLogChannelID().orElse(server.getSystemChannel().orElseThrow().getId());
                new MessageBuilder()
                    .setContent(content)
                    .setAllowedMentions(
                        new AllowedMentionsBuilder()
                            .setMentionUsers(false)
                            .setMentionRoles(false)
                            .setMentionEveryoneAndHere(false)
                            .build())
                    .send(server.getTextChannelById(errorChannelID).orElseThrow());
            } catch (DocumentUnavailableException e) {}
        });
    }
}