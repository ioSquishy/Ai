package ai.Experiments;

import java.time.Instant;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.user.UserChangePendingEvent;

import ai.App;

public class RuleAcceptTime {
    private static long botSetupChannelID = 818259653175148544L;

    public static void createPendingListener() {
        App.api.addUserChangePendingListener(event -> {
            if (event.getNewPending() == false) {
                logTime(event);
            }
        });
    }

    private static void logTime(UserChangePendingEvent event) {
        User user = event.getUser();
        Server server = event.getServer();

        long joinTimeMilliEpoch = user.getJoinedAtTimestamp(server).get().toEpochMilli();
        long ruleAcceptTimeMilliEpoch = Instant.now().toEpochMilli();

        long timeToAccept = ruleAcceptTimeMilliEpoch - joinTimeMilliEpoch;

        server.getChannelById(botSetupChannelID).ifPresent(channel -> {
            String logMessage = user.getMentionTag() + " accepted rules in " + (timeToAccept/1000) + "s and " + (timeToAccept%1000) + "ms";
            channel.asTextChannel().get().sendMessage(logMessage);
        });
    }
}