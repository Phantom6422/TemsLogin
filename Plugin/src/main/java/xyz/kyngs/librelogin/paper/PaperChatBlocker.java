/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Blocks Paper's native chat event while a player is not authorized. This class is only loaded and
 * registered on Paper, because the event does not exist on Spigot or Bukkit.
 */
public class PaperChatBlocker implements Listener {

    private final PaperLibreLogin plugin;

    public PaperChatBlocker(PaperLibreLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        var provider = plugin.getAuthorizationProvider();

        if (!provider.isAuthorized(event.getPlayer()) || provider.isAwaiting2FA(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
