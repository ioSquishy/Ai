package ai.Utility.Runnables;

import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import ai.App;
import ai.Utility.LogEmbed;
import ai.Utility.LogEmbed.EmbedType;

public class TempMute {
    private final long muteRoleID;
    private final long mutedUserID;
    private final long logChannelID;
    private final long serverID;

    public TempMute(long muteRoleID, long mutedUserID, long logChannelID, long serverID) {
        this.muteRoleID = muteRoleID;
        this.mutedUserID = mutedUserID;
        this.logChannelID = logChannelID;
        this.serverID = serverID;
    }

    public Runnable unmuteRunnable() {
        return () -> {
            try {
                new RoleDelay(muteRoleID, serverID, mutedUserID).removeRoleRunnable().run();

                EmbedBuilder embed = LogEmbed.getEmbed(
                    EmbedType.Unmute,
                    App.api.getUserById(mutedUserID).get(),
                    App.api.getYourself(), 
                    "Temporary mute duration over.");
                new EmbedDelay(embed, logChannelID, serverID).sendEmbedRunnable().run();
            } catch (InterruptedException | ExecutionException e) {}
            
        };
    }
}
