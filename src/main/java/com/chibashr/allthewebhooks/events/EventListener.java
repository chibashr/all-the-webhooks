package com.chibashr.allthewebhooks.events;

import com.chibashr.allthewebhooks.routing.EventRouter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.TimeSkipEvent;

public class EventListener implements Listener {
    private final EventRegistry registry;
    private final EventRouter router;

    public EventListener(EventRegistry registry, EventRouter router) {
        this.registry = registry;
        this.router = router;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        router.handleEvent(registry.buildContext("player.join", event));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        router.handleEvent(registry.buildContext("player.quit", event));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        router.handleEvent(registry.buildContext("player.chat", event));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        router.handleEvent(registry.buildContext("player.command", event));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBreak(BlockBreakEvent event) {
        router.handleEvent(registry.buildContext("player.break.block", event));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPlace(BlockPlaceEvent event) {
        router.handleEvent(registry.buildContext("player.place.block", event));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        router.handleEvent(registry.buildContext("player.death", event));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        router.handleEvent(registry.buildContext("entity.damage.player", event));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        router.handleEvent(registry.buildContext("inventory.open", event));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldTimeSkip(TimeSkipEvent event) {
        router.handleEvent(registry.buildContext("world.time.change", event));
    }
}
