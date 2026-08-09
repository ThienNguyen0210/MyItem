package org.ThienNguyen.Listener.Passive.Trigger;

import org.ThienNguyen.Listener.Passive.PassiveManager;
import org.bukkit.Bukkit;
import org.ThienNguyen.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;


public class PassiveCacheListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                () -> PassiveManager.getInstance().invalidatePassiveCache(player.getUniqueId()), 1L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        PassiveManager.getInstance().invalidatePassiveCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        PassiveManager.getInstance().invalidatePassiveCache(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        PassiveManager.getInstance().invalidatePassiveCache(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        PassiveManager.getInstance().clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        
        PassiveManager.getInstance().invalidatePassiveCache(event.getEntity().getUniqueId());
    }
}