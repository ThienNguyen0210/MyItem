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

public class FairyChain implements IAbility {

    @Override
    public String getName() {
        return "FAIRY_CHAIN";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        
        double radius = 4.0;
        double damageToDeal = baseDamage * (0.10 + (Math.max(0, level - 1) * 0.04));

        
        List<Entity> nearby = attacker.getNearbyEntities(radius, 3.0, radius);

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                
                new BukkitRunnable() {
                    int ticks = 0;
                    boolean isBroken = false;

                    @Override
                    public void run() {
                        
                        if (!victim.isValid() || victim.isDead() || !attacker.isOnline()) {
                            this.cancel();
                            return;
                        }

                        
                        double distance = attacker.getLocation().distance(victim.getLocation());
                        if (distance > 4.5) { 
                            isBroken = true;
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.5f);
                            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                            this.cancel();
                            return;
                        }

                        
                        drawChain(attacker.getLocation().add(0, 1, 0), victim.getLocation().add(0, 1, 0));

                        
                        if (ticks % 10 == 0) {
                            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.8f);
                        }

                        
                        if (ticks >= 60) {
                            
                            victim.setNoDamageTicks(0);
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                            victim.damage(damageToDeal, attacker);
                            victim.removeMetadata("IS_ABILITY", Main.getInstance());

                            
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 100));

                            
                            victim.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, victim.getLocation().add(0, 1, 0), 1);
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);

                            this.cancel();
                            return;
                        }

                        ticks += 2;
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 2L);
            }
        }
    }

    private void drawChain(Location start, Location end) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(d));
            
            point.getWorld().spawnParticle(Particle.REDSTONE, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 150, 200), 1.0f));
            
            if (d % 1.2 == 0) {
                point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0.01);
            }
        }
    }
}