package ai.Events;

import org.javacord.api.listener.connection.LostConnectionListener;
import org.javacord.api.listener.connection.ReconnectListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Utility.TaskScheduler;

public class GatewayEvent {
    
    public static ListenerManager<LostConnectionListener> registerLostConnectionListener() {
        return App.api.addLostConnectionListener(event -> {
            App.gatewayDisconnected = true;
        });
    }

    public static ListenerManager<ReconnectListener> registerReconnectionListener() {
        return App.api.addReconnectListener(event -> {
            App.gatewayDisconnected = false;
            TaskScheduler.gatewayReconnectListener();
        });
    }

}
