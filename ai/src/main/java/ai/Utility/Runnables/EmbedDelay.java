package ai.Utility.Runnables;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import ai.App;
import ai.Utility.TaskScheduler;

public class EmbedDelay {
    private final EmbedBuilder embed;
    private final long textChannelID;
    private final long serverID;

    public EmbedDelay(EmbedBuilder embed, long textChannelID, long serverID) {
        this.embed = embed;
        this.textChannelID = textChannelID;
        this.serverID = serverID;
    }

    public Runnable sendEmbedRunnable() {
        return () -> {
            App.api.getTextChannelById(textChannelID).ifPresentOrElse(
                textChannel -> textChannel.sendMessage(embed),
                () -> TaskScheduler.sendErrorMessage(serverID, "Unable to send embed to <#" + textChannelID + ">"));
        };
    }
}