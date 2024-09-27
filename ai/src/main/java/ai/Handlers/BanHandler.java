package ai.Handlers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.javacord.api.entity.auditlog.AuditLogActionType;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberBanEvent;
import org.javacord.api.event.server.member.ServerMemberUnbanEvent;

import ai.Data.Database.DocumentUnavailableException;
import ai.App;
import ai.Data.ServerSettings;
import ai.Utility.LogEmbed;
import ai.Utility.LogEmbed.EmbedType;

public class BanHandler {
    public static void handleBanEvent(ServerMemberBanEvent banEvent) {
        try {
            Server server = banEvent.getServer();

            ServerSettings serverSettings = new ServerSettings(server);
            if (isLogBansEnabled(serverSettings) && canLogBans(serverSettings, server)) {
                AuditLogEntry lastBan = banEvent.getServer().getAuditLog(1, AuditLogActionType.MEMBER_BAN_ADD).join().getEntries().get(0);
                if (!isBeemoBan(lastBan)) {
                    logBan(serverSettings, lastBan);
                }
            }
        } catch (DocumentUnavailableException e) {
            // e.printStackTrace();
        }
    }

    public static void handleUnbanEvent(ServerMemberUnbanEvent unbanEvent) {
        try {
            Server server = unbanEvent.getServer();

            ServerSettings serverSettings = new ServerSettings(server);
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
        if (!serverSettings.getModLogChannel().isPresent()) return false;
        if (!serverSettings.getModLogChannel().isPresent()) return false;
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

    private static void logBan(ServerSettings serverSettings, AuditLogEntry lastBanEntry) {
        serverSettings.getModLogChannel().ifPresent(channel -> {
            try {
                channel.sendMessage(LogEmbed.getEmbed(EmbedType.Ban, lastBanEntry.getTarget().get().asUser().get(), lastBanEntry.getUser().get(), lastBanEntry.getReason().orElse("")));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    private static void logUnban(ServerSettings serverSettings, ServerMemberUnbanEvent unbanEvent, AuditLogEntry lastUnbanEntry) {
        serverSettings.getModLogChannel().ifPresent(channel -> {
            try {
                String reason = lastUnbanEntry.getReason().orElse("");
                User moderator = lastUnbanEntry.getUser().get(3, TimeUnit.SECONDS);

                if (!reason.isEmpty() && App.botID == moderator.getId()) {
                    String[] reasonAndModerator = LogEmbed.separateReasonAndModerator(reason);
                    reason = reasonAndModerator[0];
                    try {
                        moderator = unbanEvent.getApi().getUserById(reasonAndModerator[1]).get(3, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }

                channel.sendMessage(LogEmbed.getEmbed(
                    EmbedType.Unban,
                    lastUnbanEntry.getTarget().get().asUser().get(3, TimeUnit.SECONDS),
                    moderator,
                    reason));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        });
    }
}
