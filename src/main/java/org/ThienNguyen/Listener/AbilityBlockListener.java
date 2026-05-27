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
        
        event.blockList().removeIf(block -> block.hasMetadata("UNBREAKABLE_FLOWER"));

        
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

    
    @EventHandler
    public void onCrystalDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof EnderCrystal && event.getEntity().hasMetadata("UNBREAKABLE_CRYSTAL")) {
            event.setCancelled(true); 
        }
    }
}