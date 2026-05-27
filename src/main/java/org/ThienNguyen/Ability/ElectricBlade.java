package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ElectricBlade implements IAbility {

    @Override
    public String getName() {
        return "ELECTRIC_BLADE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        
        double multiplier = 0.20 + (Math.max(0, level - 1) * 0.10);
        double finalDamage = baseDamage * multiplier;

        
        Set<Integer> struckEntities = new HashSet<>();

        
        
        
        
        chainLightning(attacker, target.getLocation().add(0, 1, 0), target, finalDamage, 4, struckEntities);

        
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.5f);
    }
    private void chainLightning(Player attacker, Location sourceLoc, LivingEntity currentTarget, double damage, int remainingJumps, Set<Integer> struckEntities) {
        if (currentTarget == null || remainingJumps < 0) return;

        
        struckEntities.add(currentTarget.getEntityId());

        
        drawElectricArc(sourceLoc, currentTarget.getLocation().add(0, 1, 0));

        
        applyDamage(attacker, currentTarget, damage);
        currentTarget.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentTarget.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
        currentTarget.getWorld().playSound(currentTarget.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 0.8f, 2.0f);

        
        if (remainingJumps > 0) {
            LivingEntity nextTarget = findNextChainTarget(currentTarget, struckEntities, 8.0);
            if (nextTarget != null) {
                
                final double nextDamage = damage; 
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    chainLightning(attacker, currentTarget.getLocation().add(0, 1, 0), nextTarget, nextDamage, remainingJumps - 1, struckEntities);
                }, 2L);
            }
        }
    }

    private LivingEntity findNextChainTarget(LivingEntity current, Set<Integer> struckEntities, double radius) {
        List<Entity> nearby = current.getNearbyEntities(radius, radius, radius);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : nearby) {
            if (e instanceof LivingEntity next && !(e instanceof ArmorStand) && !struckEntities.contains(e.getEntityId())) {
                
                if (e instanceof Player && ((Player) e).getGameMode() == GameMode.CREATIVE) continue;

                double dist = e.getLocation().distanceSquared(current.getLocation());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = next;
                }
            }
        }
        return closest;
    }

    private void drawElectricArc(Location start, Location end) {
        Vector vec = end.toVector().subtract(start.toVector());
        double distance = start.distance(end);
        
        for (double i = 0; i < distance; i += 0.2) {
            Vector point = vec.clone().normalize().multiply(i);
            Location particleLoc = start.clone().add(point);

            
            double offsetX = (Math.random() - 0.5) * 0.1;
            double offsetY = (Math.random() - 0.5) * 0.1;
            double offsetZ = (Math.random() - 0.5) * 0.1;

            start.getWorld().spawnParticle(Particle.REDSTONE, particleLoc.add(offsetX, offsetY, offsetZ), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 100), 0.8f)); 
        }
    }

    private void applyDamage(Player attacker, LivingEntity victim, double damage) {
        victim.setNoDamageTicks(0);
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        victim.damage(damage, attacker);
        victim.removeMetadata("IS_ABILITY", Main.getInstance());
    }
}