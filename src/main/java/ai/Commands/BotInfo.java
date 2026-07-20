package ai.Commands;

import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import ai.App;
import ai.Data.Database;

public class BotInfo {
    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("botinfo")
            .setDescription("Get information about the bot.");
    }
    
    public static void handleCommand(SlashCommandInteraction interaction) {
        String dbStatus = Database.mongoOK ? "up since: " : "down since: ";
        interaction.createImmediateResponder().setContent(
            "Version: `" + App.version + "`" + 
            "\nDatabase " + dbStatus + "<t:" + Database.downUpTimeStartEpoch + ":f>" +
            "\nLast Restart: <t:" + App.startEpoch + ":R>" +
            "\nDeveloper: `squishhy`"
            ).respond();
    }
}
