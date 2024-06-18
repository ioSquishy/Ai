package ai.Commands;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;

import ai.Database.DocumentUnavailableException;
import ai.Utility.JoinHandler;
import ai.ServerSettings;

public class Settings {
    public static SlashCommandBuilder createSettingsCommand() {
        return new SlashCommandBuilder()
            .setName("settings")
            .setDescription("Check modlog settings.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false);
    }

    public static SlashCommandBuilder createJoinMsgCommand() {
        return new SlashCommandBuilder()
            .setName("setjoinmessage")
            .setDescription("Set join message for the server.")
            .addOption(new SlashCommandOptionBuilder()
                .setName("message")
                .setDescription("Join message to be sent when a user joins the server.")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(true)
                .build())
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false);
    }

    
    public static void handleSetJoinMsgCmd(SlashCommandInteraction interaction, ServerSettings settings) {
        String newJoinMsg = interaction.getArgumentStringValueByName("message").get();
        try {
            settings.setJoinMessage(newJoinMsg);
            JoinHandler joinHandler = new JoinHandler(JoinHandler.testJoinEvent, settings);
            System.out.println(joinHandler.getJoinMessage());
            interaction.createImmediateResponder().setContent("Join message set to:\n" + joinHandler.getJoinMessage()).respond();
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
            DocumentUnavailableException.sendStandardResponse(interaction);
        }
    }

    public static void handleSetMuteRoleCmd(SlashCommandInteraction interaction, ServerSettings settings) {
        Role newMuteRole = interaction.getArgumentRoleValueByName("role").get();
        settings.setMuteRoleID(newMuteRole.getId());
        interaction.createImmediateResponder().setContent("Mute role set to: " + newMuteRole.getMentionTag()).setFlags(MessageFlag.EPHEMERAL).respond().join();
    }
}
