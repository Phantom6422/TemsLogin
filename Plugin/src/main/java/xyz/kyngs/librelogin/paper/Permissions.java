/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers the permission tree at runtime.
 * <p>
 * Paper plugins ({@code paper-plugin.yml}) do not read the {@code permissions} section of {@code plugin.yml},
 * so relying on the descriptor alone would leave {@code temslogin.admin} without its children on Paper.
 */
public final class Permissions {

    private static final Map<String, String> CHILDREN = new LinkedHashMap<>();

    static {
        CHILDREN.put("temslogin.reload.configuration", "Allows reloading the configuration");
        CHILDREN.put("temslogin.reload.messages", "Allows reloading the messages");
        CHILDREN.put("temslogin.dump", "Allows creating a dump");
        CHILDREN.put("temslogin.email.test", "Allows sending a test email");
        CHILDREN.put("temslogin.user.info", "Allows viewing user information");
        CHILDREN.put("temslogin.user.migrate", "Allows migrating user data");
        CHILDREN.put("temslogin.user.unregister", "Allows unregistering users");
        CHILDREN.put("temslogin.user.delete", "Allows deleting users");
        CHILDREN.put("temslogin.user.premium", "Allows enabling premium login for users");
        CHILDREN.put("temslogin.user.cracked", "Allows disabling premium login for users");
        CHILDREN.put("temslogin.user.register", "Allows registering users");
        CHILDREN.put("temslogin.user.login", "Allows forcing a user login");
        CHILDREN.put("temslogin.user.2faoff", "Allows disabling 2FA for users");
        CHILDREN.put("temslogin.user.emailoff", "Allows removing a user email");
        CHILDREN.put("temslogin.user.setemail", "Allows setting a user email");
        CHILDREN.put("temslogin.user.pass-change", "Allows changing a user password");
        CHILDREN.put("temslogin.user.alts", "Allows listing alternate accounts");
    }

    private Permissions() {
    }

    public static void register() {
        var manager = Bukkit.getPluginManager();

        var children = new LinkedHashMap<String, Boolean>();

        CHILDREN.forEach((name, description) -> {
            children.put(name, true);

            if (manager.getPermission(name) == null) {
                manager.addPermission(new Permission(name, description, PermissionDefault.OP));
            }
        });

        if (manager.getPermission("temslogin.admin") == null) {
            manager.addPermission(new Permission("temslogin.admin", "Grants every TemsLogin permission", PermissionDefault.OP, children));
        }

        if (manager.getPermission("temslogin.limbo.access") == null) {
            manager.addPermission(new Permission("temslogin.limbo.access", "Allows authorized players to enter a limbo world", PermissionDefault.OP));
        }
    }
}
