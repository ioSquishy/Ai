package ai.Data;

import java.time.Instant;
import java.util.List;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.javacord.api.interaction.InteractionBase;
import java.util.Optional;

import ai.Data.Database.DocumentUnavailableException;

public class ServerSettings {
    private ServerDocument serverDocument;
    private ServerDocument.Settings serverSettings;

    public ServerSettings(long serverId) throws DocumentUnavailableException {
        serverDocument = Database.getServerDoc(serverId);
        serverSettings = serverDocument.settings;
        updateLastCommandTime();
    }

    public long getServerId() {
        return serverDocument._id;
    }

    public void updateLastCommandTime() {
        serverDocument.lastCommandEpochSecond = Instant.now().getEpochSecond();
    }

    public void setJoinMessage(String newJoinMessage) {
        serverSettings.eventSettings.joinSettings.joinMessage = newJoinMessage;
    }

    public void updateSettings(String settingsJSON) throws InvalidSettingsJsonException, ClassCastException, DocumentUnavailableException {
        Document updates = verifySettingsJSON(settingsJSON); // causes InvalidSettingsJsonException
        Document updatedSettings = Database.updateDocument(serverId, settings, updates); // causes DocumentUnavailableException and ClassCastException
        settings.putAll(updatedSettings); // updates doc in cache as well
    }
    
    public String getSettingsJSON() {
        Document settingsCopy = Database.cloneDocument(settings.toBsonDocument());
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

    // get commands
    
}
