package ai.Events;

import org.javacord.api.listener.server.ServerLeaveListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Data.Database;

public class ServerEvent {

    public static ListenerManager<ServerLeaveListener> registerMemberLeaveListener() {
        return App.api.addServerLeaveListener(event -> {
            Database.removeServer(event.getServer().getId());
        });
    }

}
