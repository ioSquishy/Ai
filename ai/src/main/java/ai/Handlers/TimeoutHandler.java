package ai.Handlers;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.auditlog.AuditLogActionType;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.user.UserChangeTimeoutEvent;
import org.tinylog.Logger;

import ai.Data.Database.DocumentUnavailableException;
import ai.App;
import ai.Data.ServerSettings;
import ai.Utility.LogEmbed;
import ai.Utility.PermissionsCheck;
import ai.Utility.LogEmbed.EmbedType;
import ai.Utility.ReadableTime;

public class TimeoutHandler {
    
    public static void handleTimeoutEvent(UserChangeTimeoutEvent event) {
        try {
            Server server = event.getServer();

            ServerSettings serverSettings = new ServerSettings(server);
            if (isLogMutesEnabled(serverSettings) && PermissionsCheck.canReadAuditLog(server, true) && PermissionsCheck.canSendMessages(serverSettings.getModLogChannel().get(), true)) {

                AuditLogEntry lastTimeout = event.getServer().getAuditLog(1, AuditLogActionType.MEMBER_UPDATE).join().getEntries().get(0);
                if (!wasMutedByBot(lastTimeout)) { // if muted by bot then it was logged already with mute command
                    logTimeout(event, serverSettings, lastTimeout);
                }
            }
        } catch (DocumentUnavailableException e) {
            Logger.warn(e);
        }
    }

    private static boolean isLogMutesEnabled(ServerSettings serverSettings) {
        if (!serverSettings.isLogMuteEnabled()) return false;
        if (!serverSettings.isModLogEnabled()) return false;
        if (!serverSettings.getModLogChannel().isPresent()) return false;
        return true;
    }

    private static boolean wasMutedByBot(AuditLogEntry lastTimeout) {
        return lastTimeout.getUser().join().getId() == App.botID;
    }

    private static void logTimeout(UserChangeTimeoutEvent event, ServerSettings serverSettings, AuditLogEntry lastTimeout) {
        if (event.getNewTimeout().isPresent()) { //checks if timeout was set or removed
            serverSettings.getModLogChannel().ifPresent(channel -> {
                if (!PermissionsCheck.canSendMessages(channel, true)) return;
                try {
                    channel.sendMessage(LogEmbed.getEmbed(EmbedType.Mute, event.getUser(), lastTimeout.getUser().get(), new ReadableTime().compute(event.getNewTimeout().get().getEpochSecond()-Instant.now().getEpochSecond()), lastTimeout.getReason().orElse("")));
                } catch (InterruptedException | ExecutionException e) {
                    Logger.error(e);
                }
            });
        } else {
            serverSettings.getModLogChannel().ifPresent(channel -> {
                if (!PermissionsCheck.canSendMessages(channel, true)) return;
                try {
                    channel.sendMessage(LogEmbed.getEmbed(EmbedType.Unmute, event.getUser(), lastTimeout.getUser().get()));
                } catch (InterruptedException | ExecutionException e) {
                    Logger.error(e);
                }
            });
        }
    }
}
