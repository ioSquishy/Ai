package ai.Commands;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.tinylog.Logger;

import ai.App;
import ai.Constants.TaskSchedulerKeyPrefixs;
import ai.Data.ServerSettings;
import ai.Data.Database.DocumentUnavailableException;
import ai.Utility.InteractionException;
import ai.Utility.LogEmbed;
import ai.Utility.PermissionsCheck;
import ai.Utility.ReadableTime;
import ai.Utility.TaskScheduler;
import ai.Utility.LogEmbed.EmbedType;
import ai.Utility.Runnables.EmbedDelay;
import ai.Utility.Runnables.RoleDelay;


public class Mute {

    public static SlashCommandBuilder createCommand() {
        return new SlashCommandBuilder()
            .setName("mute")
            .setDescription("Mute someone permanently or for a custom duration.")
            .setDefaultEnabledForPermissions(PermissionType.MODERATE_MEMBERS)
            .setEnabledInDms(false)
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

    public static void handleMuteCommand(SlashCommandInteraction interaction) {
        // get server settings
        ServerSettings settings;
        try {
            settings = new ServerSettings(interaction.getServer().get());
        } catch (DocumentUnavailableException e) {
            Logger.tag("ai").debug(e);
            DocumentUnavailableException.sendStandardResponse(interaction);
            return;
        }

        // calculate duration of mute in seconds
        final long duration = (interaction.getArgumentLongValueByName("days").orElse((long) 0)*86400) + (interaction.getArgumentLongValueByName("hours").orElse((long) 0)*3600) + (interaction.getArgumentLongValueByName("minutes").orElse((long) 0)*60) + interaction.getArgumentLongValueByName("seconds").orElse((long) 0);
        // other variables
        final User moderator = interaction.getUser();
        final User mutedUser = interaction.getArgumentUserValueByName("User").get();
        final Server server = interaction.getServer().get();
        final String reason = interaction.getArgumentStringValueByName("Reason").orElse("");

        // ensure valid arguments
        Role muteRole = null;
        try {
            // save mute role id and log channel id
            muteRole = settings.getMuteRole().orElseThrow();
            
            checkPermissions(server, muteRole, moderator, mutedUser);
        } catch (NoSuchElementException e) {
            Logger.tag("ai").debug(e);
            // if mute role doesnt exist
            String errorResponse = "You do not have a valid mute role set.";
            interaction.createImmediateResponder().setContent(errorResponse).respond();
            return;
        } catch (InteractionException e) {
            Logger.tag("ai").debug(e);
            e.sendExceptionResponse(interaction);
            return;
        }
        
        // mute user
        muteUser(mutedUser, muteRole, duration, server, reason);
        sendMuteResponse(interaction, mutedUser, duration);
        logMuteEvent(mutedUser, moderator, duration, reason, settings);
    }

    private static void checkPermissions(Server server, Role muteRole, User moderator, User mutedUser) throws InteractionException {
        if (!PermissionsCheck.canManageRole(server, muteRole)) {
            throw new InteractionException("Bot cannot manage the mute role!");
        }
        if (!PermissionsCheck.canTimeoutUser(server, mutedUser)) {
            throw new InteractionException("Bot cannot timeout that user!");
        }
        if (!PermissionsCheck.userIsAboveUser(server, moderator, mutedUser)) {
            throw new InteractionException("You cannot mute that user!");
        }
    }

    private static void muteUser(User mutedUser, Role muteRole, long duration, Server server, String reason) {
        final Duration secondDuration = Duration.ofSeconds(duration);
        if (duration > 0) {
            //if duration is less than 28 days, use discord timeout
            if (duration <= 28*86400) {
                if (!reason.isBlank()) {
                    mutedUser.timeout(server, secondDuration, reason);
                } else {
                    mutedUser.timeout(server, secondDuration);
                }
            }
            //also mutes normally anyways with roledelay tempmute method
            server.addRoleToUser(mutedUser, muteRole).join();
            TaskScheduler.scheduleTask(
                TaskSchedulerKeyPrefixs.TEMP_MUTE+mutedUser.getId(),
                new RoleDelay(muteRole.getId(), server.getId(), mutedUser.getId()).removeRoleRunnable(),
                duration, TimeUnit.SECONDS);
        } else { //use mute role to perm mute if no duration set
            server.addRoleToUser(mutedUser, muteRole).join();
        }
    }

    private static void logMuteEvent(User mutedUser, User moderator, long duration, String reason, ServerSettings settings) {
        final Optional<ServerTextChannel> logChannel = settings.getModLogChannel();

        if (settings.isModLogEnabled() && settings.isLogMuteEnabled()) {
            logChannel.ifPresent(channel -> {
                channel.sendMessage(LogEmbed.getEmbed(EmbedType.Mute, mutedUser, moderator, new ReadableTime().compute(duration), reason));

                // auto unmute message
                EmbedBuilder unmuteEmbed = LogEmbed.getEmbed(
                    EmbedType.Unmute,
                    mutedUser,
                    App.api.getYourself(), 
                    "Temporary mute duration over.");
                TaskScheduler.scheduleTask(
                    TaskSchedulerKeyPrefixs.TEMP_MUTE_EMBED+mutedUser.getId(), 
                    new EmbedDelay(unmuteEmbed, channel.getId(), settings.getServerId()).sendEmbedRunnable(),
                    duration, TimeUnit.SECONDS);
            });
        }
    }

    private static void sendMuteResponse(InteractionBase interaction, User mutedUser, long duration) {
        String response = mutedUser.getMentionTag() + " was muted";
        if (duration > 0) response += " for" + new ReadableTime().compute(duration);
        interaction.createImmediateResponder().setContent(response + ".").setFlags(MessageFlag.EPHEMERAL).respond();
    }
    
}
