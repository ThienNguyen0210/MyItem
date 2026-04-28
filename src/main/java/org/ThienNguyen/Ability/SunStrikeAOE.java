package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class SunStrikeAOE implements IAbility {

    @Override
    public String getName() {
        return "SUN_STRIKE_AOE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        // 1. Tính toán sát thương: 10% + (level-1)*4%
        double multiplier = 0.10 + (Math.max(0, level - 1) * 0.04);
        double damageToDeal = baseDamage * multiplier;
        double radius = 5.0;

        // 2. Lấy danh sách kẻ địch xung quanh
        List<Entity> nearbyEnemies = attacker.getNearbyEntities(radius, 4.0, radius);

        for (Entity entity : nearbyEnemies) {
            if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                // Hiệu ứng cảnh báo tại vị trí quái
                drawWarningEffect(victim.getLocation());

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!victim.isValid() || victim.isDead()) return;

                        Location hitLoc = victim.getLocation().add(0, 0.5, 0);
                        Location skyLoc = hitLoc.clone().add(0, 10, 0);

                        // Vẽ tia giáng xuống
                        drawSunBeam(skyLoc, hitLoc);

                        // Hiệu ứng tại điểm nổ (Sửa lỗi Particle ở đây)
                        hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_PHANTOM_BITE, 1.0f, 0.8f);
                        hitLoc.getWorld().spawnParticle(Particle.FLAME, hitLoc, 10, 0.3, 0.3, 0.3, 0.05);

                        // Thay thế ORANGE_DUST bằng REDSTONE (màu cam)
                        hitLoc.getWorld().spawnParticle(Particle.REDSTONE, hitLoc, 5, 0.2, 0.2, 0.2, 0,
                                new Particle.DustOptions(Color.ORANGE, 1.0f));

                        // Gây sát thương chống loop
                        victim.setNoDamageTicks(0);
                        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                        victim.damage(damageToDeal, attacker);

                        victim.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 5L);
            }
        }

        attacker.getWorld().playSound(attacker.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 2.0f);
    }

    private void drawWarningEffect(Location loc) {
        loc.getWorld().spawnParticle(Particle.REDSTONE, loc.clone().add(0, 0.1, 0), 10, 0.5, 0, 0.5, 0,
                new Particle.DustOptions(Color.YELLOW, 1.5f));
    }

    private void drawSunBeam(Location start, Location end) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.3) {
            Location point = start.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.REDSTONE, point, 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.YELLOW, 1.2f));

            if (d % 2 == 0) {
                point.getWorld().spawnParticle(Particle.FLAME, point, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }
}