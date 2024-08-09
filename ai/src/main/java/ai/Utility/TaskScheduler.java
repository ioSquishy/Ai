package ai.Utility;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static HashMap<Long, ScheduledTask> tasks = new HashMap<Long, ScheduledTask>();
    private static Runnable checkTask(long key) {
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

    public static void scheduleTask(long key, Runnable task, long delay, TimeUnit timeUnit) {
        long endTime = Instant.now().getEpochSecond() + timeUnit.toSeconds(delay);
        tasks.put(key, new ScheduledTask(endTime, task));
        scheduler.schedule(checkTask(key), delay, timeUnit);
    }

    /**
     * Removes task from queue.
     * @param key
     * @return Task or null if key doesn't exist.
     */
    public static ScheduledTask removeTask(long key) {
        return tasks.remove(key);
    }

    // public static class EmbedDelay {
    //     private final EmbedBuilder embed;

    //     public EmbedDelay()
    // }

    public static class RoleDelay {
        private final long roleId;
        private final User user;
        private final Server server;

        public RoleDelay(long roleId, User user, Server server) {
            this.roleId = roleId;
            this.user = user;
            this.server = server;
        }

        public Runnable addRoleAfter(long delay, TimeUnit unit) {
            return () -> user.addRole(server.getRoleById(roleId).orElseThrow());
        }

        public Runnable removeRoleAfter(long delay, TimeUnit unit) {
            return () -> user.removeRole(server.getRoleById(roleId).orElseThrow());
        }
    }
}