package org.ThienNguyen.Listener.Passive.Trigger;

import org.ThienNguyen.Listener.Passive.PassiveManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;


public class PassiveDeathListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player actor = event.getEntity();

        
        
        
        
        LivingEntity killer = actor.getKiller();

        PassiveManager.getInstance().trigger(
                PassiveTrigger.ON_DEATH,
                actor,
                killer,
                0.0,
                false
        );
    }
}