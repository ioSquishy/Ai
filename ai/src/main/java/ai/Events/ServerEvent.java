package ai.Events;

import java.time.Duration;

import org.javacord.api.listener.server.ServerLeaveListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Data.Database;
import ai.Utility.TaskScheduler;

public class ServerEvent {

    // public static void addLostConnectionListener() {
    //     App.api.addLostConnectionListener(event -> {
    //         try {
    //             event.wait(Duration.ofMinutes(1).toMillis());
    //             TaskScheduler.pauseTaskScheduler();
    //         } catch (InterruptedException e) {
    //             e.printStackTrace();
    //         }
    //     });
    // }

    // public static void addConnectionListener() {
    //     App.api.addReconnectListener(event -> {
    //         if (TaskScheduler.isShutDown()) {
    //             TaskScheduler.unpauseTaskScheduler();
    //         } else {
    //             App.api.getLostConnectionListeners().get(0).notify();
    //         }
    //     });
    // }

    public static ListenerManager<ServerLeaveListener> addLeaveListener() {
        return App.api.addServerLeaveListener(event -> {
            Database.removeServer(event.getServer().getId());
        });
    }

}
