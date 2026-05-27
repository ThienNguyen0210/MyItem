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

public class Explode implements IAbility {

    @Override
    public String getName() {
        return "EXPLODE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead() || target.hasMetadata("IS_ABILITY")) return;

        
        double explodePercent = 8.0 + (level * 4.0);
        double explosionDamage = baseDamage * (explodePercent / 100.0);

        Location loc = target.getLocation();

        
        loc.getWorld().createExplosion(loc, 1.5f, false, false);

        
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.1);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 25, 0.5, 0.5, 0.5, 0.15);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity victim && !entity.equals(attacker)) {

                
                victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                
                victim.damage(explosionDamage, attacker);

                
                
                victim.setFireTicks(40);

                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 1L);
            }
        }
    }
}