package ai.Events;

import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.listener.interaction.ModalSubmitListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Constants.CustomID;
import ai.Commands.SettingsCommand;


public class ComponentEvent {

    public static ListenerManager<ModalSubmitListener> registerModalSubmitListener() {
        return App.api.addModalSubmitListener(event -> {
            ModalInteraction interaction = event.getModalInteraction();
            
            switch (interaction.getCustomId()) {
                case CustomID.SETTINGS_MODAL :
                    SettingsCommand.handleSettingsModalSubmit(interaction);
                    break;
            }
        });
    }
    
}
