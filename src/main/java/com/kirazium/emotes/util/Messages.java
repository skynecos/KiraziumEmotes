package com.kirazium.emotes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public final class Messages {
    private static final Component PREFIX = Component.text("Kirazium ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text("» ", NamedTextColor.DARK_PURPLE));

    private Messages() {
    }

    public static void info(CommandSender sender, String message) {
        sender.sendMessage(PREFIX.append(Component.text(message, NamedTextColor.GRAY)));
    }

    public static void success(CommandSender sender, String message) {
        sender.sendMessage(PREFIX.append(Component.text(message, NamedTextColor.LIGHT_PURPLE)));
    }

    public static void error(CommandSender sender, String message) {
        sender.sendMessage(PREFIX.append(Component.text(message, NamedTextColor.RED)));
    }
}
