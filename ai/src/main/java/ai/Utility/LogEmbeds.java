package ai.Utility;

import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;

public class LogEmbeds {
    public LogEmbeds() {

    }

    public EmbedBuilder standardEmbed(String type, User moderator, User offender, String reason) {
        if (reason.isEmpty()) reason = "*none*";
        return new EmbedBuilder().setTitle(type)
            .setColor(Color.RED)
            .setDescription(
                "**Offender:** " + offender.getName() + " " + offender.getMentionTag() + "\n" +
                "**Reason:** " + reason + "\n" +
                "**Moderator:** " +  moderator.getName() + " " + moderator.getMentionTag())
            .setFooter("ID: " + offender.getIdAsString())
            .setTimestampToNow();
    }

    public EmbedBuilder mute(User offender, User moderator, String duration, String reason) {
        if (duration.length() < 1) {
            duration = "Forever";
        }
        return new EmbedBuilder()
            .setTitle("Mute")
            .setColor(Color.RED)
            .setDescription(
                "**Offender:** " + offender.getName() + " " + offender.getMentionTag() + 
                "\n**Duration:** " + duration +
                "\n**Reason:** " + reason + 
                "\n**Moderator:** " + moderator.getName() + " " + moderator.getMentionTag()
                )
            .setFooter("ID: " + offender.getIdAsString())
            .setTimestampToNow();
    }

    public EmbedBuilder unmute(User offender, User moderator) {
        return new EmbedBuilder()
                .setTitle("Unmute")
                .setColor(Color.GREEN)
                .setDescription(
                    "**Offender:** " + offender.getName() + " " + offender.getMentionTag() + 
                    "\n**Moderator:** " + moderator.getName() + " " + moderator.getMentionTag()
                    )
                .setFooter("ID: " + offender.getIdAsString())
                .setTimestampToNow();
    }

    public EmbedBuilder unban(User offender, User moderator) {
        return new EmbedBuilder()
            .setTitle("Unban")
            .setColor(Color.GREEN)
            .setDescription(
                    "**Offender:** " + offender.getName() + " " + offender.getMentionTag() + 
                    "\n**Moderator:** " + moderator.getName() + " " + moderator.getMentionTag()
                )
            .setFooter("ID: " + offender.getIdAsString())
            .setTimestampToNow();    
    }
    
}
