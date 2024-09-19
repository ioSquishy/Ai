package ai.Utility;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import ai.Data.Database.DocumentUnavailableException;
import ai.Constants.TaskSchedulerKeyPrefixs;
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
            serverSettings = new ServerSettings(server);
        } catch (DocumentUnavailableException e) {
            return;
        }

        // if aimod is enabled check message
        if (!serverSettings.isAiModEnabled()) return;
        ModerationEndpoint.moderateText(message.getContent()).thenAcceptAsync(modResult -> {
            if (!isFlaggedForServer(modResult, serverSettings) || isIgnoredChannel(serverSettings, message.getChannel().getId())) return;

            // log event if applicable
            User author = message.getUserAuthor().get();
            if (serverSettings.isAiModEnabled() && serverSettings.getAiLogChannel().isPresent()) {
                logMessage(author, message, modResult, serverSettings.getAiLogChannel().get());
            }

            // // warn/mute user if applicable
            // warnUser(author, server);
        });
    }

    private static boolean isFlaggedForServer(ModerationResult modResult, ServerSettings serverSettings) {
        if (!modResult.flagged) return false;
        if (modResult.flags.hate && serverSettings.flagHate()) return true;
        if (modResult.flags.harassment && serverSettings.flagHarrassment()) return true;
        if (modResult.flags.selfHarm && serverSettings.flagSelfHarm()) return true;
        if (modResult.flags.sexual && serverSettings.flagSexual()) return true;
        if (modResult.flags.violence && serverSettings.flagViolence()) return true;
        return false; 
    }

    private static boolean isIgnoredChannel(ServerSettings serverSettings, long messageChannelID) {
        List<Long> ignoredChannelIDs = serverSettings.getAiIgnoredChannels();
        if (ignoredChannelIDs != null) {
            for (Long ignoredID : ignoredChannelIDs) {
                if (ignoredID == messageChannelID) return true;
            }
        }
        return false;
    }

    private static void logMessage(User user, Message message, ModerationResult modResult, ServerTextChannel aiLogChannel) {
        aiLogChannel.sendMessage(LogEmbed.aiModEmbed(user, message, modResult));
    }


    // <serverID, <userID, warnings>>
    // private static HashMap<Long, HashMap<Long, Integer>> serverWarnings = new HashMap<Long, HashMap<Long, Integer>>();

    // private static void warnUser(User user, Server server) {
    //     HashMap<Long, Integer> userWarnings = serverWarnings.getOrDefault(server.getId(), new HashMap<Long, Integer>(Map.of(user.getId(), 0)));
    //     userWarnings.put(user.getId(), userWarnings.get(user.getId())+1);
    //     TaskScheduler.scheduleTask(TaskSchedulerKeyPrefixs.AI_WARN_REMOVE+user.getId()+Instant.now().toEpochMilli(), removeWarning(server.getId(), user.getId()), 2, TimeUnit.SECONDS);
    //     if (userWarnings.get(user.getId()) >= 3) {
    //         muteUser(user);
    //     }
    // }

    // private static Runnable removeWarning(long serverID, long userID) {
    //     return () -> {
    //         HashMap<Long, Integer> userWarnings = serverWarnings.get(serverID);
    //         userWarnings.put(userID, userWarnings.get(userID)-1);
    //     };
    // }

    // private static void muteUser(User user) {
    //     //TODO
    // }
}
