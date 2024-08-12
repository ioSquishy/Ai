package ai.Utility.Runnables;

import java.util.NoSuchElementException;

import org.javacord.api.entity.permission.Role;

import ai.App;
import ai.Utility.TaskScheduler;

public class RoleDelay {
    private final long roleID;
    private final long serverID;
    private final long userID;

    public RoleDelay(long roleID, long serverID, long userID) {
        this.roleID = roleID;
        this.serverID = serverID;
        this.userID = userID;
    }

    public Runnable addRoleRunnable() {
        return () -> {
            try {
                Role role = App.api.getRoleById(roleID).orElseThrow();
                App.api.getUserById(userID).thenAcceptAsync(user -> user.addRole(role, "role delay"));
            } catch (NoSuchElementException e) {
                // if role no longer exists send message to the server somewhere
                TaskScheduler.sendErrorMessage(serverID, ("Failed to add <@&" + roleID + "> to <@" + userID + ">"));
            }
        };
    }

    public Runnable removeRoleRunnable() {
        return () -> {
            try {
                Role role = App.api.getRoleById(roleID).orElseThrow();
                App.api.getUserById(userID).thenAcceptAsync(user -> user.removeRole(role, "role delay"));
            } catch (NoSuchElementException e) {
                // if role no longer exists send message to the server somewhere
                TaskScheduler.sendErrorMessage(serverID, ("Failed to remove <@&" + roleID + "> to <@" + userID + ">"));
            }
        };
    }
}
