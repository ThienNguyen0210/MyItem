package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireTripleShot implements IAbility {

    @Override
    public String getName() {
        return "FIRE_TRIPLE_SHOT";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        
        double multiplier = 0.10 + (Math.max(0, level - 1) * 0.05);
        double damageToDeal = baseDamage * multiplier;

        
        for (int i = 0; i < 3; i++) {
            final int shotIndex = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isDead() || !target.isValid() || !attacker.isOnline()) {
                        return;
                    }

                    
                    target.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.0f + (shotIndex * 0.2f));

                    
                    drawFireBeam(attacker.getEyeLocation().subtract(0, 0.3, 0), target.getEyeLocation());

                    

                    
                    target.setNoDamageTicks(0);

                    
                    target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                    
                    target.damage(damageToDeal, attacker);

                    
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
                    target.getWorld().spawnParticle(Particle.LAVA, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.1);

                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (target.isValid()) {
                                target.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }
                    }.runTaskLater(Main.getInstance(), 1L);
                }
            }.runTaskLater(Main.getInstance(), i * 5L);
        }
    }

    private void drawFireBeam(Location start, Location end) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Location loc = start.clone().add(direction.clone().multiply(d));
            loc.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.02);
            if (d % 1.5 == 0) {
                loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0, 0, 0, 0.01);
            }
        }
    }
}