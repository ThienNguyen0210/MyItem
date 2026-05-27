package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; 
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class HellfireBreathSkill implements ISkill {
    @Override public String getName() { return "HellfireBreath"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {


        double skillPercent = 0.6 + (level * 0.1);

        Location startLoc = player.getEyeLocation();
        player.getWorld().playSound(startLoc, Sound.ENTITY_GHAST_SHOOT, 1.5f, 0.5f);

        Set<LivingEntity> targets = new HashSet<>();

        
        for (int i = -2; i <= 2; i++) {
            Vector direction = startLoc.getDirection().clone();
            double angle = i * 0.15; 

            double cos = Math.cos(angle); double sin = Math.sin(angle);
            double x = direction.getX() * cos - direction.getZ() * sin;
            double z = direction.getX() * sin + direction.getZ() * cos;
            direction.setX(x).setZ(z);

            
            for (double d = 1; d <= 10; d += 0.5) {
                Location point = startLoc.clone().add(direction.clone().multiply(d));

                
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.05, 0.05, 0.05, 0.02);
                if (point.getBlock().getType().isSolid()) break;

                
                for (Entity entity : point.getWorld().getNearbyEntities(point, 0.6, 0.6, 0.6)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                        
                        if (!targets.contains(victim)) {
                            targets.add(victim);

                            
                            player.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                            
                            victim.setMetadata("ABILITY_EXTRA_DAMAGE", new FixedMetadataValue(Main.getInstance(), skillPercent));

                            
                            victim.setNoDamageTicks(0);
                            victim.damage(0.01, player);

                            
                            victim.setFireTicks(40);
                            victim.getWorld().spawnParticle(Particle.SOUL, victim.getEyeLocation(), 3, 0.2, 0.2, 0.2, 0.05);

                            
                            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                                player.removeMetadata("IS_ABILITY", Main.getInstance());
                                victim.removeMetadata("ABILITY_EXTRA_DAMAGE", Main.getInstance());
                            }, 1L);
                        }
                    }
                }
            }
        }
    }
}