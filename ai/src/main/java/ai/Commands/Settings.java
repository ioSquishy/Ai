package ai.Commands;

import java.util.ArrayList;
import java.util.List;

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.interaction.SlashCommandBuilder;

import ai.Database.DocumentUnavailableException;
import ai.Utility.JoinHandler;
import ai.CustomIDs;
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
            CustomIDs.SETTINGS_JSON.name(), 
            "Server Settings", 
            "Submitting an empty text field will reset settings to defaults.", 
            settings.getSettingsJSON(), 
            false)
        ));

        actionRows.add(ActionRow.of(
            TextInput.create(
                TextInputStyle.PARAGRAPH, 
                CustomIDs.JOIN_MESSAGE.name(), 
                "Join Message", 
                "", 
                settings.getJoinMessage(), 
        false)
        ));

        return actionRows;
    }

    public static void handleSettingsModalSubmit(ModalInteraction interaction, ServerSettings settings) {
        String settingsJson = interaction.getTextInputValueByCustomId(CustomIDs.SETTINGS_JSON.name()).get();
        String joinMsg = interaction.getTextInputValueByCustomId(CustomIDs.JOIN_MESSAGE.name()).get();
        try {
            settings.setJoinMessage(joinMsg);
            settings.updateSettings(settingsJson);
            interaction.createImmediateResponder().setContent("Settings updated.").respond();
            // JoinHandler joinHandler = new JoinHandler(JoinHandler.testJoinEvent, settings);
            // interaction.createImmediateResponder().setContent(joinHandler.getJoinMessage()).respond();
            
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
            DocumentUnavailableException.sendStandardResponse(interaction);
        } catch (InvalidSettingsJsonException e) {
            e.printStackTrace();
            InvalidSettingsJsonException.sendStandardResponse(interaction);
        }
    }
}
