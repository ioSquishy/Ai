package ai.Data;

import java.util.List;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.javacord.api.interaction.InteractionBase;
import java.util.Optional;

import ai.Constants.DatabaseKey;
import ai.Data.Database.DocumentUnavailableException;

public class ServerSettings {
    private final long serverId;
    private Document settings;

    public ServerSettings(long serverId) throws DocumentUnavailableException {
        this.serverId = serverId;
        settings = Database.getServerDoc(serverId);
    }

    public long getServerId() {
        return this.serverId;
    }

    private static final List<String> editableKeys = List.of(
        DatabaseKey.muteRoleID, 
        DatabaseKey.modLogEnabled, 
        DatabaseKey.logChannelID, 
        DatabaseKey.logBans, 
        DatabaseKey.logMutes, 
        DatabaseKey.logKicks, 
        DatabaseKey.joinMessageEnabled, 
        DatabaseKey.joinMessageChannelID, 
        DatabaseKey.joinRoleIDs
    );
    public String getSettingsJSON() {
        Document settingsCopy = Database.cloneDocument(settings);
        settingsCopy.keySet().retainAll(editableKeys);
        return settingsCopy.toJson(JsonWriterSettings.builder().indent(true).build());
    }

    private Document verifySettingsJSON(String settingsJSON) throws InvalidSettingsJsonException {
        try {
            Document settings = Document.parse(settingsJSON);
            settings.keySet().retainAll(editableKeys);
            if (settings.keySet().containsAll(editableKeys) == false) {
                throw new InvalidSettingsJsonException();
            }
            return settings;
        } catch (Exception e) {
            throw new InvalidSettingsJsonException();
        }
    }
    public static class InvalidSettingsJsonException extends Exception {
        public InvalidSettingsJsonException() {
            super();
        }
        public static String getStandardResponseString() {
            return "Invalid settings format. No changes applied."; 
        }
        public static void sendStandardResponse(InteractionBase interaction) {
            interaction.createImmediateResponder().setContent(getStandardResponseString()).respond();
        }
    }

    public void updateSettings(String settingsJSON) throws InvalidSettingsJsonException, ClassCastException, DocumentUnavailableException {
        Document updates = verifySettingsJSON(settingsJSON); // causes InvalidSettingsJsonException
        Document updatedSettings = Database.updateDocument(serverId, settings, updates); // causes DocumentUnavailableException and ClassCastException
        settings.putAll(updatedSettings); // updates doc in cache as well
    }

    public List<Long> getJoinRoleIDs() {
        return Database.getLongList(settings, DatabaseKey.joinRoleIDs);
    }
    
    public void setMuteRoleID(Long id) {
        settings.put(DatabaseKey.muteRoleID, id);
    }
    public Optional<Long> getMuteRoleID() {
        return Optional.ofNullable(settings.getLong(DatabaseKey.muteRoleID));
    }

    public boolean isJoinMessageEnabled() {
        return settings.getBoolean(DatabaseKey.joinMessageEnabled);
    }

    public void setJoinMessageChannelID(Long channelID) {
        settings.put(DatabaseKey.joinMessageChannelID, channelID);
    }

    public Optional<Long> getJoinMessageChannelID() {
        return Optional.ofNullable(settings.getLong(DatabaseKey.joinMessageChannelID));
    }

    public String getJoinMessage() {
        return settings.getString(DatabaseKey.joinMessage);
    }

    public void setJoinMessage(String newJoinMessage) {
        settings.put(DatabaseKey.joinMessage, newJoinMessage);
    }

    public void setLogChannelID(Long id) {
        settings.put(DatabaseKey.logChannelID, id);
    }

    public Optional<Long> getLogChannelID() {
        return Optional.ofNullable(settings.getLong(DatabaseKey.logChannelID));
    }

    public boolean isModLogEnabled() {
        return settings.getBoolean(DatabaseKey.modLogEnabled, false);
    }

    public boolean isLogBanEnabled() {
        return settings.getBoolean(DatabaseKey.logBans, false);
    }

    public boolean isLogMuteEnabled() {
        return settings.getBoolean(DatabaseKey.logMutes, false);
    }

    public boolean isLogKicksEnabled() {
        return settings.getBoolean(DatabaseKey.logKicks, false);
    }
    
}
