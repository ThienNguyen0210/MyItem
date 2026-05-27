package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; 
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SongAm implements ISkill {
    @Override public String getName() { return "SongAm"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "HIT"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        
        double skillMultiplier = 0.7 + (level * 0.1);
        double finalSkillDamage = realPower * skillMultiplier;

        double range = 15.0;
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        
        player.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.0f);

        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (double d = 0; d < range; d += 0.5) {
                    Location point = startLoc.clone().add(direction.clone().multiply(d));

                    
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);

                    
                    for (Entity entity : point.getWorld().getNearbyEntities(point, 1.2, 1.2, 1.2)) {
                        if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                            
                            
                            victim.setNoDamageTicks(0);

                            
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                            
                            victim.damage(finalSkillDamage, player);

                            
                            victim.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, victim.getLocation(), 1);
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

                            
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (victim.isValid()) {
                                        victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                    }
                                }
                            }.runTaskLater(Main.getInstance(), 2L);

                            return; 
                        }
                    }

                    if (point.getBlock().getType().isSolid()) break;
                }
            }
        }.runTask(Main.getInstance());
    }
}