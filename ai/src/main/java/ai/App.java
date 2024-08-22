package ai;

import ai.Events.ComponentEvent;
import ai.Events.MessageEvent;
import ai.Events.ServerEvent;
import ai.Events.ServerMemberEvent;
import ai.Events.SlashCommandEvent;
import ai.Commands.*;
import ai.Data.Database;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import com.squareup.moshi.Moshi;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

import io.github.cdimascio.dotenv.Dotenv;

public class App implements Serializable {
    private static final long serialVersionUID = 0;

    public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).setAllIntents().login().join();
    public static boolean gatewayDisconnected = false;

    public static final Moshi Moshi = new Moshi.Builder().build();

    public static transient final long startEpoch = Instant.now().getEpochSecond();
    public static transient final String version = "2";
    public static void main(String[] args) {
        System.out.println("Ai is online!");
        Database.initMongoDB();

        // create/update slash commands
        api.bulkOverwriteGlobalApplicationCommands(
            Set.of(
                Ping.createCommand(),
                BotInfo.createCommand(),
                Mute.muteSlashCommand(),
                Unban.unbanSlashCommand(),
                Lockdown.createCommand(),
                Settings.createSettingsCommand()
            )
        );
        
        // add event listeners
        SlashCommandEvent.addSlashCommandCreateListener();

        MessageEvent.addMessageCreateListener();

        ComponentEvent.addModalSubmitListener();

        ServerMemberEvent.addJoinListener();
        ServerMemberEvent.addLeaveListener();
        ServerMemberEvent.addBanListener();
        ServerMemberEvent.addUnbanListener();
        ServerMemberEvent.addTimeoutChangeListener();
        
        ServerEvent.addLeaveListener();
    }
    
}
