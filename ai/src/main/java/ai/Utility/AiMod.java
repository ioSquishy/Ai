package ai.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.net.MalformedURLException;
import java.net.URL;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
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

        // check if aimod is enabled
        if (!serverSettings.isAiModEnabled()) return;

        // if message has attachments and checking images is enable
        if (serverSettings.isAiModImageCheckEnabled() && !message.getAttachments().isEmpty()) {
            // ModerationEndpoint.moderateTextAndImages(message.getContent(), getAttachmentURLs(message.getAttachments())).thenAcceptAsync(modResult -> {
            //     if (!isFlaggedForServer(modResult, serverSettings) || isIgnoredChannel(serverSettings, message.getChannel().getId())) return;
    
            //     // log event if applicable
            //     User author = message.getUserAuthor().get();
            //     if (serverSettings.isAiModEnabled() && serverSettings.getAiLogChannel().isPresent()) {
            //         logMessage(author, message, modResult, serverSettings.getAiLogChannel().get());
            //     }
            // });
            // moderate text and attachment together
            moderateMessageWithAttachments(message, serverSettings);
        } else {
            moderateMessageText(message, serverSettings);
        }
    }

    private static void moderateMessageWithAttachments(Message message, ServerSettings serverSettings) {
        String[] imageURLs = getAttachmentURLs(message.getAttachments());
        if (imageURLs.length == 1) {
            ModerationEndpoint.moderateTextAndImage(message.getContent(), imageURLs[0]).thenAcceptAsync(modResult -> {
                if (!isFlaggedForServer(modResult, serverSettings) || isIgnoredChannel(serverSettings, message.getChannel().getId())) return;
    
                // log event if applicable
                User author = message.getUserAuthor().get();
                if (serverSettings.isAiModEnabled() && serverSettings.getAiLogChannel().isPresent()) {
                    logMessage(author, message, imageURLs, modResult, serverSettings.getAiLogChannel().get());
                }
            });
        } else {
            List<CompletableFuture<ModerationResult>> cfs = new ArrayList<CompletableFuture<ModerationResult>>();
            cfs.add(ModerationEndpoint.moderateText(message.getContent()));
            for (String imageURL : getAttachmentURLs(message.getAttachments())) {
                cfs.add(ModerationEndpoint.moderateTextAndImage(null, imageURL));
            }
            CompletableFuture.runAsync(() -> {
                CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0])).thenRun(() -> {
                    ModerationResult mergedModResult = ModerationResult.mergeResults(cfs.stream().map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).filter(result -> result != null).toArray(ModerationResult[]::new));
                    
                    if (!isFlaggedForServer(mergedModResult, serverSettings) || isIgnoredChannel(serverSettings, message.getChannel().getId())) return;
    
                    // log event if applicable
                    User author = message.getUserAuthor().get();
                    if (serverSettings.isAiModEnabled() && serverSettings.getAiLogChannel().isPresent()) {
                        logMessage(author, message, imageURLs, mergedModResult, serverSettings.getAiLogChannel().get());
                    }
                });
            });
        }
    }

    private static void moderateMessageText(Message message, ServerSettings serverSettings) {
        ModerationEndpoint.moderateText(message.getContent()).thenAcceptAsync(modResult -> {
            if (!isFlaggedForServer(modResult, serverSettings) || isIgnoredChannel(serverSettings, message.getChannel().getId())) return;

            // log event if applicable
            User author = message.getUserAuthor().get();
            if (serverSettings.isAiModEnabled() && serverSettings.getAiLogChannel().isPresent()) {
                logMessage(author, message, null, modResult, serverSettings.getAiLogChannel().get());
            }
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

    /**
     * Filters attachments for just images and returns their URLs as an array of Strings
     * @param messageAttachments
     * @return array of attachment urls that are IMAGES
     */
    private static String[] getAttachmentURLs(List<MessageAttachment> messageAttachments) {
        return messageAttachments.stream().filter(attachment -> attachment.isImage()).map(attachment -> attachment.getUrl().toString()).toArray(String[]::new);
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

    private static void logMessage(User user, Message message, String[] attachmentURLs, ModerationResult modResult, ServerTextChannel aiLogChannel) {
        aiLogChannel.sendMessage(LogEmbed.aiModEmbed(user, message.getLink().toString(), Optional.ofNullable(attachmentURLs), modResult));
    }

}
