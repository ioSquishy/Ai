package ai;

import java.util.Collections;
import java.util.List;

import org.bson.Document;
import org.javacord.api.entity.channel.ServerChannel;

import ai.Database.DocumentUnavailableException;

public class ServerSettings {
    private long serverId;

    // remove all variables except serverID to make everything static
    private Long muteRoleID;

    private boolean modLogEnabled;
    private Long logChannelID;
    private boolean logBans;
    private boolean logMutes;
    private boolean logKicks;

    private boolean joinMessageEnabled;
    private Long joinMessageChannelID;
    private String joinMessage;
    private List<Long> joinRoleIDs;

    public ServerSettings(long serverId) {
        this.serverId = serverId;
        
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

    public String getJoinMessage() {
        return joinMessage;
    }

    public void setJoinMessage(String newJoinMessage) throws DocumentUnavailableException {
        Database.getServerDoc(serverId).put("joinMessage", newJoinMessage);
        this.joinMessage = newJoinMessage;
    }

    public String getJoinRoles() {
        String ids = "";
        for (Long id : joinRoleIDs) {
            ids += "<@&" + id + "> ";
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public void pullAllSettings() throws DocumentUnavailableException {
        Document mongoDoc = Database.getServerDoc(serverId);
        muteRoleID = mongoDoc.getLong("muteRoleID");

        modLogEnabled = mongoDoc.getBoolean("modLogEnabled", false);
        logChannelID = mongoDoc.getLong("logChannelID");
        logBans = mongoDoc.getBoolean("logBans", false);
        logMutes = mongoDoc.getBoolean("logMutes", false);
        logKicks = mongoDoc.getBoolean("logKicks", false);

        joinMessageChannelID = mongoDoc.getLong("joinMessageChannelID");
        joinMessage = mongoDoc.getString("joinMessage");
        joinRoleIDs = mongoDoc.getList("joinRoleIDs", long.class, Collections.EMPTY_LIST);
    }

    public Long getServerId() {
        return serverId;
    }
    
    public void setMuteRoleID(Long id) {
        muteRoleID = id;
    }
    public Long getMuteRoleID() {
        return muteRoleID;
    }
    
    public void updateModLogSettings(boolean modLogEnabled, ServerChannel channel, boolean logBans, boolean logMutes, boolean logKicks) {
        this.modLogEnabled = modLogEnabled;
        this.logChannelID = channel != null ? channel.getId() : null;
        this.logBans = logBans;
        this.logMutes = logMutes;
        this.logKicks = logKicks;
    }

    public void setLogChannelID(Long id) {
        logChannelID = id;
    }
    public Long getLogChannelID() {
        return logChannelID;
    }

    public boolean isLogEnabled() {
        return modLogEnabled;
    }
    public boolean isLogBanEnabled() {
        return logBans;
    }
    public boolean isLogMuteEnabled() {
        return logMutes;
    }
    public boolean isLogKicksEnabled() {
        return logKicks;
    }
    
}
