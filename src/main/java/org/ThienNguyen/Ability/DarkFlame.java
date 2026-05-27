package org.ThienNguyen.Ability;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class DarkFlame implements IAbility {

    @Override
    public String getName() {
        return "DARK_FLAME";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());

        
        if (target.hasMetadata("DARK_FLAME_ACTIVE")) return;
        target.setMetadata("DARK_FLAME_ACTIVE", new FixedMetadataValue(plugin, true));

        
        double damagePercent = 10.0 + ((level - 1) * 5.0);
        double damagePerSecond = baseDamage * (damagePercent / 100.0);

        Random random = new Random();
        
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f);

        new BukkitRunnable() {
            int elapsedSeconds = 0;
            int ticks = 0;

            @Override
            public void run() {
                
                if (elapsedSeconds >= 5 || target.isDead() || !target.isValid()) {
                    target.removeMetadata("DARK_FLAME_ACTIVE", plugin);
                    this.cancel();
                    return;
                }

                
                Location loc = target.getLocation().add(0, 1, 0);
                for (int i = 0; i < 4; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 1.2;
                    double offsetY = (random.nextDouble() - 0.5) * 1.8;
                    double offsetZ = (random.nextDouble() - 0.5) * 1.2;

                    
                    loc.getWorld().spawnParticle(Particle.REDSTONE, loc.clone().add(offsetX, offsetY, offsetZ), 1, blackDust);

                    
                    if (ticks % 2 == 0) {
                        loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(offsetX, offsetY, offsetZ), 1, 0.02, 0.02, 0.02, 0.01);
                    }
                }

                
                if (ticks % 20 == 0) {
                    applySafeDamage(target, attacker, damagePerSecond, plugin);
                    loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, 0.5f, 0.5f);
                    elapsedSeconds++;
                }

                ticks += 2; 
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Phương thức gây sát thương an toàn để tránh StackOverflow
     */
    private void applySafeDamage(LivingEntity victim, Player attacker, double damage, Plugin plugin) {
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(plugin, true));
        victim.damage(damage, attacker);

        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isValid()) {
                    victim.removeMetadata("IS_ABILITY", plugin);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}