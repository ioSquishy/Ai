package ai.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.callback.InteractionFollowupMessageBuilder;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

import ai.Database.DocumentUnavailableException;
import ai.Utility.JoinHandler;
import ai.Constants;
import ai.Constants.CustomID;
import ai.ServerSettings;
import ai.ServerSettings.InvalidSettingsJsonException;

public class Settings {
    public static SlashCommandBuilder createSettingsCommand() {
        return new SlashCommandBuilder()
            .setName("settings")
            .setDescription("Check modlog settings.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false);
    }

    // will add selectmenus to settings modal once discord and javacord adds it
    // private static final SelectMenuOption trueOption = new SelectMenuOptionBuilder().setValue("true").build();
    // private static final SelectMenuOption falseOption = new SelectMenuOptionBuilder().setValue("false").build();
    public static List<HighLevelComponent> createSettingsModalComponents(ServerSettings settings) throws DocumentUnavailableException {
        List<HighLevelComponent> actionRows = new ArrayList<HighLevelComponent>();

        actionRows.add(ActionRow.of(
            TextInput.create(
            TextInputStyle.PARAGRAPH, 
            CustomID.SETTINGS_JSON.name(), 
            "Server Settings", 
            "Submitting an empty text field will reset settings to defaults.", 
            settings.getSettingsJSON(), 
            false)
        ));

        actionRows.add(ActionRow.of(
            TextInput.create(
                TextInputStyle.PARAGRAPH, 
                CustomID.JOIN_MESSAGE.name(), 
                "Join Message", 
                "", 
                settings.getJoinMessage(), 
        false)
        ));

        return actionRows;
    }

    public static void handleSettingsModalSubmit(ModalInteraction interaction, ServerSettings settings) {
        String settingsJson = interaction.getTextInputValueByCustomId(CustomID.SETTINGS_JSON.name()).get();
        String joinMsg = interaction.getTextInputValueByCustomId(CustomID.JOIN_MESSAGE.name()).get();
        InteractionImmediateResponseBuilder responseMessage = interaction.createImmediateResponder();
        // set join message and update settings
        try {
            settings.setJoinMessage(joinMsg);
            settings.updateSettings(settingsJson);
            responseMessage.setContent("Settings updated.");
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
            responseMessage.setContent(DocumentUnavailableException.getStandardResponseString());
        } catch (InvalidSettingsJsonException | ClassCastException e) {
            e.printStackTrace();
            responseMessage.setContent(InvalidSettingsJsonException.getStandardResponseString());
        }

        // send message with new settings and join message
        try {
            responseMessage
                .addEmbed(getSettingsEmbed(settings))
                .addEmbed(getJoinMessageEmbed(settings));
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
        }

        responseMessage.respond();
    }

    private static EmbedBuilder getSettingsEmbed(ServerSettings settings) throws DocumentUnavailableException {
        return new EmbedBuilder()
            .setTitle("Settings")
            .setDescription(
                "Mute Role: <@&" + settings.getMuteRoleID() + ">\n" +
                "\n" +
                "ModLog Enabled: " + settings.isModLogEnabled() + "\n" +
                "ModLog Channel: <#" + settings.getLogChannelID() + ">\n" +
                "Log Bans: " + settings.isLogBanEnabled() + "\n" + 
                "Log Mutes: " + settings.isLogMuteEnabled() + "\n" + 
                "Log Kicks: " + settings.isLogKicksEnabled() + "\n" +
                "\n" +
                "Join Message Enabled: " + settings.isJoinMessageEnabled() + "\n" +
                "Join Message Channel: <#" + settings.getJoinMessageChannelID() + ">\n" +
                "Join Message Roles: " + formatJoinRoles(settings.getJoinRoleIDs())
            );
    }

    private static String formatJoinRoles(List<Long> joinRoleIDs) {
        String roles = "";
        for (Long roleID : joinRoleIDs) {
            roles += "<@&" + roleID + ">, ";
        }
        if (!roles.isEmpty()) {
            roles.substring(0, roles.length()-2);
        }
        return roles;
    }

    private static EmbedBuilder getJoinMessageEmbed(ServerSettings settings) throws DocumentUnavailableException {
        JoinHandler joinHandler = new JoinHandler(JoinHandler.testJoinEvent, settings);
        return new EmbedBuilder()
            .setTitle("Join Message Example")
            .setDescription(joinHandler.getJoinMessage());
    }

}
