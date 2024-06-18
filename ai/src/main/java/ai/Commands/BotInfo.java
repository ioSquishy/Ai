package ai.Commands;

import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import ai.App;

public class BotInfo {
    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("botinfo")
            .setDescription("Get information about the bot.");
    }
    
    public static void HandleCommand(SlashCommandInteraction interaction) {
        interaction.createImmediateResponder().setContent("Version: `" + App.version + "`\nLast Restart: <t:" + App.startEpoch + ":R>\nDeveloper: `Squishy#0007`").respond();
    }
}
