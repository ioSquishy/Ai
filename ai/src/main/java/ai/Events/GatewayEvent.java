package ai.Events;

import ai.App;

public class GatewayEvent {
    
    public static void addLostConnectionListener() {
        App.api.addLostConnectionListener(event -> {
            App.gatewayDisconnected = true;
        });
    }

    public static void addConnectionListener() {
        App.api.addReconnectListener(event -> {
            App.gatewayDisconnected = false;
        });
    }

}
