package org.ThienNguyen.Listener;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class CritEffectManager {

    public static void playCritEffect(LivingEntity target) {
        if (target == null || !target.isValid()) return;

        Location loc = target.getLocation().add(0, target.getHeight() / 2.0, 0);

        
        target.getWorld().spawnParticle(
                Particle.CRIT,
                loc,
                15,          
                0.3, 0.5, 0.3, 
                0.1            
        );

        
        
    }
}