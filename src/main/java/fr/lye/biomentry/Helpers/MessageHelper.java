package fr.lye.biomentry.Helpers;

import org.bukkit.ChatColor;

public class MessageHelper {
    public static String FormatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
