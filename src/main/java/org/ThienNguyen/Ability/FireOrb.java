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

public class FireOrb implements IAbility {

    @Override
    public String getName() {
        return "FIRE_ORB";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null) return;

        
        final Location centerLoc = target.getLocation().add(0, 0.5, 0);

        double burnMultiplier = 0.06 + (Math.max(0, level - 1) * 0.03);
        double burnDamage = baseDamage * burnMultiplier;
        double explodeDamage = baseDamage * (0.10 + (Math.max(0, level - 1) * 0.05));

        new BukkitRunnable() {
            int ticks = 0;
            boolean exploded = false;

            @Override
            public void run() {
                if (ticks >= 100) { 
                    if (!exploded) {
                        spawnFireWave(attacker, centerLoc, explodeDamage);
                        exploded = true;
                        this.cancel();
                    }
                    return;
                }

                
                double angle = ticks * 0.5;
                double x = Math.cos(angle) * 1.0;
                double z = Math.sin(angle) * 1.0;
                centerLoc.getWorld().spawnParticle(Particle.FLAME, centerLoc.clone().add(x, 0.5, z), 3, 0.02, 0.02, 0.02, 0.02);
                centerLoc.getWorld().spawnParticle(Particle.SMALL_FLAME, centerLoc.clone().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.01);

                
                if (ticks % 20 == 0) {
                    centerLoc.getWorld().playSound(centerLoc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
                    for (Entity entity : centerLoc.getWorld().getNearbyEntities(centerLoc, 2, 2, 2)) {
                        if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof ArmorStand)) {
                            applyAbilityDamage(attacker, victim, burnDamage);
                            victim.setFireTicks(40);
                        }
                    }
                }

                ticks += 2;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    
    private void spawnFireWave(Player attacker, Location center, double damage) {
        center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

        
        Set<Integer> hitList = new HashSet<>();

        new BukkitRunnable() {
            double currentRadius = 0.5;
            final double maxRadius = 10.0;

            @Override
            public void run() {
                if (currentRadius >= maxRadius) {
                    this.cancel();
                    return;
                }

                
                for (int i = 0; i < 360; i += 10) {
                    double radians = Math.toRadians(i);
                    double x = Math.cos(radians) * currentRadius;
                    double z = Math.sin(radians) * currentRadius;
                    Location particleLoc = center.clone().add(x, 0.1, z);

                    center.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0.05);
                    if (currentRadius > 5) { 
                        center.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 1, 0, 0, 0, 0.02);
                    }
                }

                
                for (Entity entity : center.getWorld().getNearbyEntities(center, currentRadius, 2, currentRadius)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof ArmorStand)) {
                        if (!hitList.contains(victim.getEntityId())) {
                            hitList.add(victim.getEntityId());
                            applyAbilityDamage(attacker, victim, damage);

                            
                            Vector push = victim.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8).setY(0.2);
                            victim.setVelocity(push);
                        }
                    }
                }

                currentRadius += 1.0; 
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    private void applyAbilityDamage(Player attacker, LivingEntity victim, double damage) {
        victim.setNoDamageTicks(0);
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        victim.damage(damage, attacker);
        victim.removeMetadata("IS_ABILITY", Main.getInstance());
    }
}