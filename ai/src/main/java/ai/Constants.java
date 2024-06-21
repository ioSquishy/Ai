package ai;

public class Constants {
    public enum CustomID {
        SETTINGS_MODAL,
        SETTINGS_JSON,
        JOIN_MESSAGE
    }

    public static class DatabaseKey {
        final static String id = "_id";
        final static String documentVersion = "documentVersion";
        final static String lastCommand = "lastCommand";
        final static String muteRoleID = "muteRoleID";
        final static String modLogEnabled = "modLogEnabled";
        final static String logChannelID = "logChannelID";
        final static String logBans = "logBans";
        final static String logMutes = "logMutes";
        final static String logKicks = "logKicks";
        final static String joinMessageEnabled = "joinMessageEnabled";
        final static String joinMessageChannelID = "joinMessageChannelID";
        final static String joinMessage = "joinMessage";
        /**
         * List < Long >
         */
        final static String joinRoleIDs = "joinRoleIDs";
    }
}
