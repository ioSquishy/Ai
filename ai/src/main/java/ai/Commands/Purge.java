package ai.Commands;

import java.util.Optional;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;

import ai.App;

public class Purge {
    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("purge")
            .setDescription("Bulk delete messages until a specific one.")
            .setDefaultEnabledForPermissions(PermissionType.MANAGE_MESSAGES)
            .addOption(new SlashCommandOptionBuilder()
                .setName("from")
                .setDescription("Delete messages from this message ID.")
                .setRequired(false)
                .setType(SlashCommandOptionType.LONG)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("until")
                .setDescription("Delete messages until this message ID is reached.")
                .setRequired(true)
                .setType(SlashCommandOptionType.LONG)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("author")
                .setDescription("Only delete messages from this user.")
                .setRequired(false)
                .setType(SlashCommandOptionType.USER)
                .build());
    }

    public static void handleCommand(SlashCommandInteraction event) {
        TextChannel channel = event.getChannel().orElseThrow();
        // channel.getMessagesAfterUntil(, null)
    }

    private static void getMessages(Optional<Long> from, long until, Optional<User> author) {

    }

    private static void purge() {

    }
}
