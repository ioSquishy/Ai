package ai;

import java.util.List;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.javacord.api.interaction.InteractionBase;

import ai.Database.DocumentUnavailableException;

public class ServerSettings {
    private long serverId;

    // private Long muteRoleID;

    // private boolean modLogEnabled;
    // private Long logChannelID;
    // private boolean logBans;
    // private boolean logMutes;
    // private boolean logKicks;

    // private boolean joinMessageEnabled;
    // private Long joinMessageChannelID;
    // private String joinMessage;
    // private List<Long> joinRoleIDs;

    public ServerSettings(long serverId) {
        this.serverId = serverId;
    }

    public long getServerId() {
        return this.serverId;
    }

    private static final List<String> editableKeys = List.of(
        "muteRoleID", 
        "modLogEnabled", 
        "logChannelID", 
        "logBans", 
        "logMutes", 
        "logKicks", 
        "joinMessageEnabled", 
        "joinMessageChannelID", 
        "joinRoleIDs"
    );
    public String getSettingsJSON() throws DocumentUnavailableException {
        Document settingsCopy = Database.cloneDocument(Database.getServerDoc(serverId));
        settingsCopy.keySet().retainAll(editableKeys);
        return settingsCopy.toJson(JsonWriterSettings.builder().indent(true).build());
    }

    private Document verifySettingsJSON(String settingsJSON) throws InvalidSettingsJsonException {
        Document settings = Document.parse(settingsJSON);
        settings.keySet().retainAll(editableKeys);
        if (settings.keySet().containsAll(editableKeys) == false) {
            throw new InvalidSettingsJsonException();
        }
        return settings;
    }
    public static class InvalidSettingsJsonException extends Exception {
        public InvalidSettingsJsonException() {
            super();
        }
        public static void sendStandardResponse(InteractionBase interaction) {
            interaction.createImmediateResponder().setContent("Invalid settings format. No changes applied.").respond();
        }
    }

    public void updateSettings(String settingsJSON) throws DocumentUnavailableException, InvalidSettingsJsonException {
        Document updates = verifySettingsJSON(settingsJSON);
        Document updatedSettings = Database.updateDocument(serverId, updates);
        Database.putDocInCache(serverId, updatedSettings);
    }
    
    public void setMuteRoleID(Long id) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put("muteRoleID", id);
    }
    public Long getMuteRoleID() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getLong("muteRoleID");
    }

    public boolean isJoinMessageEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean("joinMessageEnabled");
    }

    public void setJoinMessageChannelID(Long channelID) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put("joinMessageChannelID", channelID);
    }

    public Long getJoinMessageChannelID() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getLong("joinMessageChannelID");
    }

    public String getJoinMessage() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getString("joinMessage");
    }

    public void setJoinMessage(String newJoinMessage) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put("joinMessage", newJoinMessage);
    }

    public void setLogChannelID(Long id) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put("logChannelID", id);
    }

    public Long getLogChannelID() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getLong("logChannelID");
    }

    public boolean isModLogEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean("modLogEnabled", false);
    }

    public boolean isLogBanEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean("logBans", false);
    }

    public boolean isLogMuteEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean("logMutes", false);
    }

    public boolean isLogKicksEnabled() throws DocumentUnavailableException {
        return Database.getServerDoc(serverId).getBoolean("logKicks", false);
    }
    
}
