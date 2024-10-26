package ai.Utility;

import java.awt.Color;
import java.util.Optional;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.InteractionBase;

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
                "**Reason:** " + reason.trim() + "\n" +
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
                "\n**Reason:** " + reason.trim() + 
                "\n**Moderator:** " + moderator.getName() + " " + moderator.getMentionTag()
                )
            .setFooter("ID: " + offender.getIdAsString())
            .setTimestampToNow();
    }

    public static EmbedBuilder lockdownEmbed(User moderator, boolean initiated) {
        EmbedBuilder embed = new EmbedBuilder()
            .setDescription("Moderator: " + moderator.getMentionTag())
            .setTimestampToNow();
        if (initiated) {
            embed.setColor(Color.RED);
            embed.setTitle("Lockdown Enabled");
        } else {
            embed.setColor(Color.GREEN);
            embed.setTitle("Lockdown Disabled");
        }
        return embed;
    }

    public static EmbedBuilder aiModEmbed(User offender, String messageLink, Optional<String[]> imageURLs, ModerationResult modResult) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("AiMod: Message Flagged")
            .setUrl(messageLink)
            .setColor(Color.YELLOW)
            .setDescription(
                "**Offender:** " + offender.getName() + " " + offender.getMentionTag() + 
                "\n**Flag Reasons:** " + modResult.flags.toString() + 
                "\n**Message Text:** \n" + modResult.inputText
                )
            .setFooter("ID: " + offender.getIdAsString())
            .setTimestampToNow();
        if (imageURLs.isPresent()) {
            for (String imageURL : imageURLs.get()) {
                embed.addField("Flagged Attachment", imageURL);
            }
        }
        return embed;
    }

    /**
     * Returns moderator information that should be appended to audit log reasons on moderation actions.
     * @param interaction Slash command interaction
     * @return \nModerator: (name) (user id)
     */
    public static String getModeratorAppendage(InteractionBase interaction) {
        return "\nModerator: " + interaction.getUser().getName() + " " + interaction.getUser().getId();
    }
    
    /**
     * Separates moderator userID from audit log reason that would've been added with getModeratorAppendage()
     * @param auditReason Audit log reason containing moderator appendage.
     * @return {reason, moderatorID}
     */
    public static String[] separateReasonAndModerator(String auditReason) {
        String[] splitAuditReason = auditReason.split("\n");

        String rawModerator = splitAuditReason[splitAuditReason.length-1];
        String reason = auditReason.substring(0, (auditReason.length()-rawModerator.length()));

        String moderatorID = rawModerator.split(" ")[2];

        return new String[]{reason, moderatorID};
    }
    
}
