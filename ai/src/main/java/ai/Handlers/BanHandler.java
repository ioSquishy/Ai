package ai.Handlers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.javacord.api.entity.auditlog.AuditLogActionType;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.entity.server.Ban;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberBanEvent;
import org.javacord.api.event.server.member.ServerMemberUnbanEvent;

import ai.Data.Database.DocumentUnavailableException;
import ai.Data.ServerSettings;
import ai.Utility.LogEmbed;
import ai.Utility.LogEmbed.EmbedType;

public class BanHandler {
    public static void handleBanEvent(ServerMemberBanEvent banEvent) {
        try {
            Server server = banEvent.getServer();

            ServerSettings serverSettings = new ServerSettings(server.getId());
            if (isLogBansEnabled(serverSettings) && canLogBans(serverSettings, server)) {
                AuditLogEntry lastBan = banEvent.getServer().getAuditLog(1, AuditLogActionType.MEMBER_BAN_ADD).join().getEntries().get(0);
                if (!isBeemoBan(lastBan)) {
                    logBan(serverSettings, banEvent, lastBan);
                }
            }
        } catch (DocumentUnavailableException e) {
            // e.printStackTrace();
        }
    }

    public static void handleUnbanEvent(ServerMemberUnbanEvent unbanEvent) {
        try {
            Server server = unbanEvent.getServer();

            ServerSettings serverSettings = new ServerSettings(server.getId());
            if (isLogBansEnabled(serverSettings) && canLogBans(serverSettings, server)) {
                AuditLogEntry lastUnban = unbanEvent.getServer().getAuditLog(1, AuditLogActionType.MEMBER_BAN_REMOVE).join().getEntries().get(0);
                logUnban(serverSettings, unbanEvent, lastUnban);
            }
        } catch (DocumentUnavailableException e) {
            // e.printStackTrace();
        }
    }

    private static boolean isLogBansEnabled(ServerSettings serverSettings) {
        if (!serverSettings.isLogBanEnabled()) return false;
        if (!serverSettings.isModLogEnabled()) return false;
        return true;
    }

    private static boolean canLogBans(ServerSettings serverSettings, Server server) {
        if (!serverSettings.getLogChannelID().isPresent()) return false;
        long logChannelID = serverSettings.getLogChannelID().get();
        if (!server.getTextChannelById(logChannelID).isPresent()) return false;
        return true;
    }

    private static boolean isBeemoBan(AuditLogEntry lastBanEntry) {
        try {
            if (lastBanEntry.getUser().get(3, TimeUnit.SECONDS).getId() == 515067662028636170L) {
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }

    private static void logBan(ServerSettings serverSettings, ServerMemberBanEvent banEvent, AuditLogEntry lastBanEntry) {
        try {
            Ban ban = banEvent.requestBan().get();
            banEvent.getServer().getTextChannelById(serverSettings.getLogChannelID().get()).get().sendMessage(
                LogEmbed.getEmbed(EmbedType.Ban, ban.getUser(), banEvent.getUser(), ban.getReason().orElse("")));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void logUnban(ServerSettings serverSettings, ServerMemberUnbanEvent unbanEvent, AuditLogEntry lastUnbanEntry) {
        try {
            unbanEvent.getServer().getTextChannelById(serverSettings.getLogChannelID().get()).get().sendMessage(
            LogEmbed.getEmbed(
                EmbedType.Unban,
                lastUnbanEntry.getTarget().get().asUser().get(3, TimeUnit.SECONDS),
                lastUnbanEntry.getUser().get(3, TimeUnit.SECONDS),
                lastUnbanEntry.getReason().orElse("")));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
