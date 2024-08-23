package ai.Data;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.bson.Document;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;

import ai.App;
import ai.Data.Database.DocumentUnavailableException;

public class ServerDocument {
    private static transient final JsonAdapter<ServerDocument> documentAdapter = App.Moshi.adapter(ServerDocument.class).serializeNulls();
    protected static transient final JsonAdapter<ServerDocument.Settings> settingsAdapter = App.Moshi.adapter(ServerDocument.Settings.class).serializeNulls();

    // this is required to ensure new values are initialized to default values
    @SuppressWarnings("unused")
    private ServerDocument() {
        _id = 0;
    }

    protected final long _id;
    public ServerDocument(long serverID) {
        this._id = serverID;
    }
    public long lastCommandEpochSecond = Instant.now().getEpochSecond();

    public Settings settings = new Settings();
    public static class Settings {
        public ModerationSettings moderationSettings = new ModerationSettings();
        public static class ModerationSettings {
            public Long muteRoleID = null;
            public Long modLogChannelID = null;
            public boolean modLogEnabled = false;

            public ModLogSettings modLogSettings = new ModLogSettings();
            public static class ModLogSettings {
                public boolean logBans = true;
                public boolean logMutes = true;
                public boolean logKicks = true;
            }

            public Long aiModLogChannelID = null;
            public boolean aiModEnabled = false;
            public AiModSettings aiModSettings = new AiModSettings();
            public static class AiModSettings {
                public boolean flagHate = true;
                public boolean flagHarrassment = true;
                public boolean flagSelfHarm = true;
                public boolean flagSexual = true;
                public boolean flagViolence = true;
            }
        }

        public EventSettings eventSettings = new EventSettings();
        public static class EventSettings {
            public JoinSettings joinSettings = new JoinSettings();
            public static class JoinSettings {
                public Long joinMessageChannelID = null;
                public boolean joinMessageEnabled = false;
                public String joinMessage = null;
                public List<Long> joinRoleIDs = Collections.<Long>emptyList();
            }
        }
    }

    // methods
    public String toJson() {
        return documentAdapter.toJson(this);
    }

    public Document toBsonDocument() {
        return Document.parse(toJson());
    }

    public void updateSettings(ServerDocument.Settings serverSettings) {
        System.out.println(Document.parse(settingsAdapter.toJson(serverSettings)).keySet());
    }

    @Override
    public String toString() {
        return documentAdapter.indent("    ").toJson(this);
    }

    public void update() {
        
    }

    // static methods
    public static ServerDocument fromJson(String json) throws DocumentUnavailableException {
        try {
            return documentAdapter.fromJson(json);
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
            throw new DocumentUnavailableException(e.getMessage());
        }
    }
    
    public static ServerDocument fromBsonDocument(Document document) throws DocumentUnavailableException {
        return ServerDocument.fromJson(document.toJson());
    }
}
