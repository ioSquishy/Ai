package ai;

import ai.Events.ComponentEvent;
import ai.Events.ServerEvent;
import ai.Events.ServerMemberEvent;
import ai.Events.SlashCommandEvent;
import ai.Commands.*;
import ai.Data.Database;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import java.io.Serializable;
import java.time.Instant;


import io.github.cdimascio.dotenv.Dotenv;

public class App implements Serializable {
    private static final long serialVersionUID = 0;

    public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("TOKEN")).setAllIntents().login().join();

    public static transient final long startEpoch = Instant.now().getEpochSecond();
    public static transient final String version = "2";
    public static void main(String[] args) {
        System.out.println("Ai is online!");
        Database.initMongoDB();

        // create slash commands
        Ping.createCommand().createGlobal(api).join();
        BotInfo.createCommand().createGlobal(api).join();
        Mute.muteSlashCommand().createGlobal(api).join();
        Unban.unbanSlashCommand().createGlobal(api).join();
        Lockdown.createCommand().createGlobal(api).join();
        Settings.createSettingsCommand().createGlobal(api).join();
        
        // add event listeners
        SlashCommandEvent.addSlashCommandCreateListener();

        ComponentEvent.addModalSubmitListener();

        ServerMemberEvent.addJoinListener();
        ServerMemberEvent.addLeaveListener();
        ServerMemberEvent.addBanListener();
        ServerMemberEvent.addUnbanListener();
        ServerMemberEvent.addTimeoutChangeListener();
        
        ServerEvent.addLeaveListener();
    }
    
}
