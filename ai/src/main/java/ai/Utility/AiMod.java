package ai.Utility;

import java.util.Collections;
import java.util.HashMap;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import ai.Data.Database.DocumentUnavailableException;
import ai.API.OpenAI.ModerationEndpoint;
import ai.API.OpenAI.ModerationEndpoint.ModerationResult;
import ai.Data.ServerSettings;

public class AiMod {

    public static void moderateMessage(Message message) {
        if (!message.isServerMessage()) return; // checks if message sent in a server
        if (!message.getUserAuthor().isPresent()) return; // checks if not a webhook

        // attempt to get server settings or return
        Server server = message.getServer().get();
        ServerSettings serverSettings;
        try {
            serverSettings = new ServerSettings(server.getId());
        } catch (DocumentUnavailableException e) {
            return;
        }

        // if aimod is enabled check message
        if (!serverSettings.isAiModEnabled()) return;
        ModerationEndpoint.moderateText(message.getContent()).thenAcceptAsync(modResult -> {
            System.out.println("message scanned");
            if (!modResult.flagged) return;

            // log event if applicable
            User author = message.getUserAuthor().get();
            if (serverSettings.isModLogEnabled() && canLogMessage(serverSettings, server)) {
                logMessage(author, message, modResult, server.getTextChannelById(serverSettings.getLogChannelID().get()).get());
            }

            // // warn/mute user if applicable
            // warnUser(author, server);

        });
    }

    // // <serverID, <userID, warnings>>
    // private static HashMap<Long, HashMap<Long, Integer>> serverWarnings = new HashMap<Long, HashMap<Long, Integer>>();

    // private static void warnUser(User user, Server server) {
    //     HashMap<Long, Integer> userWarnings = serverWarnings.getOrDefault(server.getId(), new HashMap<Long, Integer>());
    //     userWarnings.put(user.getId(), userWarnings.getOrDefault(user.getId()+1, 1));
    // }

    private static boolean canLogMessage(ServerSettings serverSettings, Server server) {
        if (!serverSettings.getLogChannelID().isPresent()) return false;
        long logChannelID = serverSettings.getLogChannelID().get();
        if (!server.getTextChannelById(logChannelID).isPresent()) return false;
        return true;
    }

    private static void logMessage(User user, Message message, ModerationResult modResult, ServerTextChannel modlogChannel) {
        modlogChannel.sendMessage(LogEmbed.aiModEmbed(user, message, modResult));
    }

}
