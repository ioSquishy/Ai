package ai.Data;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.javacord.api.interaction.InteractionBase;

import com.squareup.moshi.JsonDataException;

import java.util.Optional;

import ai.Data.Database.DocumentUnavailableException;

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
        serverDocument.settings.eventSettings.joinSettings.joinMessage = newJoinMessage;
    }

    public void updateSettings(String settingsJSON) throws JsonDataException {
        serverDocument.setSettings(settingsJSON);
    }
    
    public String getSettingsJSON() {
        return serverDocument.settings.toString();
    }

    // get commands
    
}
