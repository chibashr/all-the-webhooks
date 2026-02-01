package com.chibashr.allthewebhooks.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AsyncExecutor {
    private final JavaPlugin plugin;

    public AsyncExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void runAsync(Runnable task) {
        try {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } catch (NoSuchMethodError error) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
}
