package ai.Commands;

import java.awt.Color;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import ai.ServerSettings;


public class Lockdown  {

    public static void handleCommand(SlashCommandInteraction interaction, ServerSettings settings) {
        interaction.createImmediateResponder().setContent(runSlashCmd(interaction.getServer().get(), interaction.getUser(), settings.getLogChannelID())).respond().join();
    }

    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("lockdown")
            .setDescription("Enable/Disable a lockdown.")
            .setDefaultDisabled();
    }

    private static String runSlashCmd(Server server, User author, long logChannelId) {
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
        if (initialized) {
            server.getChannelById(logChannelId).get().asServerTextChannel().get().sendMessage(
            new EmbedBuilder()
                .setTitle("Lockdown Enabled")
                .setDescription("Moderator: " + author.getMentionTag())
                .setColor(Color.RED)
                .setTimestampToNow()
            ).join();
        } else {
            server.getChannelById(logChannelId).get().asServerTextChannel().get().sendMessage(
            new EmbedBuilder()
                .setTitle("Lockdown Disabled")
                .setDescription("Moderator: " + author.getMentionTag())
                .setColor(Color.GREEN)
                .setTimestampToNow()
            ).join();
        }
        return initialized ? "https://imgur.com/hIaMyPd.gif" : "https://imgur.com/O99TyXi.gif";
    }
    
}