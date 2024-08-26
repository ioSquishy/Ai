package ai.Data;

import java.time.Instant;

import com.squareup.moshi.JsonDataException;

import java.util.Optional;

import ai.Data.Database.DocumentUnavailableException;
import ai.Data.ServerDocument.Settings.EventSettings;
import ai.Data.ServerDocument.Settings.ModerationSettings;
import ai.Data.ServerDocument.Settings.EventSettings.JoinSettings;
import ai.Data.ServerDocument.Settings.ModerationSettings.AiModSettings;
import ai.Data.ServerDocument.Settings.ModerationSettings.ModLogSettings;

public class ServerSettings {
    private ServerDocument serverDocument;

    public ServerSettings(long serverId) throws DocumentUnavailableException {
        serverDocument = Database.getServerDoc(serverId);
        serverDocument.settings = serverDocument.settings;
        updateLastCommandTime();
    }

    public long getServerId() {
        return serverDocument._id;
    }

    public void updateLastCommandTime() {
        serverDocument.lastCommandEpochSecond = Instant.now().getEpochSecond();
    }

    public void setJoinMessage(String newJoinMessage) {
        serverDocument.hiddenSettings.joinMessage = newJoinMessage;
    }

    public void updateSettings(String settingsJSON) throws JsonDataException {
        serverDocument.setSettings(settingsJSON);
    }
    
    public String getSettingsJSON() {
        return serverDocument.settings.toString();
    }

    // private get commands

    private ModerationSettings modSettings() {
        return serverDocument.settings.moderationSettings;
    }
    private ModLogSettings modLogSettings() {
        return modSettings().modLogSettings;
    }
    private AiModSettings aiModSettings() {
        return modSettings().aiModSettings;
    }

    private EventSettings eventSettings() {
        return serverDocument.settings.eventSettings;
    }
    private JoinSettings joinSettings() {
        return eventSettings().joinSettings;
    }

    // public get commands
    public String getJoinMessage() {
        return serverDocument.hiddenSettings.joinMessage;
    }

    public boolean isJoinMessageEnabled() {
        return joinSettings().joinMessageEnabled;
    }

    public Optional<Long> getJoinMessageChannelID() {
        return Optional.ofNullable(joinSettings().joinMessageChannelID);
    }

    public Optional<Long> getMuteRoleID() {
        return Optional.ofNullable(modSettings().muteRoleID);
    }

    public Optional<Long> getModLogChannelID() {
        return Optional.ofNullable(modLogSettings().modLogChannelID);
    }

    public boolean isModLogEnabled() {
        return modLogSettings().modLogEnabled;
    }

    public boolean isLogMuteEnabled() {
        return modLogSettings().logMutes;
    }

    public boolean isLogBanEnabled() {
        return modLogSettings().logBans;
    }

    public boolean isLogKicksEnabled() {
        return modLogSettings().logKicks;
    }

    // ai mod
    public boolean isAiModEnabled() {
        return aiModSettings().aiModEnabled;
    }

    public boolean flagHate() {
        return aiModSettings().flagHate;
    }

    public boolean flagHarrassment() {
        return aiModSettings().flagHarrassment;
    }

    public boolean flagSelfHarm() {
        return aiModSettings().flagSelfHarm;
    }

    public boolean flagSexual() {
        return aiModSettings().flagSexual;
    }

    public boolean flagViolence() {
        return aiModSettings().flagViolence;
    }

}
