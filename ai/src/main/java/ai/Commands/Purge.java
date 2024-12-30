package ai.Commands;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.tinylog.Logger;

import ai.Utility.InteractionException;
import ai.Utility.PermissionsCheck;

public class Purge {
    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("purge")
            .setDescription("Bulk delete messages until a specific one.")
            .setDefaultEnabledForPermissions(PermissionType.MANAGE_MESSAGES)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName("until")
                .setDescription("Delete messages bellow this message ID. (inclusive)")
                .setRequired(true)
                .setType(SlashCommandOptionType.STRING)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("from")
                .setDescription("Delete messages above this message ID. (inclusive)")
                .setRequired(false)
                .setType(SlashCommandOptionType.STRING)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("author")
                .setDescription("Only delete messages from this user/bot.")
                .setRequired(false)
                .setType(SlashCommandOptionType.USER)
                .build());
    }

    public static void handleCommand(SlashCommandInteraction event) {
        TextChannel channel = event.getChannel().orElseThrow();

        if (!PermissionsCheck.canDeleteMessages(channel)) {
            event.createImmediateResponder().setContent("I cannot delete messages.").respond();
            return;
        }

        long fromMsgID;
        long untilMsgID;
        try {
            String fromValue = event.getArgumentStringValueByName("from").orElse(channel.getMessages(1).join().first().getId() + "");
            fromMsgID = Long.parseLong(fromValue);
            untilMsgID = Long.parseLong(event.getArgumentStringValueByName("until").orElseThrow());
        } catch (NumberFormatException e) {
            new InteractionException("Not a valid input for from/until options. Enter the ID of the messages.").sendExceptionResponse(event);
            return;
        }

        event.respondLater().thenAccept(responseUpdater -> {
            try {
                Message fromMsg = getMessage(fromMsgID, event);
                Message untilMsg = getMessage(untilMsgID, event);
                
                channel.getMessagesBetween(fromMsg, untilMsg).thenAccept(messages -> {
                    // add messages to a mutable list
                    ArrayList<Message> mutableMessages = new ArrayList<Message>(messages);
                    mutableMessages.add(fromMsg);
                    mutableMessages.add(untilMsg);
                    // filter message set to only include messages from specified author if applicable
                    if (event.getArgumentUserValueByName("author").isPresent()) {
                        long authorIdToKeep = event.getArgumentUserValueByName("author").get().getId();
                        try {
                            mutableMessages.removeIf(message -> {
                                return message.getAuthor().getId() != authorIdToKeep;
                            });
                        } catch (Exception e) {
                            Logger.error(e);
                        }
                    }
                    // delete messages and respond
                    channel.deleteMessages(mutableMessages).thenRun(() -> {
                        responseUpdater.setContent(mutableMessages.size() + " messages purged.").setFlags(MessageFlag.EPHEMERAL).update();
                    });
                });
            } catch (InteractionException e) {
                Logger.error(e);
                responseUpdater.setContent(e.getExceptionMessage()).setFlags(MessageFlag.EPHEMERAL).update();
            } catch (Exception e) {
                Logger.error(e);
                responseUpdater.setContent("Failed.").setFlags(MessageFlag.EPHEMERAL).update();
            }
        });
    }

    private static Message getMessage(long messageID, SlashCommandInteraction interaction) throws InteractionException {
        try {
            return interaction.getChannel().get().getMessageById(messageID).get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new InteractionException("Could not find a message with id `" + messageID + "`.");
        }
    }
}
