package com.chibashr.allthewebhooks.util;

import org.bukkit.Location;

public final class LocationFormatter {
    private LocationFormatter() {
    }

    public static String format(Location location) {
        if (location == null) {
            return "";
        }
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
