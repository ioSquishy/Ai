package ai.Handlers;

import org.javacord.api.event.message.MessageCreateEvent;

import ai.Utility.AiMod;

public class MessageHandler {
    // check if logging ai warnings is enabled
    // check if mod log is enabled
    // check if mod log channel is present
    // submit message to moderation endpoint and log if flagged
    public static void handleMessage(MessageCreateEvent event) {
        AiMod.moderateMessage(event.getMessage());
    }
}
