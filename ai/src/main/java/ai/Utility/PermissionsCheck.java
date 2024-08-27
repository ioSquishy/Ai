package ai.Utility;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.interaction.InteractionBase;

public class PermissionsCheck {
    public static boolean validate(TextChannel textChannel) throws InvalidPermissionsException {
        return true;
    }

    public static boolean validate(Role role) throws InvalidPermissionsException {
        return true;
    }

    public static class InvalidPermissionsException extends Exception {
        public final String exceptionReason;

        public InvalidPermissionsException(String reason) {
            super(reason);
            exceptionReason = reason;
        }

        public void sendExceptionResponse(InteractionBase interaction) {
            interaction.createImmediateResponder().setContent(exceptionReason).setFlags(MessageFlag.SUPPRESS_NOTIFICATIONS, MessageFlag.EPHEMERAL).respond();
        }
    }
}
