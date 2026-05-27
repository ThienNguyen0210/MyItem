package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class WindTornado implements IAbility {

    @Override
    public String getName() {
        return "WIND_TORNADO";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        
        double multiplier = 0.10 + (Math.max(0, level - 1) * 0.05);
        double finalDamage = baseDamage * multiplier;

        Location startLoc = attacker.getLocation().add(0, 0.5, 0);
        Vector direction = startLoc.getDirection().normalize();

        
        startLoc.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);

        new BukkitRunnable() {
            int distanceTraveled = 0;
            final int maxDistance = 15;
            final Set<Integer> hitEntities = new HashSet<>(); 

            @Override
            public void run() {
                if (distanceTraveled >= maxDistance) {
                    this.cancel();
                    return;
                }

                
                startLoc.add(direction);

                
                for (int i = 0; i < 5; i++) {
                    double angle = i * Math.PI / 2.5;
                    double x = Math.cos(angle) * 0.5;
                    double z = Math.sin(angle) * 0.5;
                    startLoc.getWorld().spawnParticle(Particle.CLOUD, startLoc.clone().add(x, i * 0.3, z), 1, 0, 0, 0, 0.02);
                }
                startLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, startLoc, 1, 0.1, 0.1, 0.1, 0);

                
                for (Entity entity : startLoc.getWorld().getNearbyEntities(startLoc, 1.5, 2.0, 1.5)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof ArmorStand)) {

                        if (!hitEntities.contains(victim.getEntityId())) {
                            hitEntities.add(victim.getEntityId());

                            
                            victim.setVelocity(new Vector(0, 4.6, 0));

                            
                            victim.setNoDamageTicks(0);
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                            victim.damage(finalDamage, attacker);
                            victim.removeMetadata("IS_ABILITY", Main.getInstance());

                            
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.2f);
                        }
                    }
                }

                distanceTraveled++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L); 
    }
}