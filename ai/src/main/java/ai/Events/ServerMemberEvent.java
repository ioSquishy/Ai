package ai.Events;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.javacord.api.entity.auditlog.AuditLogActionType;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.listener.server.member.ServerMemberBanListener;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;
import org.javacord.api.listener.server.member.ServerMemberLeaveListener;
import org.javacord.api.listener.server.member.ServerMemberUnbanListener;
import org.javacord.api.listener.user.UserChangeTimeoutListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Data.ServerSettings;
import ai.Data.Database.DocumentUnavailableException;
import ai.Utility.JoinHandler;
import ai.Utility.LogEmbed;
import ai.Utility.LogEmbed.EmbedType;
import ai.Utility.ReadableTime;

public class ServerMemberEvent {

    public static ListenerManager<ServerMemberJoinListener> addJoinListener() {
        return App.api.addServerMemberJoinListener(event -> {
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
    }

    public static ListenerManager<ServerMemberLeaveListener> addLeaveListener() {
        return App.api.addServerMemberLeaveListener(event -> {
            AuditLogEntry lastKick = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_KICK).join().getEntries().get(0);
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            try {
                if (settings.isModLogEnabled() && settings.isLogKicksEnabled() && lastKick.getTarget().isPresent() && lastKick.getTarget().get().asUser().join().getId() == event.getUser().getId() && lastKick.getCreationTimestamp().getEpochSecond() >= Instant.now().getEpochSecond()-10 && settings.getLogChannelID().isPresent() && event.getServer().getTextChannelById(settings.getLogChannelID().get()).isPresent()) {
                    event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(LogEmbed.getEmbed(EmbedType.Kick, lastKick.getTarget().get().asUser().join(), lastKick.getUser().join(), null, lastKick.getReason().orElse("")));
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
            }
        });
    }

    public static ListenerManager<ServerMemberBanListener> addBanListener() {
        return App.api.addServerMemberBanListener(event -> {
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
                    if (App.api.getChannelById(settings.getLogChannelID().orElse(-1L)).isPresent() && App.api.getChannelById(settings.getLogChannelID().orElse(-1L)).get().asTextChannel().isPresent()) {
                        event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(LogEmbed.getEmbed(EmbedType.Ban, event.getUser(), lastBan.getUser().join(),  null, event.requestBan().join().getReason().orElse("")));
                    }
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
                return;
            }
        });
    }

    public static ListenerManager<ServerMemberUnbanListener> addUnbanListener() {
        return App.api.addServerMemberUnbanListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            try {
                if (settings.isModLogEnabled() && settings.isLogBanEnabled() && settings.getLogChannelID().isPresent() && event.getServer().getTextChannelById(settings.getLogChannelID().get()).isPresent()) {
                    AuditLogEntry lastBan = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_BAN_ADD).join().getEntries().get(0);
                    event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(LogEmbed.getEmbed(EmbedType.Unban, event.getUser(), lastBan.getUser().join(), null, null));
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
            }
        });
    }

    public static ListenerManager<UserChangeTimeoutListener> addTimeoutChangeListener() {
        return App.api.addUserChangeTimeoutListener(event -> {
            ServerSettings settings = new ServerSettings(event.getServer().getId());
            try {
                if (settings.isModLogEnabled() && settings.isLogMuteEnabled() && event.getServer().getTextChannelById(settings.getLogChannelID().orElse(-1L)).isPresent()) {
                    AuditLogEntry lastTimeout = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_UPDATE).join().getEntries().get(0);
                    if (!lastTimeout.getUser().join().getIdAsString().equals(App.api.getYourself().getIdAsString())) { //if mod is not bot
                        if (event.getNewTimeout().isPresent()) { //checks if timeout was set or removed
                            try {
                                event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(LogEmbed.getEmbed(EmbedType.Mute, event.getUser(), lastTimeout.getUser().get(), new ReadableTime().compute(event.getNewTimeout().get().getEpochSecond()-Instant.now().getEpochSecond()), lastTimeout.getReason().orElse("")));
                            } catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
                        } else {
                            try {
                                event.getServer().getTextChannelById(settings.getLogChannelID().get()).get().sendMessage(LogEmbed.getEmbed(EmbedType.Unmute, event.getUser(), lastTimeout.getUser().get(), null, null));
                            } catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
                        }
                    }
                }
            } catch (DocumentUnavailableException e) {
                e.printStackTrace();
            }
        });
    }

}
