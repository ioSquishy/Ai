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
            if (!isFlaggedForServer(modResult, serverSettings)) return;

            // log event if applicable
            User author = message.getUserAuthor().get();
            if (serverSettings.isModLogEnabled() && canLogMessage(serverSettings, server)) {
                logMessage(author, message, modResult, server.getTextChannelById(serverSettings.getModLogChannelID().get()).get());
            }

            // // warn/mute user if applicable
            // warnUser(author, server);

        });
    }

    public static boolean isFlaggedForServer(ModerationResult modResult, ServerSettings serverSettings) {
        if (!modResult.flagged) return false;
        if (modResult.flags.hate && serverSettings.flagHate()) return true;
        if (modResult.flags.harassment && serverSettings.flagHarrassment()) return true;
        if (modResult.flags.selfHarm && serverSettings.flagSelfHarm()) return true;
        if (modResult.flags.sexual && serverSettings.flagSexual()) return true;
        if (modResult.flags.violence && serverSettings.flagViolence()) return true;
        return false; 
    }

    // // <serverID, <userID, warnings>>
    // private static HashMap<Long, HashMap<Long, Integer>> serverWarnings = new HashMap<Long, HashMap<Long, Integer>>();

    // private static void warnUser(User user, Server server) {
    //     HashMap<Long, Integer> userWarnings = serverWarnings.getOrDefault(server.getId(), new HashMap<Long, Integer>());
    //     userWarnings.put(user.getId(), userWarnings.getOrDefault(user.getId()+1, 1));
    // }

    private static boolean canLogMessage(ServerSettings serverSettings, Server server) {
        if (!serverSettings.getModLogChannelID().isPresent()) return false;
        long logChannelID = serverSettings.getModLogChannelID().get();
        if (!server.getTextChannelById(logChannelID).isPresent()) return false;
        return true;
    }

    private static void logMessage(User user, Message message, ModerationResult modResult, ServerTextChannel modlogChannel) {
        modlogChannel.sendMessage(LogEmbed.aiModEmbed(user, message, modResult));
    }

}
