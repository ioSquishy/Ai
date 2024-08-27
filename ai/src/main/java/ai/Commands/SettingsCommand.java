package ai.Commands;


import java.util.ArrayList;
import java.util.List;

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

import com.squareup.moshi.JsonDataException;

import ai.Constants;
import ai.Constants.CustomID;
import ai.Data.ServerSettings;
import ai.Data.Database.DocumentUnavailableException;
import ai.Handlers.JoinHandler;

public class SettingsCommand {
    public static SlashCommandBuilder createSettingsCommand() {
        return new SlashCommandBuilder()
            .setName("settings")
            .setDescription("Check modlog settings.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false);
    }

    public static void handleSettingsCommand(SlashCommandInteraction interaction) {
        try {
            ServerSettings settings = new ServerSettings(interaction.getServer().get());
            interaction.respondWithModal(CustomID.SETTINGS_MODAL, "Settings", createSettingsModalComponents(settings));
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
            DocumentUnavailableException.sendStandardResponse(interaction);
        }
    }

    // will add selectmenus to settings modal once discord and javacord adds it
    // private static final SelectMenuOption trueOption = new SelectMenuOptionBuilder().setValue("true").build();
    // private static final SelectMenuOption falseOption = new SelectMenuOptionBuilder().setValue("false").build();
    private static List<HighLevelComponent> createSettingsModalComponents(ServerSettings settings) throws DocumentUnavailableException {
        List<HighLevelComponent> actionRows = new ArrayList<HighLevelComponent>();

        actionRows.add(ActionRow.of(
            TextInput.create(
            TextInputStyle.PARAGRAPH, 
            Constants.CustomID.SETTINGS_JSON, 
            "Server Settings", 
            "Submitting an empty text field will reset settings to defaults.", 
            settings.getSettingsJSON(), 
            false)
        ));

        actionRows.add(ActionRow.of(
            TextInput.create(
                TextInputStyle.PARAGRAPH, 
                CustomID.JOIN_MESSAGE, 
                "Join Message", 
                "", 
                settings.getJoinMessage(), 
        false)
        ));

        return actionRows;
    }

    public static void handleSettingsModalSubmit(ModalInteraction interaction) {
        String settingsJson = interaction.getTextInputValueByCustomId(CustomID.SETTINGS_JSON).get();
        String joinMsg = interaction.getTextInputValueByCustomId(CustomID.JOIN_MESSAGE).get();
        InteractionImmediateResponseBuilder responseMessage = interaction.createImmediateResponder();
        
        try {
            // set join message and update settings
            ServerSettings settings = new ServerSettings(interaction.getServer().get());
            settings.setJoinMessage(joinMsg);
            settings.updateSettings(settingsJson);
            responseMessage.setContent("Settings updated.");

             // send message with new settings and join message
            responseMessage
                .addEmbed(getSettingsEmbed(settings))
                .addEmbed(getJoinMessageEmbed(settings));
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
            responseMessage.setContent(DocumentUnavailableException.getStandardResponseString());
        } catch (JsonDataException e) {
            e.printStackTrace();
            responseMessage.setContent("Invalid settings JSON. No changes applied.");
        } finally {
            responseMessage.respond();
        }
    }

    private static EmbedBuilder getSettingsEmbed(ServerSettings settings) throws DocumentUnavailableException {
        return new EmbedBuilder()
            .setTitle("Settings")
            .setDescription(
                settings.getSettingsJSON()
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
