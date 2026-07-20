package ai.Handlers;

import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.auditlog.AuditLog;
import org.javacord.api.entity.auditlog.AuditLogActionType;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.tinylog.Logger;

import ai.Data.Database.DocumentUnavailableException;
import ai.Data.ServerSettings;
import ai.Utility.LogEmbed;
import ai.Utility.PermissionsCheck;
import ai.Utility.LogEmbed.EmbedType;

public class LeaveHandler {
    /**
     * Checks if leave event was caused by a user being kicked and handles if that is true.
     */
    public static void handleLeaveEvent(ServerMemberLeaveEvent event) {
        Server server = event.getServer();
        try {
            ServerSettings serverSettings = new ServerSettings(server);
            new LeaveHandler(serverSettings, event).handleIfKick();
        } catch (DocumentUnavailableException e) {
            Logger.tag("ai").debug(e);
        }
    }

    private final ServerSettings serverSettings;
    private final ServerMemberLeaveEvent leaveEvent;
    
    private LeaveHandler(ServerSettings serverSettings, ServerMemberLeaveEvent leaveEvent) {
        this.serverSettings = serverSettings;
        this.leaveEvent = leaveEvent;
    }

    private void handleIfKick() {
        if (isLogKicksEnabled() && canLogKicks() && wasKicked()) {
            logKick();
        }
    }

    private boolean isLogKicksEnabled() {
        if (!serverSettings.isModLogEnabled()) return false;
        if (!serverSettings.isLogKicksEnabled()) return false;
        return true;
    }

    private boolean canLogKicks() {
        if (!serverSettings.getModLogChannel().isPresent()) return false;
        return true;
    }

    private AuditLogEntry lastAuditEntry;
    private boolean wasKicked() {
        if (!PermissionsCheck.canReadAuditLog(leaveEvent.getServer(), true)) return false;
        try {
            AuditLog auditLog = leaveEvent.getServer().getAuditLog(1).get(2, TimeUnit.SECONDS);
            if (auditLog.getEntries().isEmpty()) return false;
            lastAuditEntry = auditLog.getEntries().get(0);
            return lastAuditEntry.getType() == AuditLogActionType.MEMBER_KICK;
        } catch (Exception e) {
            Logger.error(e);
            return false;
        }
        
    }

    private void logKick() {
        serverSettings.getModLogChannel().ifPresent(logChannel -> {
            if (PermissionsCheck.canSendMessages(logChannel, true)) {
                logChannel.sendMessage(LogEmbed.getEmbed(
                    EmbedType.Kick, lastAuditEntry.getTarget().get().asUser().join(), lastAuditEntry.getUser().join(), lastAuditEntry.getReason().orElse("")
                ));
            }
        });
    }
}
