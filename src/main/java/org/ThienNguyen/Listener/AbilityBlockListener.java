package org.ThienNguyen.Listener;

import org.bukkit.entity.EnderCrystal;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class
AbilityBlockListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("UNBREAKABLE_FLOWER")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        // Chặn block hoa không bị nổ
        event.blockList().removeIf(block -> block.hasMetadata("UNBREAKABLE_FLOWER"));

        // Chặn EndCrystal kỹ năng không tự nổ tung địa hình khi bị tác động
        if (event.getEntity() instanceof EnderCrystal && event.getEntity().hasMetadata("UNBREAKABLE_CRYSTAL")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().hasMetadata("UNBREAKABLE_FLOWER")) {
            event.setCancelled(true);
        }
    }

    // --- LOGIC MỚI: CHẶN CRYSTAL BỊ PHÁ HỦY ---
    @EventHandler
    public void onCrystalDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof EnderCrystal && event.getEntity().hasMetadata("UNBREAKABLE_CRYSTAL")) {
            event.setCancelled(true); // Không thể bị đánh nổ
        }
    }
}