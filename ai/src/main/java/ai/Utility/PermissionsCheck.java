package ai.Utility;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;

public class PermissionsCheck {
    public static boolean canSendMessages(TextChannel channel, boolean dmOwnerIfFalse) {
        if (channel.canYouWrite()) {
            return true;
        } else {
            if (dmOwnerIfFalse) {
                channel.asServerChannel().get().getServer().getOwner().get().sendMessage("I cannot send messages in the channel: <#" + channel.getId() + ">!");
            }
            return false;
        }
    }

    public static boolean canReadAuditLog(Server server, boolean dmOwnerIfFalse) {
        if (server.canYouViewAuditLog()) {
            return true;
        } else {
            if (dmOwnerIfFalse) {
                server.getOwner().get().sendMessage("I cannot read the audit log!");
            }
            return false;
        }
    }

    public static boolean canDeleteMessages(TextChannel channel) {
        return channel.canYouManageMessages();
    }

    public static boolean canManageRole(Server server, Role role) {
        return server.canManageRole(server.getApi().getYourself(), role);
    }
}
