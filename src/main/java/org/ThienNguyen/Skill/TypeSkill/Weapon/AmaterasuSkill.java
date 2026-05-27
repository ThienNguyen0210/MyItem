package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; 
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.attribute.Attribute;

public class AmaterasuSkill implements ISkill {
    @Override public String getName() { return "Amaterasu"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        
        LivingEntity target = getTargetEntity(player, 15);

        if (target == null || target.isDead()) {
            return;
        }

        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        
        double damagePerSecond = realPower * (0.25 + (level * 0.10));

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);

        
        new BukkitRunnable() {
            private int ticks = 0;
            @Override
            public void run() {
                
                if (target == null || target.isDead() || !target.isValid() || ticks >= 100) {
                    this.cancel();
                    return;
                }

                
                Location loc = target.getLocation().add(0, 1, 0);
                target.getWorld().spawnParticle(Particle.REDSTONE, loc, 15, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.BLACK, 1.5F));
                target.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 3, 0.2, 0.4, 0.2, 0.02);

                
                if (ticks % 20 == 0) {

                    
                    
                    target.setNoDamageTicks(0);

                    
                    target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                    
                    target.damage(damagePerSecond, player);

                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (target.isValid()) {
                                target.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }
                    }.runTaskLater(Main.getInstance(), 2L);

                    
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    
    private LivingEntity getTargetEntity(Player player, int range) {
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity target && player.hasLineOfSight(e)) {
                
                Vector toTarget = target.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
                double dot = toTarget.dot(player.getEyeLocation().getDirection());

                if (dot > 0.98) { 
                    return target;
                }
            }
        }
        return null;
    }
}