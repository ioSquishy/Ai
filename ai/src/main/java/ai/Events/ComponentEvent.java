package ai.Events;

import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.listener.interaction.ModalSubmitListener;
import org.javacord.api.util.event.ListenerManager;

import ai.App;
import ai.Constants.CustomID;
import ai.Commands.Settings;


public class ComponentEvent {

    public static ListenerManager<ModalSubmitListener> addModalSubmitListener() {
        return App.api.addModalSubmitListener(event -> {
            ModalInteraction interaction = event.getModalInteraction();
            long serverID = interaction.getServer().get().getId();
            
            switch (interaction.getCustomId()) {
                case CustomID.SETTINGS_MODAL :
                    Settings.handleSettingsModalSubmit(interaction, serverID);
                    break;
            }
        });
    }
    
}
