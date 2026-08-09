package org.ThienNguyen.Ability;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BubbleDeflector implements IAbility {
    @Override
    public String getName() {
        return "Bubble_Deflector";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());
        Location startLoc = target.getLocation().add(0, 1.0, 0);

        // Sửa ở đây: Kiểm tra an toàn tránh vector bằng 0 gây ra NaN
        Vector direction;
        try {
            direction = target.getLocation().subtract(attacker.getLocation()).toVector();
            if (direction.lengthSquared() < 1.0E-7) {
                direction = attacker.getLocation().getDirection();
            }
            direction.normalize();
        } catch (Exception e) {
            direction = attacker.getLocation().getDirection();
        }

        double damageMultiplier = 0.6 + ((level - 1) * 0.1);
        double finalDamage = baseDamage * damageMultiplier;

        applySafeDamage(target, attacker, finalDamage, plugin);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BOAT_PADDLE_LAND, 1.0f, 1.2f);

        final Vector finalDirection = direction; // Cần final để dùng trong BukkitRunnable

        new BukkitRunnable() {
            int ticks = 0;
            final double piercingRange = 5.0;
            final Set<LivingEntity> damagedEntities = new HashSet<>();

            @Override
            public void run() {
                if (ticks >= 15) {
                    this.cancel();
                    return;
                }

                for (double d = 0; d <= piercingRange; d += 0.5) {
                    Location pLoc = startLoc.clone().add(finalDirection.clone().multiply(d));
                    pLoc.getWorld().spawnParticle(Particle.BUBBLE, pLoc, 2, 0.1, 0.1, 0.1, 0.02);
                    if (ticks % 4 == 0) {
                        pLoc.getWorld().spawnParticle(Particle.FISHING, pLoc, 1, 0, 0, 0, 0);
                    }
                }

                if (ticks % 5 == 0) {
                    damagedEntities.clear();
                    damageEntitiesInBeam(attacker, startLoc, finalDirection, piercingRange, finalDamage, target, plugin);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applySafeDamage(LivingEntity victim, Player attacker, double damage, Plugin plugin) {
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(plugin, true));
        victim.damage(damage, attacker);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isValid()) victim.removeMetadata("IS_ABILITY", plugin);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void damageEntitiesInBeam(Player attacker, Location start, Vector dir, double range, double damage, LivingEntity initialTarget, Plugin plugin) {
        Location end = start.clone().add(dir.clone().multiply(range));
        Collection<Entity> entities = start.getWorld().getNearbyEntities(start.clone().add(dir.clone().multiply(range/2)), range, range, range);

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity nearbyLiving && entity != attacker && entity != initialTarget) {
                if (isEntityInBeam(start, end, dir, nearbyLiving)) {
                    applySafeDamage(nearbyLiving, attacker, damage, plugin);
                    nearbyLiving.getWorld().spawnParticle(Particle.BUBBLE_POP, nearbyLiving.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
    }

    private boolean isEntityInBeam(Location start, Location end, Vector direction, LivingEntity entity) {
        Location entityLoc = entity.getLocation().add(0, 1, 0);
        Vector startToEntity = entityLoc.clone().subtract(start).toVector();
        double dotProduct = startToEntity.dot(direction);

        if (dotProduct < 0 || dotProduct > start.distance(end)) return false;

        Vector closestPointOnLine = start.clone().add(direction.clone().multiply(dotProduct)).toVector();
        double distanceSquared = entityLoc.toVector().distanceSquared(closestPointOnLine);

        return distanceSquared < 1.2;
    }
}