package ai.Commands;

import java.util.NoSuchElementException;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;

import ai.Data.Database.DocumentUnavailableException;
import ai.Data.ServerSettings;
import ai.Utility.LogEmbed;
import ai.Utility.LogEmbed.EmbedType;

public class Unmute {

    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("unmute")
            .setDescription("Unmute someone.")
            .setDefaultEnabledForPermissions(PermissionType.MANAGE_MESSAGES)
            .addOption(new SlashCommandOptionBuilder()
                .setName("User")
                .setType(SlashCommandOptionType.USER)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("Reason")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(false)
                .build());
    }
    
    public static void handleUnmuteCommand(SlashCommandInteraction interaction) {
        try {
            ServerSettings settings = new ServerSettings(interaction.getServer().get());

            // get variables
            User targetUser = interaction.getArgumentUserValueByName("User").get();
            Role muteRole;
            final User moderator = interaction.getUser();
            final String reason = interaction.getArgumentStringValueByName("reason").orElse("");
            
            try {
                muteRole = settings.getMuteRole().orElseThrow();
            } catch (NoSuchElementException e) { // thrown if bad muterole
                interaction.createImmediateResponder().setContent("You do not have a valid mute role set!").respond();
                return;
            }

            // unmute
            unmuteUser(targetUser, moderator, reason, muteRole);
            
            // log if needed
            logUnmute(targetUser, moderator, reason, settings);

            // respond
            interaction.createImmediateResponder().setContent(targetUser.getName() + " was unmuted.").setFlags(MessageFlag.EPHEMERAL).respond();
        } catch (DocumentUnavailableException e) {
            DocumentUnavailableException.sendStandardResponse(interaction);
        }
    }

    /**
     * Unmutes user then logs event if applicable.
     * @param offender
     * @param moderator
     * @param reason
     * @param serverSettings
     * @throws DocumentUnavailableException
     */
    private static void unmuteUser(User offender, User moderator, String reason, Role muteRole) throws DocumentUnavailableException {
        muteRole.getServer().createUpdater()
            .removeRoleFromUser(offender, muteRole)
            .removeUserTimeout(offender)
            .update();
    }

    private static void logUnmute(User offender, User moderator, String reason, ServerSettings serverSettings) {
        if (serverSettings.isModLogEnabled() && serverSettings.isLogMuteEnabled()) {
            serverSettings.getModLogChannel().ifPresent(channel -> {
                channel.sendMessage(LogEmbed.getEmbed(EmbedType.Unmute, offender, moderator, reason));
            });
        }
    }
}
