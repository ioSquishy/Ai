package ai.Utility;

import java.util.List;
import java.util.Map.Entry;

import static java.util.Map.entry;

import java.util.Arrays;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;

import ai.App;
import ai.Database.DocumentUnavailableException;
import ai.ServerSettings;

public class JoinHandler {

    public static final ServerMemberJoinEvent testJoinEvent = new ServerMemberJoinEvent() {
        @Override
        public Server getServer() {
            return App.api.getServerById(1000221094009114644L).orElse(null);
        }

        @Override
        public DiscordApi getApi() {
            return App.api;
        }

        @Override
        public User getUser() {
            return App.api.getYourself();
        }
    };

    private final List<Entry<String, String>> joinMessageReplaceValues;
    private final ServerSettings serverSettings;

    public JoinHandler(ServerMemberJoinEvent joinEvent, ServerSettings serverSettings) {
        this.serverSettings = serverSettings;
        joinMessageReplaceValues = Arrays.asList(
            entry("\\{USER\\}", joinEvent.getUser().getMentionTag()),
            entry("\\\\n", "\n")
        );
    }

    public String getJoinMessage() {
        String joinMessage = serverSettings.getJoinMessage();
        for (Entry<String, String> replaceEntry : joinMessageReplaceValues) {
            joinMessage = joinMessage.replaceAll(replaceEntry.getKey(), replaceEntry.getValue());
        }
        return joinMessage;
    }

    public void sendJoinMessage() {
        try {
            App.api.getServerTextChannelById(serverSettings.getJoinMessageChannelID()).ifPresent(channel -> {
                channel.sendMessage(getJoinMessage());
            });
        } catch (DocumentUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void addRoles() {

    }
}
