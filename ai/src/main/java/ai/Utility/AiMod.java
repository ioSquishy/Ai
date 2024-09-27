package ai.Utility;

import java.util.List;

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
            if (serverSettings.isAiModEnabled() && canLogMessage(serverSettings, server)) {
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
        if (modResult.flags.illicit && serverSettings.flagIllicit()) return true;
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

    private static boolean canLogMessage(ServerSettings serverSettings, Server server) {
        if (!serverSettings.getAiLogChannel().isPresent()) return false;
        if (!serverSettings.getAiLogChannel().isPresent()) return false;
        return true;
    }

    private static void logMessage(User user, Message message, ModerationResult modResult, ServerTextChannel aiLogChannel) {
        aiLogChannel.sendMessage(LogEmbed.aiModEmbed(user, message, modResult));
    }

}
