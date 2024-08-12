package ai.Commands;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

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
import ai.Database.DocumentUnavailableException;
import ai.ServerSettings;
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

    public static void handleMuteCommand(SlashCommandInteraction interaction, ServerSettings settings) {
        // calculate duration of mute in seconds
        final long duration = (interaction.getArgumentLongValueByName("days").orElse((long) 0)*86400) + (interaction.getArgumentLongValueByName("hours").orElse((long) 0)*3600) + (interaction.getArgumentLongValueByName("minutes").orElse((long) 0)*60) + interaction.getArgumentLongValueByName("seconds").orElse((long) 0);

        long muteRoleID = -1;
        Long initLogChannel = null; // null if not logging
        // ensure valid arguments
        try {
            if (!hasValidArguments(interaction, settings)) {
                return;
            }
            // save mute role id and log channel id
            muteRoleID = settings.getMuteRoleID().orElseThrow();
            initLogChannel = settings.isLogMuteEnabled() ? settings.getLogChannelID().orElse(null) : null;
        } catch (NoSuchElementException e) {
            // if mute role doesnt exist
            String errorResponse = "You do not have a mute role set.";
            interaction.createImmediateResponder().setContent(errorResponse).respond();
            return;
        } catch (DocumentUnavailableException e) { 
            DocumentUnavailableException.sendStandardResponse(interaction);
            return;
        }

        final User moderator = interaction.getUser();
        final User mutedUser = interaction.getArgumentUserValueByName("User").get();
        final Server server = interaction.getServer().get();
        final String reason = interaction.getArgumentStringValueByName("Reason").orElse("");
        final Long finalLogChannel = initLogChannel;

        // try to get mute role
        Role muteRole = null;
        try {
            muteRole = App.api.getRoleById(muteRoleID).orElseThrow();
        } catch (NoSuchElementException e) {
            String errorResponse = "Mute role with ID `" + muteRoleID + "` does not exist.";
            interaction.createImmediateResponder().setContent(errorResponse).respond();
            return;
        }

        // check permissions
        try {
            hasPermission(moderator, mutedUser, muteRole, server);
        } catch (NoPermissionException e) {
            e.sendExceptionResponse(interaction);
            return;
        }
        
        // mute user
        muteUser(mutedUser, muteRole, duration, server, finalLogChannel, reason);
        sendMuteResponse(interaction, mutedUser, duration);

        //send log message
        try {
            if (settings.isModLogEnabled() && settings.isLogMuteEnabled() && finalLogChannel != null) {
                App.api.getTextChannelById(finalLogChannel).ifPresent(channel -> {
                    channel.sendMessage(LogEmbed.getEmbed(EmbedType.Mute, mutedUser, moderator, new ReadableTime().compute(duration), reason));

                    // auto unmute message
                    EmbedBuilder unmuteEmbed = LogEmbed.getEmbed(
                        EmbedType.Unmute,
                        mutedUser,
                        App.api.getYourself(), 
                        "Temporary mute duration over.");
                    TaskScheduler.scheduleTask(
                        TaskSchedulerKeyPrefixs.TEMP_MUTE_EMBED+mutedUser.getId(), 
                        new EmbedDelay(unmuteEmbed, finalLogChannel, server.getId()).sendEmbedRunnable(),
                        duration, TimeUnit.SECONDS);
                });
                
            }
        } catch (DocumentUnavailableException e) {
            // e.printStackTrace();
        }
    }

    private static void muteUser(User mutedUser, Role muteRole, long duration, Server server, Long logChannelID, String reason) {
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
                new RoleDelay(muteRole.getId(), server.getId(), mutedUser.getId()).addRoleRunnable(),
                duration, TimeUnit.SECONDS);
        } else { //use mute role to perm mute if no duration set
            server.addRoleToUser(mutedUser, muteRole).join();
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
    private static boolean hasValidArguments(SlashCommandInteraction interaction, ServerSettings settings) throws DocumentUnavailableException {
        //check if user is in server
        if (interaction.requestArgumentUserValueByName("user").isEmpty()) { 
            interaction.createImmediateResponder().setContent("That user is not in the server!").setFlags(MessageFlag.EPHEMERAL).respond().join();
            return false;
        }
        //check if there is a muterole
        if (settings.getMuteRoleID() == null) { 
            interaction.createImmediateResponder().setContent("You do not have a mute role set!").setFlags(MessageFlag.EPHEMERAL).respond().join();
            return false;
        }
        // check if muterole is valid
        if (settings.getMuteRoleID().isEmpty() || App.api.getRoleById(settings.getMuteRoleID().get()).isEmpty()) {
            interaction.createImmediateResponder().setContent("Invalid mute role!").setFlags(MessageFlag.EPHEMERAL).respond().join();
            return false;
        }
        
        return true;
    }

    private static boolean hasPermission(User moderator, User mutedUser, Role muteRole, Server server) throws NoPermissionException {
        if (!server.canManageRole(moderator, muteRole)) {
            throw new NoPermissionException(moderator.getMentionTag() + " cannot manage the role: " + muteRole.getMentionTag());
        }
        if (!server.canTimeoutUser(moderator, mutedUser)) {
            throw new NoPermissionException(moderator.getMentionTag() + " cannot timeout the user: " + mutedUser.getMentionTag());
        }
        if (!server.canYouTimeoutUser(mutedUser)) {
            throw new NoPermissionException(App.api.getYourself().getMentionTag() + " cannot timeout the user: " + mutedUser.getMentionTag());
        }
        // if (!server.)
        return true;
    }
    private static class NoPermissionException extends Exception {
        final String exceptionReason;
        public NoPermissionException(String reason) {
            super(reason);
            exceptionReason = reason;
        }

        public void sendExceptionResponse(InteractionBase interaction) {
            interaction.createImmediateResponder().setContent(exceptionReason).setFlags(MessageFlag.SUPPRESS_NOTIFICATIONS).respond();
        }
    }

    public static void handleUnmuteCommand(SlashCommandInteraction interaction, ServerSettings settings) {
        try {
            // get variables
            User targetUser;
            Role muteRole;
            try {
                targetUser = interaction.getArgumentUserValueByName("User").get();
                muteRole = App.api.getRoleById(settings.getMuteRoleID().orElseThrow()).get();
            } catch (NoSuchElementException e) { // thrown if bad muterole
                interaction.createImmediateResponder().setContent("You do not have a valid mute role set!").respond();
                return;
            }

            // unmute and log
            unmuteUser(targetUser, targetUser, interaction.getArgumentStringValueByName("reason").orElse(""), settings);
            
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
    public static void unmuteUser(User offender, User moderator, String reason, ServerSettings serverSettings) throws DocumentUnavailableException {
        App.api.getServerById(serverSettings.getServerId()).get().createUpdater()
            .removeRoleFromUser(offender, App.api.getRoleById(serverSettings.getMuteRoleID().get()).get())
            .removeUserTimeout(offender)
            .update();
        
        logUnmute(offender, moderator, reason, serverSettings);
    }

    private static void logUnmute(User offender, User moderator, String reason, ServerSettings serverSettings) {
        try {
            if (serverSettings.isModLogEnabled() && serverSettings.isLogMuteEnabled() && serverSettings.getLogChannelID().isPresent()) {
                App.api.getServerTextChannelById(serverSettings.getLogChannelID().get()).get()
                    .sendMessage(LogEmbed.getEmbed(EmbedType.Unmute, offender, moderator, null, reason));
            }
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
        }
    }
    
}
