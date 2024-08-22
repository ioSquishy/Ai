package ai.Events;

import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Handlers.MessageHandler;

public class MessageEvent {
    
    public static ListenerManager<MessageCreateListener> addMessageCreateListener() {
        return App.api.addMessageCreateListener(event -> {
            MessageHandler.handleMessage(event);
        });
    }

}
