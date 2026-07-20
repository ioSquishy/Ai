package ai.Commands;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;

import ai.Utility.LogEmbed;
import ai.Utility.PermissionsCheck;

public class Unban {

    public static SlashCommandBuilder createCommand() {
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
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("reason")
                .setDescription("Reason for unbanning.")
                .setRequired(false)
                .setType(SlashCommandOptionType.STRING)
                .build());
    }

    public static void handleCommand(SlashCommandInteraction interaction) {
        Server server = interaction.getServer().get();
        if (!PermissionsCheck.canBanUsers(server)) {
            interaction.createImmediateResponder().setContent("I cannot unban users.").respond();
            return;
        }

        User bannedUser = interaction.getArgumentUserValueByName("user").get();
        String reason = interaction.getArgumentStringValueByName("reason").orElse("") + LogEmbed.getModeratorAppendage(interaction);
        server.unbanUser(bannedUser, reason);
        interaction.createImmediateResponder().setContent(bannedUser.getMentionTag() + " was unbanned.").setFlags(MessageFlag.EPHEMERAL).respond();
    }
}
