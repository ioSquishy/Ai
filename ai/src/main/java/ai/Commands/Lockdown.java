package ai.Commands;

import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.tinylog.Logger;

import ai.Data.Database.DocumentUnavailableException;
import ai.Utility.LogEmbed;
import ai.Utility.PermissionsCheck;
import ai.Data.ServerSettings;

/**
 * Locks down the server by setting the @everyone roles Send Message Permission to disabled.
 */
public class Lockdown  {
    private static final String openDoorGif = "https://github.com/ioSquishy/Ai/blob/main/ai/src/main/Assets/open_door.gif?raw=true";
    private static final String slamDoorGif = "https://github.com/ioSquishy/Ai/blob/main/ai/src/main/Assets/slam_door.gif?raw=true";

    public static void handleCommand(SlashCommandInteraction interaction) {
        Server server = interaction.getServer().get();

        // check permissions
        if (!PermissionsCheck.canManageRoles(server)) {
            interaction.createImmediateResponder().setContent("I cannot manage roles!").respond();
            return;
        }

        // lockdown server
        PermissionState newLockdownState = server.getEveryoneRole().getPermissions().getState(PermissionType.SEND_MESSAGES) == PermissionState.ALLOWED ? PermissionState.DENIED : PermissionState.ALLOWED;
        updateEveryoneRolePermissions(server, newLockdownState);

        // respond to command
        String gifLink = newLockdownState == PermissionState.DENIED ? slamDoorGif : openDoorGif;
        interaction.createImmediateResponder().setContent(gifLink).respond();

        // log if applicable
        ServerSettings serverSettings;
        try {
            serverSettings = new ServerSettings(server);
            if (serverSettings.isModLogEnabled() && serverSettings.isLogLockdownsEnabled()) {
                logLockdownEvent(interaction.getUser(), serverSettings, newLockdownState);
            }
        } catch (DocumentUnavailableException e) {
            Logger.debug(e);
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
            if (!PermissionsCheck.canSendMessages(logChannel, true)) return;
            logChannel.sendMessage(LogEmbed.lockdownEmbed(author, initialized));
        });
    }
    
}