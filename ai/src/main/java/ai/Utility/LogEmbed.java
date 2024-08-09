package ai.Utility;

import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;

public class LogEmbed {
    public static enum EmbedType {
        Ban,
        Unban,
        Kick,
        Mute,
        Unmute
    }

    public EmbedBuilder getEmbed(EmbedType type, User offender, User moderator) {
        return getEmbed(type, offender, moderator, null, null);
    }

    public EmbedBuilder getEmbed(EmbedType type, User offender, User moderator, String reason) {
        return getEmbed(type, offender, moderator, null, reason);
    }

    /**
     * Returns a modlog embed based on type.
     * @param type
     * @param offender
     * @param moderator
     * @param duration optional, can be null
     * @param reason optional, can be null
     * @return
     */
    public EmbedBuilder getEmbed(EmbedType type, User offender, User moderator, String duration, String reason) {
        switch (type) {
            case Ban:
                return standardEmbed(type, Color.RED, offender, moderator, reason);
            case Kick:
                return standardEmbed(type, Color.RED, offender, moderator, reason);
            case Mute:
                return timedEmbed(type, Color.RED, offender, moderator, duration, reason);
            case Unban:
                return standardEmbed(type, Color.GREEN, offender, moderator, reason);
            case Unmute:
                return standardEmbed(type, Color.GREEN, offender, moderator, reason);
        }
        return null;
    }

    private EmbedBuilder standardEmbed(EmbedType type, Color color, User offender, User moderator, String reason) {
        if (reason == null || reason.isEmpty()) reason = "*none*";
        return new EmbedBuilder().setTitle(type.toString())
            .setColor(color)
            .setDescription(
                "**Offender:** " + offender.getName() + " " + offender.getMentionTag() + "\n" +
                "**Reason:** " + reason + "\n" +
                "**Moderator:** " +  moderator.getName() + " " + moderator.getMentionTag())
            .setFooter("ID: " + offender.getIdAsString())
            .setTimestampToNow();
    }

    private EmbedBuilder timedEmbed(EmbedType type, Color color, User offender, User moderator, String duration, String reason) {
        if (reason == null || reason.isEmpty()) reason = "*none*";
        if (duration.length() < 1) duration = "Forever";

        return new EmbedBuilder()
            .setTitle(type.toString())
            .setColor(color)
            .setDescription(
                "**Offender:** " + offender.getName() + " " + offender.getMentionTag() + 
                "\n**Duration:** " + duration +
                "\n**Reason:** " + reason + 
                "\n**Moderator:** " + moderator.getName() + " " + moderator.getMentionTag()
                )
            .setFooter("ID: " + offender.getIdAsString())
            .setTimestampToNow();
    }
    
}
