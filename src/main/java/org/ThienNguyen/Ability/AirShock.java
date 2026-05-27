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

public class AirShock implements IAbility {

    @Override
    public String getName() {
        return "AIR_SHOCK";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        
        double percent = 5.0 + (Math.max(0, level - 1) * 3.0);
        double extraDamage = baseDamage * (percent / 100.0);

        
        double currentExtra = 0.0;
        if (target.hasMetadata("ABILITY_EXTRA_DAMAGE")) {
            currentExtra = target.getMetadata("ABILITY_EXTRA_DAMAGE").get(0).asDouble();
        }
        target.setMetadata("ABILITY_EXTRA_DAMAGE", new FixedMetadataValue(Main.getInstance(), currentExtra + extraDamage));

        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isDead() || !target.isValid()) return;

                
                
                double yForce = 0.6 + (level * 0.2);

                
                if (yForce > 40.0) yForce = 40.0;

                
                Vector pushBack = attacker.getLocation().getDirection().setY(0).normalize().multiply(0.3);

                
                Vector finalVelocity = new Vector(0, yForce, 0).add(pushBack);

                
                target.setVelocity(finalVelocity);

                
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
            }
        }.runTaskLater(Main.getInstance(), 1L); 

        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 5 || target.isDead()) {
                    this.cancel();
                    return;
                }
                
                Location loc = target.getLocation().add(0, 0.1, 0);
                target.getWorld().spawnParticle(Particle.CLOUD, loc, 5, 0.3, 0.1, 0.3, 0.05);
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 1L, 2L);
    }
}