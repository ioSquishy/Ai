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
    private static transient final JsonAdapter<ServerDocument.Settings> settingsAdapter = App.Moshi.adapter(ServerDocument.Settings.class).serializeNulls();

    // this is required to ensure new values are initialized to default values
    @SuppressWarnings("unused")
    private ServerDocument() {
        _id = 0;
    }

    protected final long _id;
    protected ServerDocument(long serverID) {
        this._id = serverID;
    }
    protected long lastCommandEpochSecond = Instant.now().getEpochSecond();

    protected Settings settings = new Settings();
    protected static class Settings {
        protected ModerationSettings moderationSettings = new ModerationSettings();
        protected static class ModerationSettings {
            protected Long muteRoleID = null;

            protected ModLogSettings modLogSettings = new ModLogSettings();
            protected static class ModLogSettings {
                protected boolean modLogEnabled = false;
                protected Long modLogChannelID = null;
                
                protected boolean logBans = true;
                protected boolean logMutes = true;
                protected boolean logKicks = true;
            }
            
            protected AiModSettings aiModSettings = new AiModSettings();
            protected static class AiModSettings {
                protected boolean aiModEnabled = false;
                protected Long aiLogChannelID = null;
                protected List<Long> ignoredChannels = Collections.<Long>emptyList();
                protected boolean checkImages = true;

                // protected boolean autoMuteEnabled = true;
                // protected int autoMuteWarnThreshold = 3; // mutes user after 3 warnings

                protected boolean flagHate = true;
                protected boolean flagHarrassment = true;
                protected boolean flagSelfHarm = true;
                protected boolean flagSexual = true;
                protected boolean flagViolence = true;
                protected boolean flagIllicit = true;
            }
        }

        protected EventSettings eventSettings = new EventSettings();
        protected static class EventSettings {
            protected JoinSettings joinSettings = new JoinSettings();
            protected static class JoinSettings {
                protected Long joinMessageChannelID = null;
                protected boolean joinMessageEnabled = false;
                
                protected List<Long> joinRoleIDs = Collections.<Long>emptyList();
            }
        }

        @Override
        public String toString() {
            return settingsAdapter.indent("    ").toJson(this);
        }
    }

    protected HiddenSettings hiddenSettings = new HiddenSettings();
    protected static class HiddenSettings {
        protected String joinMessage = null;
    }

    // methods
    protected String toJson() {
        return documentAdapter.toJson(this);
    }

    protected Document toBsonDocument() {
        return Document.parse(toJson());
    }

    protected void setSettings(String settingsJSON) throws JsonDataException {
        try {
            this.settings = settingsAdapter.failOnUnknown().fromJson(settingsJSON);
        } catch (IOException | JsonDataException e) {
            throw new JsonDataException(e);
        };
    }

    @Override
    public String toString() {
        return documentAdapter.indent("    ").toJson(this);
    }

    // static methods
    protected static ServerDocument fromJson(String json) throws DocumentUnavailableException {
        try {
            return documentAdapter.fromJson(json);
        } catch (JsonDataException | IOException e) {
            e.printStackTrace();
            throw new DocumentUnavailableException(e.getMessage());
        }
    }
    
    protected static ServerDocument fromBsonDocument(Document document) throws DocumentUnavailableException {
        return ServerDocument.fromJson(document.toJson());
    }
}
