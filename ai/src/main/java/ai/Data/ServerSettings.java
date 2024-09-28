package ai.Data;

import java.time.Instant;

import com.squareup.moshi.JsonDataException;

import java.util.List;
import java.util.Optional;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;

import ai.App;
import ai.Data.Database.DocumentUnavailableException;
import ai.Data.ServerDocument.Settings.EventSettings;
import ai.Data.ServerDocument.Settings.ModerationSettings;
import ai.Data.ServerDocument.Settings.EventSettings.JoinSettings;
import ai.Data.ServerDocument.Settings.ModerationSettings.AiModSettings;
import ai.Data.ServerDocument.Settings.ModerationSettings.ModLogSettings;

public class ServerSettings {
    private ServerDocument serverDocument;
    private Server server;

    public ServerSettings(Server server) throws DocumentUnavailableException {
        serverDocument = Database.getServerDoc(server.getId());
        serverDocument.settings = serverDocument.settings;
        updateLastCommandTime();

        this.server = server;
    }

    public ServerSettings(long serverID) throws DocumentUnavailableException {
        this(App.api.getServerById(serverID).orElseThrow());
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

    private Optional<ServerTextChannel> getOptionalTextChannel(Long channelID) {
        if (channelID == null) return null;
        return server.getTextChannelById(channelID);
    }
    private Optional<Role> getOptionalRole(Long roleID) {
        if (roleID == null) return null;
        return server.getRoleById(roleID);
    }

    // public get commands
    public String getJoinMessage() {
        return serverDocument.hiddenSettings.joinMessage;
    }

    public boolean isJoinMessageEnabled() {
        return joinSettings().joinMessageEnabled;
    }

    public Optional<ServerTextChannel> getJoinMessageChannel() {
        return getOptionalTextChannel(joinSettings().joinMessageChannelID);
    }

    public Optional<Role> getMuteRole() {
        return getOptionalRole(modSettings().muteRoleID);
    }

    public Optional<ServerTextChannel> getModLogChannel() {
        return getOptionalTextChannel(modLogSettings().modLogChannelID);
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

    public boolean isAiModImageCheckEnabled() {
        return aiModSettings().checkImages;
    }

    public Optional<ServerTextChannel> getAiLogChannel() {
        return getOptionalTextChannel(aiModSettings().aiLogChannelID);
    }

    public List<Long> getAiIgnoredChannels() {
        return aiModSettings().ignoredChannels;
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

    public boolean flagIllicit() {
        return aiModSettings().flagIllicit;
    }

}
