package ai.Utility;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.interaction.InteractionBase;

public class InteractionException extends Exception {
    private String exceptionMessage;

    public InteractionException() {
        super();
    }

    public InteractionException(String message) {
        super(message);
        this.exceptionMessage = message;
    }

    public void sendExceptionResponse(InteractionBase interaction) {
        interaction.createImmediateResponder().setContent(exceptionMessage).setFlags(MessageFlag.SUPPRESS_NOTIFICATIONS, MessageFlag.EPHEMERAL).respond();
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
