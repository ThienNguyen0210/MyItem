package org.ThienNguyen.Ability;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class VenomSpread implements IAbility {

    @Override
    public String getName() {
        return "VENOM_SPREAD";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null) return;

        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());
        final Location center = target.getLocation().clone();

        
        int durationTicks = 40 + ((level - 1) * 10);
        int poisonLevel = level - 1;
        final double radius = 2.5;

        center.getWorld().playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 1.2f, 0.5f);

        Color poisonColor = Color.fromRGB(43, 104, 21);
        Particle.DustOptions dust = new Particle.DustOptions(poisonColor, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                
                if (ticks >= durationTicks) {
                    this.cancel();
                    return;
                }

                
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = Math.random() * radius;
                    Location pLoc = center.clone().add(Math.cos(angle) * r, 0.2, Math.sin(angle) * r);
                    pLoc.getWorld().spawnParticle(Particle.REDSTONE, pLoc, 1, dust);
                }

                
                if (ticks % 5 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 2.0, radius)) {
                        if (entity instanceof LivingEntity victim) {
                            if (victim.equals(attacker)) continue;

                            
                            
                            PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 100, poisonLevel, false, true, true);
                            victim.addPotionEffect(poison);

                            
                            if (ticks % 20 == 0) {
                                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5f, 0.5f);
                            }
                        }
                    }
                }
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}