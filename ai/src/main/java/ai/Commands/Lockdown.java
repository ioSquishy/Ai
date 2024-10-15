package ai.Commands;

import java.awt.Color;
import java.io.File;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionFollowupMessageBuilder;

import ai.Data.Database.DocumentUnavailableException;
import ai.Data.ServerSettings;

/**
 * Locks down the server by setting the @everyone roles Send Message Permission to disabled.
 */
public class Lockdown  {
    private static final File openDoorGif = new File("ai\\src\\main\\Assets\\open_door.gif");
    private static final File slamDoorGif = new File("ai\\src\\main\\Assets\\slam_door.gif");


    public static void handleCommand(SlashCommandInteraction interaction) {
        Server server = interaction.getServer().get();

        PermissionState newLockdownState = server.getEveryoneRole().getPermissions().getState(PermissionType.SEND_MESSAGES) == PermissionState.ALLOWED ? PermissionState.DENIED : PermissionState.ALLOWED;
        updateEveryoneRolePermissions(server, newLockdownState);


        System.out.println(slamDoorGif.getAbsolutePath());
        InteractionFollowupMessageBuilder response = interaction.createFollowupMessageBuilder();
        if (newLockdownState == PermissionState.DENIED) { // if lockdown was initialized
            response.addAttachment(slamDoorGif);
        } else {
            response.addAttachment(openDoorGif);
        }
        response.setContent("??").send();

        ServerSettings serverSettings;
        try {
            serverSettings = new ServerSettings(server);
            logLockdownEvent(interaction.getUser(), serverSettings, newLockdownState);
        } catch (DocumentUnavailableException e) {
            // e.printStackTrace();
        }
    }

    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("lockdown")
            .setDescription("Enable/Disable a lockdown.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false);
    }

    /**
     * Updates the @everyone role permission states to be disabled if initializing a lockdown and vice versa.
     */
    private static void updateEveryoneRolePermissions(Server server, PermissionState state) {
        Role everyoneRole = server.getEveryoneRole();
        everyoneRole.updatePermissions(everyoneRole.getPermissions().toBuilder()
            .setState(PermissionType.SEND_MESSAGES, state)
            .setState(PermissionType.SEND_MESSAGES_IN_THREADS, state)
            .setState(PermissionType.ADD_REACTIONS, state)
            .setState(PermissionType.CONNECT, state)
            .setState(PermissionType.CHANGE_NICKNAME, state)
            .build()).join();
    }

    private static void logLockdownEvent(User author, ServerSettings settings, PermissionState lockdownState) {
        boolean initialized = lockdownState == PermissionState.DENIED;

        settings.getModLogChannel().ifPresent(logChannel -> {
            if (initialized) {
                logChannel.sendMessage(getLockdownEnabledEmbed(author));
            } else {
                logChannel.sendMessage(getLockdownDisabledEmbed(author));
            }
        });
    }

    private static EmbedBuilder getLockdownEnabledEmbed(User moderator) {
        return new EmbedBuilder()
            .setTitle("Lockdown Enabled")
            .setDescription("Moderator: " + moderator.getMentionTag())
            .setColor(Color.RED)
            .setTimestampToNow();
    }

    private static EmbedBuilder getLockdownDisabledEmbed(User moderator) {
        return new EmbedBuilder()
            .setTitle("Lockdown Disabled")
            .setDescription("Moderator: " + moderator.getMentionTag())
            .setColor(Color.GREEN)
            .setTimestampToNow();
    }
    
}