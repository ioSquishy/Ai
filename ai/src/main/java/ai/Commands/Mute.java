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

import ai.App;
import ai.Constants.TaskSchedulerKeyPrefixs;
import ai.Data.ServerSettings;
import ai.Data.Database.DocumentUnavailableException;
import ai.Utility.LogEmbed;
import ai.Utility.ReadableTime;
import ai.Utility.TaskScheduler;
import ai.Utility.LogEmbed.EmbedType;
import ai.Utility.Runnables.EmbedDelay;
import ai.Utility.Runnables.RoleDelay;


public class Mute {

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

    public SlashCommandBuilder unmuteCommand() {
        return new SlashCommandBuilder()
            .setName("unmute")
            .setDescription("Unmute someone.")
            .setDefaultEnabledForPermissions(PermissionType.MANAGE_MESSAGES)
            .addOption(new SlashCommandOptionBuilder()
                .setName("User")
                .setType(SlashCommandOptionType.USER)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("Reason")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(false)
                .build());
    }

    public static void handleMuteCommand(SlashCommandInteraction interaction) {
        // get server settings
        ServerSettings settings;
        try {
            settings = new ServerSettings(interaction.getServer().get());
        } catch (DocumentUnavailableException e) {
            DocumentUnavailableException.sendStandardResponse(interaction);
            return;
        }

        // calculate duration of mute in seconds
        final long duration = (interaction.getArgumentLongValueByName("days").orElse((long) 0)*86400) + (interaction.getArgumentLongValueByName("hours").orElse((long) 0)*3600) + (interaction.getArgumentLongValueByName("minutes").orElse((long) 0)*60) + interaction.getArgumentLongValueByName("seconds").orElse((long) 0);

        Role muteRole = null;
        // ensure valid arguments
        try {
            hasValidArguments(settings);
            // save mute role id and log channel id
            muteRole = settings.getMuteRole().orElseThrow();
        } catch (NoSuchElementException e) {
            // if mute role doesnt exist
            String errorResponse = "You do not have a valid mute role set.";
            interaction.createImmediateResponder().setContent(errorResponse).respond();
            return;
        } catch (MuteException e) {
            e.sendExceptionResponse(interaction);
            return;
        }

        final User moderator = interaction.getUser();
        final User mutedUser = interaction.getArgumentUserValueByName("User").get();
        final Server server = interaction.getServer().get();
        final String reason = interaction.getArgumentStringValueByName("Reason").orElse("");

        // check permissions
        try {
            hasPermission(moderator, mutedUser, muteRole, server);
        } catch (MuteException e) {
            e.sendExceptionResponse(interaction);
            return;
        }
        
        // mute user
        muteUser(mutedUser, muteRole, duration, server, reason);
        sendMuteResponse(interaction, mutedUser, duration);
        logMuteEvent(mutedUser, moderator, duration, reason, settings);
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

    /**
     * Checks if user is in the server.
     * Checks if server settings has a mute role.
     * Checks if the mute role is a valid role.
     * 
     * @param interaction
     * @param settings
     * @return Returns false if any fail.
     * @throws DocumentUnavailableException If database is unaccessible and server is not in cache.
     */
    private static boolean hasValidArguments(ServerSettings settings) throws MuteException {
        //check if there is a muterole
        if (settings.getMuteRole().isEmpty()) { 
            throw new MuteException("You do not have a mute role set!");
        }
        // check if muterole is valid
        Role muteRole = settings.getMuteRole().orElse(null);
        if (muteRole == null) {
            throw new MuteException("Invalid mute role!");
        }
        // check if bot can manage the muterole
        if (!App.api.getYourself().canManageRole(muteRole)) {
            throw new MuteException("I cannot manage that role!");
        }
        return true;
    }

    private static boolean hasPermission(User moderator, User mutedUser, Role muteRole, Server server) throws MuteException {
        if (!server.canManageRole(moderator, muteRole)) {
            throw new MuteException(moderator.getMentionTag() + " cannot manage the role: " + muteRole.getMentionTag());
        }
        if (!server.canTimeoutUser(moderator, mutedUser)) {
            throw new MuteException(moderator.getMentionTag() + " cannot timeout the user: " + mutedUser.getMentionTag());
        }
        if (!server.canYouTimeoutUser(mutedUser)) {
            throw new MuteException(App.api.getYourself().getMentionTag() + " cannot timeout the user: " + mutedUser.getMentionTag());
        }
        return true;
    }
    private static class MuteException extends Exception {
        final String exceptionReason;
        public MuteException(String reason) {
            super(reason);
            exceptionReason = reason;
        }

        public void sendExceptionResponse(InteractionBase interaction) {
            interaction.createImmediateResponder().setContent(exceptionReason).setFlags(MessageFlag.SUPPRESS_NOTIFICATIONS, MessageFlag.EPHEMERAL).respond();
        }
    }

    public static void handleUnmuteCommand(SlashCommandInteraction interaction) {
        try {
            ServerSettings settings = new ServerSettings(interaction.getServer().get());
            // get variables
            User targetUser;
            Role muteRole;
            final User moderator = interaction.getUser();
            final String reason = interaction.getArgumentStringValueByName("reason").orElse("");
            try {
                targetUser = interaction.getArgumentUserValueByName("User").get();
                muteRole = settings.getMuteRole().orElseThrow();
            } catch (NoSuchElementException e) { // thrown if bad muterole
                interaction.createImmediateResponder().setContent("You do not have a valid mute role set!").respond();
                return;
            }

            // unmute
            unmuteUser(targetUser, moderator, reason, muteRole);
            
            // log if needede
            logUnmute(targetUser, moderator, reason, settings);

            // respond
            interaction.createImmediateResponder().setContent(targetUser.getName() + " was unmuted.").setFlags(MessageFlag.EPHEMERAL).respond();
        } catch (DocumentUnavailableException e) {
            DocumentUnavailableException.sendStandardResponse(interaction);
        }
    }

    /**
     * Unmutes user then logs event if applicable.
     * @param offender
     * @param moderator
     * @param reason
     * @param serverSettings
     * @throws DocumentUnavailableException
     */
    private static void unmuteUser(User offender, User moderator, String reason, Role muteRole) throws DocumentUnavailableException {
        muteRole.getServer().createUpdater()
            .removeRoleFromUser(offender, muteRole)
            .removeUserTimeout(offender)
            .update();
    }

    private static void logUnmute(User offender, User moderator, String reason, ServerSettings serverSettings) {
        if (serverSettings.isModLogEnabled() && serverSettings.isLogMuteEnabled()) {
            serverSettings.getModLogChannel().ifPresent(channel -> {
                channel.sendMessage(LogEmbed.getEmbed(EmbedType.Unmute, offender, moderator, reason));
            });
        }
    }
    
}
