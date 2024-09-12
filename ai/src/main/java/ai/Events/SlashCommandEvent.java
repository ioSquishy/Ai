package ai.Events;

import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Commands.BotInfo;
import ai.Commands.Lockdown;
import ai.Commands.Mute;
import ai.Commands.Ping;
import ai.Commands.Purge;
import ai.Commands.SettingsCommand;
import ai.Commands.Unban;

public class SlashCommandEvent {

    public static ListenerManager<SlashCommandCreateListener> addSlashCommandCreateListener() {
        return App.api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            
            switch (interaction.getCommandName().toString()) {
                case "ping" : Ping.handleCommand(interaction); break;
                case "botinfo" : BotInfo.HandleCommand(interaction); break;
                case "lockdown" : Lockdown.handleCommand(interaction); break;
                case "mute" : Mute.handleMuteCommand(interaction); break;
                case "unmute" : Mute.handleUnmuteCommand(interaction); break;
                case "unban" : Unban.handleCommand(interaction); break;
                case "purge" : Purge.handleCommand(interaction); break;
                case "settings" : SettingsCommand.handleSettingsCommand(interaction); break;
            }
        });
    }

}
