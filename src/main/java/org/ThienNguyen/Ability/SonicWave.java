package org.ThienNguyen.Ability;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SonicWave implements IAbility {

    @Override
    public String getName() {
        return "SONIC_WAVE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        
        double damagePercent = 6.0 + (level * 3.0);
        double finalDamage = baseDamage * (damagePercent / 100.0);

        Location startLoc = attacker.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        
        startLoc.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.5f);

        new BukkitRunnable() {
            int distance = 0;
            Location currentLoc = startLoc.clone();
            final List<Integer> hitEntities = new ArrayList<>(); 

            @Override
            public void run() {
                
                if (distance > 15) {
                    this.cancel();
                    return;
                }

                
                currentLoc.add(direction);

                
                currentLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, currentLoc, 1, 0, 0, 0, 0);
                currentLoc.getWorld().spawnParticle(Particle.CRIT, currentLoc, 5, 0.2, 0.2, 0.2, 0.05);

                
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (entity instanceof LivingEntity victim && !victim.equals(attacker)) {
                        if (!hitEntities.contains(victim.getEntityId())) {
                            
                            victim.damage(finalDamage, attacker);
                            hitEntities.add(victim.getEntityId());

                            
                            currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.2f);
                        }
                    }
                }

                distance++;
            }
        }.runTaskTimer(JavaPlugin.getProvidingPlugin(getClass()), 0L, 1L);

    }
}