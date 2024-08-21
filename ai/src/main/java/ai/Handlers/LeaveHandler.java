package ai.Handlers;

import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.auditlog.AuditLog;
import org.javacord.api.entity.auditlog.AuditLogActionType;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import ai.Data.Database.DocumentUnavailableException;
import ai.Data.ServerSettings;
import ai.Utility.LogEmbed;
import ai.Utility.LogEmbed.EmbedType;

public class LeaveHandler {
    /**
     * Checks if leave event was caused by a user being kicked and handles if that is true.
     */
    public static void handleLeaveEvent(ServerMemberLeaveEvent event) {
        Server server = event.getServer();
        try {
            ServerSettings serverSettings = new ServerSettings(server.getId());
            new LeaveHandler(serverSettings, event).handleIfKick();
        } catch (DocumentUnavailableException e) {
            // e.printStackTrace();
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
        if (!serverSettings.getLogChannelID().isPresent()) return false;
        long logChannelID = serverSettings.getLogChannelID().get();
        if (!leaveEvent.getServer().getTextChannelById(logChannelID).isPresent()) return false;
        return true;
    }

    private AuditLogEntry lastKickEntry;
    private User lastKickedUser;
    private boolean wasKicked() {
        try {
            AuditLog auditLog = leaveEvent.getServer().getAuditLog(1, AuditLogActionType.MEMBER_KICK).get(3, TimeUnit.SECONDS);
            if (auditLog.getEntries().isEmpty()) return false;
            lastKickEntry = auditLog.getEntries().get(0);
            lastKickedUser = lastKickEntry.getTarget().get().asUser().get(3, TimeUnit.SECONDS);
            if (leaveEvent.getUser().getId() == lastKickedUser.getId()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        
    }

    private void logKick() {
        leaveEvent.getServer().getTextChannelById(serverSettings.getLogChannelID().get()).ifPresent(logChannel -> {
            logChannel.sendMessage(LogEmbed.getEmbed(
                EmbedType.Kick, lastKickedUser, lastKickEntry.getUser().join(), lastKickEntry.getReason().orElse("")
            ));
        });
    }
}
