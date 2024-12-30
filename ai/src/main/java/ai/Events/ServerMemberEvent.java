package ai.Events;

import org.javacord.api.listener.server.member.ServerMemberBanListener;
import org.javacord.api.listener.server.member.ServerMemberJoinListener;
import org.javacord.api.listener.server.member.ServerMemberLeaveListener;
import org.javacord.api.listener.server.member.ServerMemberUnbanListener;
import org.javacord.api.listener.user.UserChangeTimeoutListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Handlers.BanHandler;
import ai.Handlers.JoinHandler;
import ai.Handlers.LeaveHandler;
import ai.Handlers.TimeoutHandler;

public class ServerMemberEvent {

    public static ListenerManager<ServerMemberJoinListener> registerJoinListener() {
        return App.api.addServerMemberJoinListener(event -> {
            JoinHandler.handleJoinEvent(event);
        });
    }

    public static ListenerManager<ServerMemberLeaveListener> registerLeaveListener() {
        return App.api.addServerMemberLeaveListener(event -> {
            LeaveHandler.handleLeaveEvent(event);
        });
    }

    public static ListenerManager<ServerMemberBanListener> registerBanListener() {
        return App.api.addServerMemberBanListener(event -> {
            BanHandler.handleBanEvent(event);
        });
    }

    public static ListenerManager<ServerMemberUnbanListener> registerUnbanListener() {
        return App.api.addServerMemberUnbanListener(event -> {
            BanHandler.handleUnbanEvent(event);
        });
    }

    public static ListenerManager<UserChangeTimeoutListener> registerTimeoutChangeListener() {
        return App.api.addUserChangeTimeoutListener(event -> {
            TimeoutHandler.handleTimeoutEvent(event);
        });
    }
}
