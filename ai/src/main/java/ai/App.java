package ai;

import ai.Constants.CustomID;
import ai.Database.DocumentUnavailableException;
import ai.Commands.*;
import ai.Utility.*;
import ai.Utility.LogEmbed.EmbedType;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import org.javacord.api.entity.auditlog.AuditLogActionType;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.io.Serializable;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import io.github.cdimascio.dotenv.Dotenv;

public class App implements Serializable {
    private static final long serialVersionUID = 0;

    public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("TOKEN")).setAllIntents().login().join();

    public static transient final long startEpoch = Instant.now().getEpochSecond();
    public static transient final String version = "2";
    public static void main(String[] args) {
        System.out.println("Ai is online!");
        Database.initMongoDB();

        // create slash commands
        Ping.createCommand().createGlobal(api).join();
        BotInfo.createCommand().createGlobal(api).join();
        Mute.muteSlashCommand().createGlobal(api).join();
        Unban.unbanSlashCommand().createGlobal(api).join();
        Lockdown.createCommand().createGlobal(api).join();
        Settings.createSettingsCommand().createGlobal(api).join();
        
        // slash command listener
        api.addSlashCommandCreateListener(event -> {
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

        // modal submission listener
        api.addModalSubmitListener(event -> {
            ModalInteraction interaction = event.getModalInteraction();
            ServerSettings serverSettings = new ServerSettings(interaction.getServer().get().getId());
            
            switch (interaction.getCustomId()) {
                case CustomID.SETTINGS_MODAL :
                    Settings.handleSettingsModalSubmit(interaction, serverSettings);
                    break;
            }
        });

        // join listener
        api.addServerMemberJoinListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            JoinHandler joinHandler = new JoinHandler(event, settings);
            try {
                if (settings.isJoinMessageEnabled()) {
                    joinHandler.sendJoinMessage();
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
            }
        });

        // ban listener
        api.addServerMemberBanListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            try {
                if (!settings.isModLogEnabled() || !settings.isLogBanEnabled() || !settings.getLogChannelID().isPresent()) {
                    return;
                }
                AuditLogEntry lastBan = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_BAN_ADD).join().getEntries().get(0);
                try { //flood/beemo protection
                    if (lastBan.getUser().get(1, TimeUnit.SECONDS).getIdAsString().equals("515067662028636170")) {
                        return;
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    return;
                }
                if (settings.isLogBanEnabled()) {
                    if (api.getChannelById(settings.getLogChannelID().orElse(-1L)).isPresent() && api.getChannelById(settings.getLogChannelID().orElse(-1L)).get().asTextChannel().isPresent()) {
                        event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(new LogEmbed().getEmbed(EmbedType.Ban, event.getUser(), lastBan.getUser().join(),  null, event.requestBan().join().getReason().orElse("")));
                    }
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
                return;
            }
        });

        // unban listener
        api.addServerMemberUnbanListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            try {
                if (settings.isModLogEnabled() && settings.isLogBanEnabled() && settings.getLogChannelID().isPresent() && event.getServer().getTextChannelById(settings.getLogChannelID().get()).isPresent()) {
                    AuditLogEntry lastBan = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_BAN_ADD).join().getEntries().get(0);
                    event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(new LogEmbed().getEmbed(EmbedType.Unban, event.getUser(), lastBan.getUser().join(), null, null));
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
            }
        });

        api.addUserChangeTimeoutListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            try {
                if (settings.isModLogEnabled() && settings.isLogMuteEnabled() && event.getServer().getTextChannelById(settings.getLogChannelID().orElse(-1L)).isPresent()) {
                    AuditLogEntry lastTimeout = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_UPDATE).join().getEntries().get(0);
                    if (!lastTimeout.getUser().join().getIdAsString().equals(api.getYourself().getIdAsString())) { //if mod is not bot
                        if (event.getNewTimeout().isPresent()) { //checks if timeout was set or removed
                            try {
                                event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(new LogEmbed().getEmbed(EmbedType.Mute, event.getUser(), lastTimeout.getUser().get(), new ReadableTime().compute(event.getNewTimeout().get().getEpochSecond()-Instant.now().getEpochSecond()), lastTimeout.getReason().orElse("")));
                            } catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
                        } else {
                            try {
                                event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(new LogEmbed().getEmbed(EmbedType.Unmute, event.getUser(), lastTimeout.getUser().get(), null, null));
                            } catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
                        }
                    }
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
            }
        });

        api.addServerMemberLeaveListener(event -> {
            AuditLogEntry lastKick = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_KICK).join().getEntries().get(0);
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            try {
                if (settings.isModLogEnabled() && settings.isLogKicksEnabled() && lastKick.getTarget().isPresent() && lastKick.getTarget().get().asUser().join().getId() == event.getUser().getId() && lastKick.getCreationTimestamp().getEpochSecond() >= Instant.now().getEpochSecond()-10 && settings.getLogChannelID().isPresent() && event.getServer().getTextChannelById(settings.getLogChannelID().get()).isPresent()) {
                    event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(new LogEmbed().getEmbed(EmbedType.Kick, lastKick.getTarget().get().asUser().join(), lastKick.getUser().join(), null, lastKick.getReason().orElse("")));
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
            }
        });

        api.addServerLeaveListener(event -> {
            Database.removeServer(event.getServer().getId());
        });
        
    }
    
}
