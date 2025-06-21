package fr.lye.biomentry.Models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPreferences {
    private static final Map<UUID, Boolean> notificationsEnabled = new HashMap<>();

    public static boolean areNotificationsEnabled(UUID playerUUID) {
        return notificationsEnabled.getOrDefault(playerUUID, true);
    }

    public static void setNotificationsEnabled(UUID playerUUID, boolean enabled) {
        notificationsEnabled.put(playerUUID, enabled);
    }

    public static void toggleNotifications(UUID playerUUID) {
        boolean current = areNotificationsEnabled(playerUUID);
        setNotificationsEnabled(playerUUID, !current);
    }

    public static void clearPreferences(UUID playerUUID) {
        notificationsEnabled.remove(playerUUID);
    }
}
