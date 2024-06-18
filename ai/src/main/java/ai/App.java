package ai;

import ai.Database.DocumentUnavailableException;
import ai.Commands.*;
import ai.Utility.*;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import org.javacord.api.entity.auditlog.AuditLogActionType;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.awt.Color;
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
        Mute.setMuteRoleCommand().createGlobal(api).join();
        Mute.muteSlashCommand().createGlobal(api).join();
        Unban.unbanSlashCommand().createGlobal(api).join();
        Lockdown.createCommand().createGlobal(api).join();
        ModLog.modLogCommand().createGlobal(api).join();
        Settings.createSettingsCommand().createGlobal(api).join();
        Settings.createJoinMsgCommand().createGlobal(api).join();

        // handle slash commands
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
                case "modlog" : ModLog.handleUpdateCmd(interaction, settings); break;
                case "setmuterole" : Settings.handleSetMuteRoleCmd(interaction, settings); break;
                case "setjoinmessage" : Settings.handleSetJoinMsgCmd(interaction, settings); break;
                case "settings" :
                    // settings.pullAllSettings();
                    interaction.createImmediateResponder().addEmbed(
                        new EmbedBuilder()
                            .setTitle("Settings")
                            .setColor(Color.BLUE)
                            .setDescription(
                                "**Mod Log Enabled:** " + settings.isLogEnabled() + "\n" +
                                "**Mod Log Channel:** <#" + settings.getLogChannelID() + ">\n" +
                                "**Mute Role:** <@&" + settings.getMuteRoleID() + ">\n" + 
                                "**Log Bans:** " + settings.isLogBanEnabled() + "\n" +
                                "**Log Mutes:** " + settings.isLogMuteEnabled() + "\n" + 
                                "**Log Kicks:** " + settings.isLogKicksEnabled() + "\n" +
                                "**Join Roles:**" + settings.getJoinRoles() + "\n" +
                                "**Join Message:**" + settings.getJoinMessage()
                            )).respond().join();
                    break;
            }
        });

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

        api.addServerMemberBanListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            if (!settings.isLogEnabled() || !settings.isLogBanEnabled()) {
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
                if (api.getChannelById(settings.getLogChannelID()).isPresent() && api.getChannelById(settings.getLogChannelID()).get().asTextChannel().isPresent()) {
                    event.getServer().getTextChannelById(settings.getLogChannelID()).get().sendMessage(new LogEmbeds().standardEmbed("Ban", lastBan.getUser().join(), event.getUser(), event.requestBan().join().getReason().orElse("")));
                }
            }
        });

        api.addServerMemberUnbanListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            if (settings.isLogEnabled() && settings.isLogBanEnabled() && event.getServer().getTextChannelById(settings.getLogChannelID()).isPresent()) {
                AuditLogEntry lastBan = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_BAN_ADD).join().getEntries().get(0);
                event.getServer().getTextChannelById(settings.getLogChannelID()).get().sendMessage(new LogEmbeds().unban(event.getUser(), lastBan.getUser().join()));
            }
        });

        api.addUserChangeTimeoutListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            if (settings.isLogEnabled() && settings.isLogMuteEnabled() && event.getServer().getTextChannelById(settings.getLogChannelID()).isPresent()) {
                AuditLogEntry lastTimeout = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_UPDATE).join().getEntries().get(0);
                if (!lastTimeout.getUser().join().getIdAsString().equals(api.getYourself().getIdAsString())) { //if mod is not bot
                    if (event.getNewTimeout().isPresent()) { //checks if timeout was set or removed
                        try {
                            event.getServer().getTextChannelById(settings.getLogChannelID()).get().sendMessage(new LogEmbeds().mute(event.getUser(), lastTimeout.getUser().get(), new ReadableTime().compute(event.getNewTimeout().get().getEpochSecond()-Instant.now().getEpochSecond()), lastTimeout.getReason().orElse("")));
                        } catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
                    } else {
                        try {
                            event.getServer().getTextChannelById(settings.getLogChannelID()).get().sendMessage(new LogEmbeds().unmute(event.getUser(), lastTimeout.getUser().get()));
                        } catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
                    }
                }
            }
        });

        api.addServerMemberLeaveListener(event -> {
            AuditLogEntry lastKick = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_KICK).join().getEntries().get(0);
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            if (settings.isLogEnabled() && settings.isLogKicksEnabled() && lastKick.getTarget().isPresent() && lastKick.getTarget().get().asUser().join().getId() == event.getUser().getId() && lastKick.getCreationTimestamp().getEpochSecond() >= Instant.now().getEpochSecond()-10 && event.getServer().getTextChannelById(settings.getLogChannelID()).isPresent()) {
                event.getServer().getTextChannelById(settings.getLogChannelID()).get().sendMessage(new LogEmbeds().standardEmbed("Kick", lastKick.getUser().join(), lastKick.getTarget().get().asUser().join(), lastKick.getReason().orElse("")));
            }
        });

        api.addServerLeaveListener(event -> {
            Database.removeServer(event.getServer().getId());
        });
        
    }
    
}
