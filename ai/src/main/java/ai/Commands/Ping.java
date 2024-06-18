package ai.Commands;

import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import ai.App;

public class Ping  {
    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("ping")
            .setDescription("Pong!");
    }

    public static void handleCommand (SlashCommandInteraction interaction) {
        interaction.createImmediateResponder().setContent("Pong! `" + App.api.getLatestGatewayLatency().toMillis() + "ms`").respond();
    }
}