package ai.Utility;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

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

    public static boolean canManageRoles(Server server) {
        return server.canYouManageRoles();
    }

    public static boolean canManageRole(Server server, Role role) {
        return server.canManageRole(server.getApi().getYourself(), role);
    }

    public static boolean canTimeoutUser(Server server, User user) {
        return server.canYouTimeoutUser(user);
    }

    public static boolean canBanUsers(Server server) {
        return server.canYouBanUsers();
    }

    /**
     * Returns true if the user's highest role is greater than the targets highest role.
     * @param server server to check
     * @param user if this user's role is greater than target, returns true
     * @param target if this uer's role is less than user, returns true
     */
    public static boolean userIsAboveUser(Server server, User user, User target) {
        int userPos = user.getRoles(server).get(0).getPosition();
        int targetPos = target.getRoles(server).get(0).getPosition();
        return userPos > targetPos;
    }
}
