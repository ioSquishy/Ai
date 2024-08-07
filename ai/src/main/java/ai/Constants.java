package ai;

public class Constants {
    public static class CustomID {
        public static final String SETTINGS_MODAL = "SETTINGS_MODAL";
        public static final String SETTINGS_JSON = "SETTINGS_JSON";
        public static final String JOIN_MESSAGE = "JOIN_MESSAGE";

        public static final String MANUAL_MUTE = "MANUAL_MUTE";
    }

    public static class DatabaseKey {
        public static final String id = "_id";
        public static final String documentVersion = "documentVersion";
        public static final String lastCommand = "lastCommand";
        public static final String muteRoleID = "muteRoleID";
        public static final String modLogEnabled = "modLogEnabled";
        public static final String logChannelID = "logChannelID";
        public static final String logBans = "logBans";
        public static final String logMutes = "logMutes";
        public static final String logKicks = "logKicks";
        public static final String joinMessageEnabled = "joinMessageEnabled";
        public static final String joinMessageChannelID = "joinMessageChannelID";
        public static final String joinMessage = "joinMessage";
        /**
         * List < Long >
         */
        public static final String joinRoleIDs = "joinRoleIDs";
    }

    public static class GlobalJsonKeys {
        public static final String customID = "customID";
        public static final String moderatorID = "moderatorID";
    }

    public static class MuteJsonKeys {
        public static final String mutedUserID = "mutedUserID";
        public static final String durationSeconds = "durationSeconds";
    }
}
