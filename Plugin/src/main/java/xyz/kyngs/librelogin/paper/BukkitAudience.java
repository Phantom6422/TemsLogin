/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * An {@link Audience} implementation for platforms that do not natively support Adventure
 * (Bukkit and Spigot). Components are converted to legacy strings before being sent.
 */
public class BukkitAudience implements Audience {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final long MILLIS_PER_TICK = 50L;

    private final CommandSender sender;

    public BukkitAudience(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void sendMessage(Component message) {
        sender.sendMessage(LEGACY.serialize(message));
    }

    @Override
    public void sendActionBar(Component message) {
        if (sender instanceof Player player) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(LEGACY.serialize(message)));
        }
    }

    @Override
    public void sendPlayerListHeader(Component header) {
        if (sender instanceof Player player) {
            player.setPlayerListHeader(LEGACY.serialize(header));
        }
    }

    @Override
    public void sendPlayerListFooter(Component footer) {
        if (sender instanceof Player player) {
            player.setPlayerListFooter(LEGACY.serialize(footer));
        }
    }

    @Override
    public void sendPlayerListHeaderAndFooter(Component header, Component footer) {
        if (sender instanceof Player player) {
            player.setPlayerListHeaderFooter(LEGACY.serialize(header), LEGACY.serialize(footer));
        }
    }

    @Override
    public void showTitle(Title title) {
        if (!(sender instanceof Player player)) {
            return;
        }

        var mainTitle = LEGACY.serialize(title.title());
        var subTitle = title.subtitle() == null ? "" : LEGACY.serialize(title.subtitle());
        var times = title.times();

        if (times == null) {
            player.sendTitle(mainTitle, subTitle);
        } else {
            player.sendTitle(mainTitle, subTitle, ticks(times.fadeIn()), ticks(times.stay()), ticks(times.fadeOut()));
        }
    }

    @Override
    public void clearTitle() {
        if (sender instanceof Player player) {
            player.resetTitle();
        }
    }

    @Override
    public void resetTitle() {
        clearTitle();
    }

    private static int ticks(Duration duration) {
        return (int) (duration.toMillis() / MILLIS_PER_TICK);
    }
}
