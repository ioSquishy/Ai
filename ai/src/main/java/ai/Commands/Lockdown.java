package ai.Commands;

import java.awt.Color;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import ai.Data.ServerSettings;
import ai.Data.Database.DocumentUnavailableException;

/**
 * Locks down the server by setting the @everyone roles Send Message Permission to disabled.
 */
public class Lockdown  {

    public static void handleCommand(SlashCommandInteraction interaction) {
        try {
            ServerSettings settings = new ServerSettings(interaction.getServer().get());
            ServerTextChannel logChannel = settings.isModLogEnabled() ? settings.getModLogChannel().orElse(null) : null;
            interaction.createImmediateResponder().setContent(runSlashCmd(interaction.getServer().get(), interaction.getUser(), logChannel)).respond().join();
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
            interaction.createImmediateResponder().setContent(
                    "Database unreachable so unable to log event in ModLog (if applicable). However, command still ran." + 
                    runSlashCmd(interaction.getServer().get(), interaction.getUser(), null)
                ).respond().join();
        }
    }

    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("lockdown")
            .setDescription("Enable/Disable a lockdown.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false);
    }

    private static String runSlashCmd(Server server, User author, ServerTextChannel logChannel) {
        Role everyoneRole = server.getEveryoneRole();
        PermissionState state = everyoneRole.getPermissions().getState(PermissionType.SEND_MESSAGES) == PermissionState.ALLOWED ? PermissionState.DENIED : PermissionState.ALLOWED;
            everyoneRole.updatePermissions(everyoneRole.getPermissions().toBuilder()
                .setState(PermissionType.SEND_MESSAGES, state)
                .setState(PermissionType.SEND_MESSAGES_IN_THREADS, state)
                .setState(PermissionType.ADD_REACTIONS, state)
                .setState(PermissionType.CONNECT, state)
                .setState(PermissionType.CHANGE_NICKNAME, state)
                .build()).join();
        boolean initialized = state == PermissionState.DENIED;
        if (logChannel != null) {
            if (initialized) {
                logChannel.sendMessage(getLockdownEnabledEmbed(author));
            } else {
                logChannel.sendMessage(getLockdownDisabledEmbed(author));
            }
        }
        return initialized ? "https://imgur.com/hIaMyPd.gif" : "https://imgur.com/O99TyXi.gif";
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