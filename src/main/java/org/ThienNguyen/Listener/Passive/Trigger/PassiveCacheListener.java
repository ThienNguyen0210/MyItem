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

/**
 * Invalidate passiveIdsCache (PassiveManager) mỗi khi equipment của player có thể đã đổi.
 * Tương tự cách CacheListener gọi refreshCache() cho PlayerCombatCache — cùng nguyên tắc:
 * chỉ tính lại khi có thay đổi thật, không tính lại mỗi đòn đánh.
 *
 * Delay 1 tick khi click inventory để chắc Bukkit đã commit thay đổi item trước khi đọc lại.
 */
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
        // Phòng trường hợp đồ rớt/đổi khi chết — cache sẽ tự build lại đúng lúc cần.
        PassiveManager.getInstance().invalidatePassiveCache(event.getEntity().getUniqueId());
    }
}