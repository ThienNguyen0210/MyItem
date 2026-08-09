package org.ThienNguyen.Listener.Passive.Trigger;

import org.ThienNguyen.Listener.Passive.PassiveManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;


public class PassiveBlockBreakListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        PassiveManager.getInstance().triggerBlockBreak(event.getPlayer(), event.getBlock());
    }
}