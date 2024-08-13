package ai.Data;

import java.util.List;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.javacord.api.interaction.InteractionBase;
import java.util.Optional;

import ai.Constants.DatabaseKey;
import ai.Data.Database.DocumentUnavailableException;

public class ServerSettings {
    private long serverId;

    public ServerSettings(long serverId) {
        this.serverId = serverId;
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
    public String getSettingsJSON() throws DocumentUnavailableException {
        Document settingsCopy = Database.cloneDocument(Database.getServerDoc(serverId));
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
        Document updatedSettings = Database.updateDocument(serverId, Database.getServerDoc(serverId), updates); // causes DocumentUnavailableException and ClassCastException
        Database.putDocInCache(serverId, updatedSettings);
    }

    public List<Long> getJoinRoleIDs() throws DocumentUnavailableException {
        return Database.getLongList(Database.getServerDoc(serverId), DatabaseKey.joinRoleIDs);
    }
    
    public void setMuteRoleID(Long id) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put(DatabaseKey.muteRoleID, id);
    }
    public Optional<Long> getMuteRoleID() throws DocumentUnavailableException {
        return Optional.ofNullable(Database.getServerDoc(serverId).getLong(DatabaseKey.muteRoleID));
    }

    public boolean isJoinMessageEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean(DatabaseKey.joinMessageEnabled);
    }

    public void setJoinMessageChannelID(Long channelID) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put(DatabaseKey.joinMessageChannelID, channelID);
    }

    public Optional<Long> getJoinMessageChannelID() throws DocumentUnavailableException {
        return Optional.ofNullable(Database.getServerDoc(serverId).getLong(DatabaseKey.joinMessageChannelID));
    }

    public String getJoinMessage() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getString(DatabaseKey.joinMessage);
    }

    public void setJoinMessage(String newJoinMessage) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put(DatabaseKey.joinMessage, newJoinMessage);
    }

    public void setLogChannelID(Long id) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put(DatabaseKey.logChannelID, id);
    }

    public Optional<Long> getLogChannelID() throws DocumentUnavailableException {
        return Optional.ofNullable(Database.getServerDoc(serverId).getLong(DatabaseKey.logChannelID));
    }

    public boolean isModLogEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean(DatabaseKey.modLogEnabled, false);
    }

    public boolean isLogBanEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean(DatabaseKey.logBans, false);
    }

    public boolean isLogMuteEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean(DatabaseKey.logMutes, false);
    }

    public boolean isLogKicksEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean(DatabaseKey.logKicks, false);
    }
    
}
