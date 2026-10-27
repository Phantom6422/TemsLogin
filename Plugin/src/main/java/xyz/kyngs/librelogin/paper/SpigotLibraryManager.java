/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.kyngs.librarian.LibraryManager;
import xyz.kyngs.librarian.classloader.URLClassLoaderHelper;

import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Loads the runtime libraries on Bukkit and Spigot, where Paper's plugin loader is not available.
 */
public class SpigotLibraryManager extends LibraryManager {

    private final JavaPlugin plugin;
    private final URLClassLoaderHelper helper;

    public SpigotLibraryManager(JavaPlugin plugin) {
        super(plugin.getDataFolder().toPath().resolve("libraries"));
        this.plugin = plugin;

        var classLoader = plugin.getClass().getClassLoader();

        if (!(classLoader instanceof URLClassLoader urlClassLoader)) {
            throw new IllegalStateException("The plugin class loader is not a URLClassLoader, cannot load libraries");
        }

        this.helper = new URLClassLoaderHelper(urlClassLoader, this);
    }

    @Override
    protected void addToClasspath(Path file) {
        helper.addToClasspath(file);
    }

    @Override
    protected InputStream getPluginResourceAsInputStream(String path) {
        return plugin.getResource(path);
    }
}
