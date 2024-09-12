package ai.Commands;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;

public class Unban {

    public static SlashCommandBuilder unbanSlashCommand() {
        return new SlashCommandBuilder()
            .setName("unban")
            .setDescription("Unban someone from the server.")
            .setDefaultEnabledForPermissions(PermissionType.BAN_MEMBERS)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName("user")
                .setDescription("User to unban. Can be user ID.")
                .setRequired(true)
                .setType(SlashCommandOptionType.USER)
                .build());
    }

    public static void handleCommand(SlashCommandInteraction interaction) {
        interaction.getServer().get().unbanUser(interaction.getArgumentUserValueByName("user").get());
        interaction.createImmediateResponder().setContent(interaction.getArgumentUserValueByName("user").get().getMentionTag() + " was unbanned.").setFlags(MessageFlag.EPHEMERAL).respond().join();
    }
}
