package ai.Data;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.squareup.moshi.JsonAdapter;

import ai.App;

public class ServerDocument {
    public static transient final JsonAdapter<ServerDocument> jsonAdapter = App.Moshi.adapter(ServerDocument.class).serializeNulls();

    public long id_;
    public int documentVersion = 1;
    public long lastCommandEpochSecond = Instant.now().getEpochSecond()/60;

    public ModerationSettings moderationSettings = new ModerationSettings();
    public class ModerationSettings {
        public Long muteRoleID = null;
        public Long modLogChannelID = null;
        public boolean modLogEnabled = false;

        public ModLogSettings modLogSettings = new ModLogSettings();
        public class ModLogSettings {
            public boolean logBans = true;
            public boolean logMutes = true;
            public boolean logKicks = true;
        }

        public Long aiModLogChannelID = null;
        public boolean aiModEnabled = false;
        public AiModSettings aiModSettings = new AiModSettings();
        public class AiModSettings {
            public boolean flagHate = true;
            public boolean flagHarrassment = true;
            public boolean flagSelfHarm = true;
            public boolean flagSexual = true;
            public boolean flagViolence = true;
        }
    }

    public EventSettings eventSettings = new EventSettings();
    public class EventSettings {

        public JoinSettings joinSettings = new JoinSettings();
        public class JoinSettings {
            public Long joinMessageChannelID = null;
            public boolean joinMessageEnabled = false;
            public String joinMessage = null;
            public List<Long> joinRoleIDs = Collections.<Long>emptyList();
        }
    }
}
