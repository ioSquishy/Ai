package ai.Utility;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.awt.Color;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RoleDelay {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final long roleId;
    private final User user;
    private final Server server;

    public RoleDelay(long roleId, User user, Server server) {
        this.roleId = roleId;
        this.user = user;
        this.server = server;
    }

    public void addRoleAfter(long delay, TimeUnit unit) {
        final Runnable addRole = () -> user.addRole(server.getRoleById(roleId).orElseThrow());
        scheduler.schedule(addRole, delay, unit);
    }

    public void removeRoleAfter(long delay, TimeUnit unit) {
        final Runnable removeRole = () -> user.removeRole(server.getRoleById(roleId).orElseThrow());
        scheduler.schedule(removeRole, delay, unit);
    }

    public void tempMute(long delay, TimeUnit unit, Long logChannelID) {
        // mute user
        server.addRoleToUser(user, server.getRoleById(roleId).get()).join();
        // create runnable to unmute user later
        Runnable runnable = new Runnable() {
            public void run() {
                user.removeRole(server.getRoleById(roleId).orElseThrow());
                server.getTextChannelById(logChannelID).ifPresent(channel -> {
                    channel.sendMessage(new EmbedBuilder()
                        .setTitle("Unmute")
                        .setDescription("**User:** " + user.getName() + " " + user.getMentionTag() + 
                            "\n**Moderator:** <@959987022582403193>" +
                            "\n**Reason:** Mute duration is over.")
                        .setColor(Color.GREEN)
                        .setFooter(user.getIdAsString())
                        .setTimestampToNow());
                });
            }
        };
        // submit runnable
        scheduler.schedule(runnable, delay, unit);
    }

    public long getUserId() {
        return user.getId();
    }
}