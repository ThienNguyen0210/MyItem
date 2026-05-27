package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class PlagueSpread implements IAbility {

    @Override
    public String getName() {
        return "PLAGUE_SPREAD";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        
        infect(attacker, target, level, baseDamage);
    }

    private void infect(Player attacker, LivingEntity victim, int level, double baseDamage) {
        if (victim == null || victim.isDead()) return;

        
        double multiplier = 0.05 + (Math.max(0, level - 1) * 0.05);
        double damagePerSecond = baseDamage * multiplier;

        
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.8f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; 

            @Override
            public void run() {
                
                if (!victim.isValid() || victim.isDead()) {
                    spreadToNearby(attacker, victim.getLocation(), level, baseDamage);
                    this.cancel();
                    return;
                }

                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                
                Location loc = victim.getLocation().add(0, 1, 0);
                
                victim.getWorld().spawnParticle(Particle.REDSTONE, loc, 8, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(0, 128, 255), 1.2f));
                victim.getWorld().spawnParticle(Particle.SNEEZE, loc, 3, 0.2, 0.2, 0.2, 0.01);

                
                if (ticks % 20 == 0) {
                    victim.setNoDamageTicks(0);
                    victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    victim.damage(damagePerSecond, attacker);
                    victim.removeMetadata("IS_ABILITY", Main.getInstance());

                    
                    victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 0.5f);
                }

                ticks += 2;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    private void spreadToNearby(Player attacker, Location deathLoc, int level, double baseDamage) {
        double spreadRadius = 8.0;
        List<Entity> nearby = (List<Entity>) deathLoc.getWorld().getNearbyEntities(deathLoc, spreadRadius, spreadRadius, spreadRadius);

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity nextVictim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {
                
                infect(attacker, nextVictim, level, baseDamage);

                
                deathLoc.getWorld().spawnParticle(Particle.SOUL, deathLoc, 10, 0.5, 0.5, 0.5, 0.1);
                break; 
            }
        }
    }
}