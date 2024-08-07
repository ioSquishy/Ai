package ai.Commands;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.message.MessageFlag;
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
import ai.Database.DocumentUnavailableException;
import ai.ServerSettings;
import ai.Utility.LogEmbeds;
import ai.Utility.ReadableTime;
import ai.Utility.RoleDelay;


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
        Long logChannelID = null; // null if not logging
        // ensure valid arguments
        try {
            if (!hasValidArguments(interaction, settings)) {
                return;
            }
            // save mute role id and log channel id
            muteRoleID = settings.getMuteRoleID().orElseThrow();
            logChannelID = settings.isLogMuteEnabled() ? settings.getLogChannelID().orElse(null) : null;
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
        muteUser(mutedUser, muteRole, duration, server, logChannelID, reason);
        sendMuteResponse(interaction, mutedUser, duration);

        //send log message
        try {
            if (settings.isModLogEnabled() && settings.isLogMuteEnabled() && logChannelID != null) {
                App.api.getTextChannelById(logChannelID).ifPresent(channel -> {
                    channel.sendMessage(new LogEmbeds().mute(mutedUser, moderator, new ReadableTime().compute(duration), reason));
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
            new RoleDelay(muteRole.getId(), mutedUser, server).tempMute(duration, TimeUnit.SECONDS, logChannelID);
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
            User targetUser = interaction.getArgumentUserValueByName("User").get();
            // unmute
            try {
                interaction.getServer().get().createUpdater()
                    .removeRoleFromUser(targetUser, App.api.getRoleById(settings.getMuteRoleID().orElseThrow()).get())
                    .removeUserTimeout(targetUser)
                    .update();
            } catch (NoSuchElementException e) { // thrown if null muteroleid
                interaction.createImmediateResponder().setContent("You do not have a valid mute role set!").respond();
                return;
            }
            
            // respond
            interaction.createImmediateResponder().setContent(interaction.getArgumentUserValueByName("User").get().getName() + " was unmuted.").setFlags(MessageFlag.EPHEMERAL).respond();
    
            // log message if applicable
            if (settings.isModLogEnabled() && settings.isLogMuteEnabled() && settings.getLogChannelID().isPresent()) {
                App.api.getServerTextChannelById(settings.getLogChannelID().get()).get()
                    .sendMessage(new LogEmbeds().unmute(
                        interaction.getArgumentUserValueByName("User").get(),
                        interaction.getUser(),
                        Optional.ofNullable(interaction.getArgumentStringValueByName("Reason").orElse(null))));
            }
        } catch (DocumentUnavailableException e) {
            DocumentUnavailableException.sendStandardResponse(interaction);
        }
    }
    
}
