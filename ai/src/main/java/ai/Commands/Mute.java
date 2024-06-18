package ai.Commands;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;

import ai.App;
import ai.ServerSettings;
import ai.Utility.LogEmbeds;
import ai.Utility.ReadableTime;
import ai.Utility.RoleDelay;


public class Mute {

    public static SlashCommandBuilder setMuteRoleCommand() {
        return new SlashCommandBuilder()
        .setName("setmuterole")
        .setDescription("Set the mute role.")
        .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
        .setEnabledInDms(false)
        .addOption(new SlashCommandOptionBuilder()
            .setName("role")
            .setDescription("Role to set as mute role.")
            .setRequired(true)
            .setType(SlashCommandOptionType.ROLE)
            .build());
    }

    public static SlashCommandBuilder muteSlashCommand() {
        return new SlashCommandBuilder()
            .setName("mute")
            .setDescription("Mute someone permanently or for a custom duration.")
            .setDefaultEnabledForPermissions(PermissionType.MANAGE_MESSAGES)
            .addOption(new SlashCommandOptionBuilder()
                .setName("User")
                .setDescription("User to mute.")
                .setRequired(true)
                .setType(SlashCommandOptionType.USER)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("Days")
                .setDescription("Optional number of days to mute.")
                .setRequired(false)
                .setType(SlashCommandOptionType.LONG)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("Hours")
                .setDescription("Optional number of hours to mute.")
                .setRequired(false)
                .setType(SlashCommandOptionType.LONG)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("Minutes")
                .setDescription("Optional number of minutes to mute.")
                .setRequired(false)
                .setType(SlashCommandOptionType.LONG)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("Seconds")
                .setDescription("Optional number of seconds to mute.")
                .setRequired(false)
                .setType(SlashCommandOptionType.LONG)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("Reason")
                .setDescription("Optional Reason")
                .setRequired(false)
                .setType(SlashCommandOptionType.STRING)
                .build());
    }

    public SlashCommandBuilder unmuteCmd() {
        return new SlashCommandBuilder()
            .setName("unmute")
            .setDescription("Unmute someone.")
            .setDefaultEnabledForPermissions(PermissionType.MANAGE_MESSAGES)
            .addOption(new SlashCommandOptionBuilder()
                .setName("User")
                .setType(SlashCommandOptionType.USER)
                .setRequired(true)
                .build());
    }

    public static void handleMuteCommand(SlashCommandInteraction interaction, ServerSettings settings) {
        if (!interaction.getArgumentUserValueByName("user").isPresent()) { //check if user is in server
            interaction.createImmediateResponder().setContent("That user is not in the server!").setFlags(MessageFlag.EPHEMERAL).respond().join();
            return;
        }
        if (settings.getMuteRoleID() == null) { //check if there is a muterole
            interaction.createImmediateResponder().setContent("You do not have a mute role set!").setFlags(MessageFlag.EPHEMERAL).respond().join();
            return;
        }
        //duration of mute in seconds
        long duration = (interaction.getArgumentLongValueByName("days").orElse((long) 0)*86400) + (interaction.getArgumentLongValueByName("hours").orElse((long) 0)*3600) + (interaction.getArgumentLongValueByName("minutes").orElse((long) 0)*60) + interaction.getArgumentLongValueByName("seconds").orElse((long) 0);
        if (duration > Integer.MAX_VALUE) duration = Integer.MAX_VALUE;
        if (duration > 0) {
            //if duration is less than 28 days, use discord timeout
            if (Duration.ofSeconds(duration).getSeconds() <= 28*86400) {
                if (interaction.getArgumentStringValueByName("Reason").isPresent()) {
                    interaction.getArgumentUserValueByName("User").get().timeout(interaction.getServer().get(), Duration.ofSeconds(duration), interaction.getArgumentStringValueByName("Reason").get());
                } else {
                    interaction.getArgumentUserValueByName("User").get().timeout(interaction.getServer().get(), Duration.ofSeconds(duration));
                }
            }
            //also mutes normally anyways to use roledelay tempmute method lol
            interaction.getServer().get().addRoleToUser(interaction.getArgumentUserValueByName("User").get(), App.api.getRoleById(settings.getMuteRoleID()).get()).join();
            new RoleDelay(settings.getMuteRoleID(), interaction.getArgumentUserValueByName("User").get(), interaction.getServer().get(), settings.getLogChannelID()).tempMute(duration, TimeUnit.SECONDS);
        } else { //use mute role to perm mute
            interaction.getServer().get().addRoleToUser(interaction.getArgumentUserValueByName("User").get(), App.api.getRoleById(settings.getMuteRoleID()).get()).join();
        }
        //respond
        String response = interaction.getArgumentUserValueByName("user").get().getMentionTag() + " was muted";
        if (duration > 0) response += " for" + new ReadableTime().compute(duration);
        interaction.createImmediateResponder().setContent(response + ".").setFlags(MessageFlag.EPHEMERAL).respond().join();
        //send log message
        if (settings.isLogEnabled() && settings.isLogMuteEnabled()) {
            App.api.getChannelById(settings.getLogChannelID()).get().asTextChannel().get().sendMessage(new LogEmbeds().mute(interaction.getArgumentUserValueByName("User").get(), interaction.getUser(), new ReadableTime().compute(duration), interaction.getArgumentStringValueByName("Reason").orElse("")));
        }
    }

    public static void handleUnmuteCommand(SlashCommandInteraction interaction, ServerSettings settings) {
        interaction.getServer().get().createUpdater()
            .removeRoleFromUser(interaction.getArgumentUserValueByName("User").get(), App.api.getRoleById(settings.getMuteRoleID()).get())
            .removeUserTimeout(interaction.getArgumentUserValueByName("User").get())
            .update().join();
        interaction.createImmediateResponder().setContent(interaction.getArgumentUserValueByName("User").get().getName() + " was unmuted.").setFlags(MessageFlag.EPHEMERAL).respond().join();
        if (settings.isLogEnabled() && settings.isLogMuteEnabled() && settings.getLogChannelID() != null) {
            App.api.getChannelById(settings.getLogChannelID()).get().asServerTextChannel().get().sendMessage(new LogEmbeds().unmute(interaction.getArgumentUserValueByName("User").get(), interaction.getUser())).join();
        }
    }
    
}
