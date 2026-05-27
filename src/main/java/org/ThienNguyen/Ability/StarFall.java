package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class StarFall implements IAbility {

    @Override
    public String getName() {
        return "STAR_FALL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        double multiplier = 0.20 + (Math.max(0, level - 1) * 0.10);
        double damageToDeal = baseDamage * multiplier;

        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                
                if (ticks >= 20 || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                
                drawCircle(target.getLocation(), 1.0, Color.LIME);
                ticks += 2;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isDead() || !target.isValid()) return;

                
                Location currentLoc = target.getLocation();
                Location skyLoc = currentLoc.clone().add(0, 10, 0);

                drawBeam(skyLoc, currentLoc);

                currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.5f);
                currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);

                
                target.setNoDamageTicks(0);
                target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                target.damage(damageToDeal, attacker);

                target.removeMetadata("IS_ABILITY", Main.getInstance());
            }
        }.runTaskLater(Main.getInstance(), 20L);
    }

    private void drawCircle(Location loc, double radius, Color color) {
        for (double t = 0; t < 30; t += 1.0) { 
            double angle = t * 2 * Math.PI / 30;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location particleLoc = loc.clone().add(x, 0.1, z);
            particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(color, 1.2f));
        }
    }

    private void drawBeam(Location start, Location end) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.25) {
            Location point = start.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.REDSTONE, point, 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.AQUA, 1.5f));
        }
    }
}