package ai.Commands;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;

import ai.ServerSettings;

public class ModLog {

    public static void handleUpdateCmd(SlashCommandInteraction interaction, ServerSettings settings) {
        settings.updateModLogSettings(
            interaction.getArgumentBooleanValueByName("enabled").get(), 
            interaction.getArgumentChannelValueByName("channel").orElse(null), 
            interaction.getArgumentBooleanValueByName("bans").orElse(false), 
            interaction.getArgumentBooleanValueByName("mutes").orElse(false), 
            interaction.getArgumentBooleanValueByName("kicks").orElse(false));
        interaction.createImmediateResponder().setContent("Mod log settings updated.").setFlags(MessageFlag.EPHEMERAL).respond().join();
    }

    public static SlashCommandBuilder modLogCommand() {
        return new SlashCommandBuilder()
            .setName("modlog")
            .setDescription("Edit modlog settings.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName("enabled")
                .setDescription("Toggle modlog.")
                .setRequired(true)
                .setType(SlashCommandOptionType.BOOLEAN)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("channel")
                .setDescription("Channel to send log messages to.")
                .setRequired(false)
                .setType(SlashCommandOptionType.CHANNEL)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("bans")
                .setDescription("Log bans?")
                .setRequired(false)
                .setType(SlashCommandOptionType.BOOLEAN)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("mutes")
                .setDescription("Log mutes and timeouts?")
                .setRequired(false)
                .setType(SlashCommandOptionType.BOOLEAN)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("kicks")
                .setDescription("Log kicks?")
                .setRequired(false)
                .setType(SlashCommandOptionType.BOOLEAN)
                .build());
    }
    
}
