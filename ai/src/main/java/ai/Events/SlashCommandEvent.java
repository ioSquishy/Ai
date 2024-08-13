package ai.Events;

import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Commands.BotInfo;
import ai.Commands.Lockdown;
import ai.Commands.Mute;
import ai.Commands.Ping;
import ai.Commands.Settings;
import ai.Commands.Unban;
import ai.Data.ServerSettings;

public class SlashCommandEvent {

    public static ListenerManager<SlashCommandCreateListener> addSlashCommandCreateListener() {
        return App.api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            ServerSettings settings = new ServerSettings(interaction.getServer().orElseThrow().getId());
            switch (interaction.getCommandName().toString()) {
                case "ping" : Ping.handleCommand(interaction); break;
                case "botinfo" : BotInfo.HandleCommand(interaction); break;
                case "lockdown" : Lockdown.handleCommand(interaction, settings); break;
                case "mute" : Mute.handleMuteCommand(interaction, settings); break;
                case "unmute" : Mute.handleUnmuteCommand(interaction, settings); break;
                case "unban" : Unban.handleCommand(interaction); break;
                case "settings" : Settings.handleSettingsCommand(interaction, settings); break;
            }
        });
    }

}
