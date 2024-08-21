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

    public static ListenerManager<ServerMemberJoinListener> addJoinListener() {
        return App.api.addServerMemberJoinListener(event -> {
            JoinHandler.handleJoinEvent(event);
        });
    }

    public static ListenerManager<ServerMemberLeaveListener> addLeaveListener() {
        return App.api.addServerMemberLeaveListener(event -> {
            LeaveHandler.handleLeaveEvent(event);
        });
    }

    public static ListenerManager<ServerMemberBanListener> addBanListener() {
        return App.api.addServerMemberBanListener(event -> {
            BanHandler.handleBanEvent(event);
        });
    }

    public static ListenerManager<ServerMemberUnbanListener> addUnbanListener() {
        return App.api.addServerMemberUnbanListener(event -> {
            BanHandler.handleUnbanEvent(event);
        });
    }

    public static ListenerManager<UserChangeTimeoutListener> addTimeoutChangeListener() {
        return App.api.addUserChangeTimeoutListener(event -> {
            TimeoutHandler.handleTimeoutEvent(event);
        });
    }

}
