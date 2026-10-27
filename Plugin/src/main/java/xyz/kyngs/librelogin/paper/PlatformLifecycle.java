/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import org.bukkit.World;
import org.bukkit.entity.Player;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;

/**
 * Keeps the bootstrap independent from the platform implementation, so the platform class
 * (and its third-party dependencies) is only loaded after the libraries have been injected.
 */
public interface PlatformLifecycle extends LibreLoginPlugin<Player, World> {

    void enablePlatform();

    void disablePlatform();
}
