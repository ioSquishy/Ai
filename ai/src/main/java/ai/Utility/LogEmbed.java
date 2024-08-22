package ai.Utility;

import java.awt.Color;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;

import ai.API.OpenAI.ModerationEndpoint.ModerationResult;

public class LogEmbed {
    public static enum EmbedType {
        Ban,
        Unban,
        Kick,
        Mute,
        Unmute
    }

    /**
     * Returns a modlog embed based on type.
     * @param type
     * @param offender
     * @param moderator
     * @return
     */
    public static EmbedBuilder getEmbed(EmbedType type, User offender, User moderator) {
        return getEmbed(type, offender, moderator, null, null);
    }

    /**
     * Returns a modlog embed based on type.
     * @param type
     * @param offender
     * @param moderator
     * @param reason optional, can be null
     * @return
     */
    public static EmbedBuilder getEmbed(EmbedType type, User offender, User moderator, String reason) {
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
    public static EmbedBuilder getEmbed(EmbedType type, User offender, User moderator, String duration, String reason) {
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

    private static EmbedBuilder standardEmbed(EmbedType type, Color color, User offender, User moderator, String reason) {
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

    private static EmbedBuilder timedEmbed(EmbedType type, Color color, User offender, User moderator, String duration, String reason) {
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

    public static EmbedBuilder aiModEmbed(User offender, Message message, ModerationResult modResult) {
        return new EmbedBuilder()
            .setTitle("AiMod: Message Flagged")
            .setUrl(message.getLink().toString())
            .setColor(Color.YELLOW)
            .setDescription(
                "**Offender:** " + offender.getName() + " " + offender.getMentionTag() + 
                "\n**Flag Reasons:** " + modResult.flagReasons + 
                "\n**Message:** \n" + modResult.inputText
                )
            .setFooter("ID: " + offender.getIdAsString())
            .setTimestampToNow();
    }
    
}
