package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class ShadowWave implements IAbility {

    @Override
    public String getName() {
        return "SHADOW_WAVE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        
        int effectDuration = 60 + (Math.max(0, level - 1) * 20);
        double radius = 10.0;

        Location center = attacker.getLocation();

        
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);

        
        new BukkitRunnable() {
            double r = 1.0;
            @Override
            public void run() {
                if (r > radius) {
                    this.cancel();
                    return;
                }

                
                for (int i = 0; i < 40; i++) {
                    double angle = i * 2 * Math.PI / 40;
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    Location pLoc = center.clone().add(x, 0.2, z);

                    
                    pLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, pLoc, 1, 0, 0, 0, 0.02);
                    
                    pLoc.getWorld().spawnParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.BLACK, 1.5f));
                }
                r += 1.5; 
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        
        List<Entity> targets = attacker.getNearbyEntities(radius, 5.0, radius);
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                
                victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, effectDuration, 0));

                
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, effectDuration, 0));

                
                
                double instantDmg = baseDamage * 0.05;
                victim.setNoDamageTicks(0);
                victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                victim.damage(instantDmg, attacker);
                victim.removeMetadata("IS_ABILITY", Main.getInstance());

                
                victim.getWorld().spawnParticle(Particle.SQUID_INK, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }
}