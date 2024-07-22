package ai.Commands;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bson.Document;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.ComponentType;
import org.javacord.api.entity.message.component.SelectMenuBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.InteractionBase;
import org.javacord.api.interaction.SelectMenuInteraction;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;

import ai.App;
import ai.Database.DocumentUnavailableException;
import ai.ServerSettings;
import ai.Constants.CustomID;
import ai.Constants.GlobalJsonKeys;
import ai.Constants.MuteJsonKeys;
import ai.Utility.CustomCoder;
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
            muteRoleID = settings.getMuteRoleID().get();
            logChannelID = settings.isLogMuteEnabled() ? settings.getLogChannelID().orElse(null) : null;
            
            // throw new DocumentUnavailableException("fake exception");
        } catch (DocumentUnavailableException e) { 
            e.printStackTrace();
            String unavailableMsg = "Currently unable to get mute role because my database is temporarily down.\n" +
                "Please select a one-time mute role to use to continue carrying out mute command.";

            ActionRow selectMenu = getSelectRoleMenu(interaction.getArgumentUserValueByName("User").get().getId(), duration);
            interaction.createImmediateResponder()
                .setContent(unavailableMsg)
                .addComponents(selectMenu)
                .setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }

        
        final User mutedUser = interaction.getArgumentUserValueByName("User").get();
        final Server server = interaction.getServer().get();
        final String reason = interaction.getArgumentStringValueByName("Reason").orElse("");
        final Role muteRole = App.api.getRoleById(muteRoleID).orElseThrow();
        
        // mute user
        muteUser(mutedUser, muteRole, duration, server, logChannelID, reason);

        // respond
        sendMuteResponse(interaction, mutedUser, duration);

        //send log message
        try {
            if (settings.isModLogEnabled() && settings.isLogMuteEnabled() && logChannelID != null) {
                App.api.getTextChannelById(logChannelID).ifPresent(channel -> {
                    channel.sendMessage(new LogEmbeds().mute(mutedUser, interaction.getUser(), new ReadableTime().compute(duration), reason));
                });
            }
        } catch (DocumentUnavailableException e) {
            // e.printStackTrace();
        }
        
    }

    public static void handleManualMute(SelectMenuInteraction interaction) {
        ManualMuteJSON muteData = ManualMuteJSON.decode(interaction.getCustomId());

        // ensure person using the command has permissions
        final User invoker = interaction.getUser();
        // if (invoker.getId() != muteData.moderatorID) {
        //     interaction.createImmediateResponder().setContent("Only the original moderator can carry out this action.").respond();
        //     return;
        // }

        // try to get muted user
        User mutedUser = null;
        try {
            mutedUser = App.api.getUserById(muteData.mutedUserID).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            interaction.createImmediateResponder().setContent("Discord could not find the user you are trying to mute.").respond();
            return;
        }
        
        // get mute role
        final Role muteRole = interaction.getSelectedRoles().get(0);

        // get server
        final Server server = interaction.getServer().get();

        // mute user
        final long durationSeconds = muteData.durationSeconds;
        muteUser(mutedUser, muteRole, durationSeconds, server, null, "");
        sendMuteResponse(interaction, mutedUser, durationSeconds);
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
            new RoleDelay(muteRole.getId(), mutedUser, server, logChannelID).tempMute(duration, TimeUnit.SECONDS);
        } else { //use mute role to perm mute
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

    private static ActionRow getSelectRoleMenu(long mutedUserID, long muteDuration) {
        String customIdJson = new ManualMuteJSON(mutedUserID, muteDuration).encode();
        System.out.println(customIdJson);
        return ActionRow.of(new SelectMenuBuilder(ComponentType.SELECT_MENU_ROLE, customIdJson).build());
    }

    private static class ManualMuteJSON extends CustomCoder {
        private static final String customID = CustomID.MANUAL_MUTE;
        public long mutedUserID;
        public long durationSeconds;

        public ManualMuteJSON(long mutedUserID, long durationSeconds) {
            this.mutedUserID = mutedUserID;
            this.durationSeconds = durationSeconds;
        }

        public String encode() {
            return new Document()
                .append(GlobalJsonKeys.customID, customID)
                .append(MuteJsonKeys.mutedUserID, mutedUserID)
                .append(MuteJsonKeys.durationSeconds, durationSeconds)
                    .toJson();
        }

        public static ManualMuteJSON decode(String json) {
            Document parsedJson = Document.parse(json);
            return new ManualMuteJSON(
                (Long) parsedJson.getLong(MuteJsonKeys.mutedUserID), 
                (Integer) parsedJson.getInteger(MuteJsonKeys.durationSeconds));
        }
    }

    public static void handleUnmuteCommand(SlashCommandInteraction interaction, ServerSettings settings) {
        try {
            // unmute
            try {
                interaction.getServer().get().createUpdater()
                    .removeRoleFromUser(interaction.getArgumentUserValueByName("User").get(), App.api.getRoleById(settings.getMuteRoleID().orElseThrow()).get())
                    .removeUserTimeout(interaction.getArgumentUserValueByName("User").get())
                    .update().join();
            } catch (NoSuchElementException e) { // thrown if null muteroleid
                interaction.createImmediateResponder().setContent("You do not have a valid mute role set!").respond();
                return;
            }
            
            // respond
            interaction.createImmediateResponder().setContent(interaction.getArgumentUserValueByName("User").get().getName() + " was unmuted.").setFlags(MessageFlag.EPHEMERAL).respond().join();
    
            // log message if applicable
            if (settings.isModLogEnabled() && settings.isLogMuteEnabled() && settings.getLogChannelID().isPresent()) {
                App.api.getServerTextChannelById(settings.getLogChannelID().get()).get()
                    .sendMessage(new LogEmbeds().unmute(interaction.getArgumentUserValueByName("User").get(), interaction.getUser()));
            }
        } catch (DocumentUnavailableException e) {
            DocumentUnavailableException.sendStandardResponse(interaction);
        }
        
    }
    
}
