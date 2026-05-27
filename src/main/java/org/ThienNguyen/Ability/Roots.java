package org.ThienNguyen.Ability;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Roots implements IAbility {

    @Override
    public String getName() {
        return "ROOTS";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());

        
        if (target.hasMetadata("ROOTED")) return;
        target.setMetadata("ROOTED", new FixedMetadataValue(plugin, true));

        
        int durationTicks = 30 + ((level - 1) * 5);

        
        Location rootLoc = target.getLocation();

        
        rootLoc.getWorld().playSound(rootLoc, Sound.BLOCK_ROOTED_DIRT_PLACE, 1.0f, 0.8f);
        rootLoc.getWorld().playSound(rootLoc, Sound.BLOCK_CHERRY_WOOD_BREAK, 0.8f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                
                if (ticks >= durationTicks || target.isDead() || !target.isValid()) {
                    target.removeMetadata("ROOTED", plugin);
                    this.cancel();
                    return;
                }

                
                Location current = target.getLocation();
                if (current.getX() != rootLoc.getX() || current.getZ() != rootLoc.getZ()) {
                    
                    Location freezeLoc = rootLoc.clone();
                    freezeLoc.setYaw(current.getYaw());
                    freezeLoc.setPitch(current.getPitch());
                    target.teleport(freezeLoc);
                }

                
                if (ticks % 2 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double x = Math.cos(angle) * 0.5;
                        double z = Math.sin(angle) * 0.5;

                        
                        rootLoc.getWorld().spawnParticle(Particle.BLOCK_CRACK, rootLoc.clone().add(x, 0.1, z), 2, 0.1, 0.2, 0.1, 0.05,
                                org.bukkit.Material.OAK_LOG.createBlockData());

                        
                        if (ticks % 4 == 0) {
                            rootLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, rootLoc.clone().add(x, 0.5, z), 1, 0.1, 0.3, 0.1, 0.02);
                        }
                    }
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}