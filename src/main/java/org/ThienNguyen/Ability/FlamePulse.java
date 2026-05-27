package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class FlamePulse implements IAbility {

    @Override
    public String getName() {
        return "FLAME_PULSE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead() || target.hasMetadata("IS_ABILITY")) return;

        
        double radius = 1.0 + level;
        double percent = 50.0 + (level * 10.0);
        double areaDamage = baseDamage * (percent / 100.0);

        Location center = target.getLocation();

        
        target.setFireTicks(40 + (level * 10)); 
        center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.5f);

        
        new BukkitRunnable() {
            double currentRadius = 0.5;

            @Override
            public void run() {
                if (currentRadius > radius) {
                    this.cancel();
                    return;
                }

                
                int particles = (int) (currentRadius * 15);
                for (int i = 0; i < particles; i++) {
                    double angle = 2 * Math.PI * i / particles;
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;

                    Location pLoc = center.clone().add(x, 0.2, z);
                    center.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0, 0.1, 0, 0.02);
                }

                
                for (Entity entity : center.getWorld().getNearbyEntities(center, currentRadius, 1.5, currentRadius)) {
                    if (entity instanceof LivingEntity victim) {
                        
                        if (victim.equals(attacker) || victim.equals(target)) continue;

                        if (victim.hasMetadata("IS_ABILITY")) continue;

                        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        victim.damage(areaDamage, attacker);

                        
                        victim.setFireTicks(40);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }.runTaskLater(Main.getInstance(), 1L);
                    }
                }

                currentRadius += 0.5;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}