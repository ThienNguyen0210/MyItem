package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class LeafStorm implements IAbility {

    @Override
    public String getName() {
        return "LEAF_STORM";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        double multiplier = 0.10 + (Math.max(0, level - 1) * 0.05);
        double damagePerSecond = baseDamage * multiplier;
        double radius = 4.0;

        // Lấy vị trí cố định ngay khi kích hoạt
        final Location staticCenter = attacker.getLocation();

        attacker.getWorld().playSound(staticCenter, Sound.BLOCK_AZALEA_LEAVES_STEP, 1.5f, 1.0f);
        attacker.getWorld().playSound(staticCenter, Sound.ITEM_ELYTRA_FLYING, 0.5f, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60) {
                    this.cancel();
                    return;
                }

                // A. Hiệu ứng lá rơi tại vị trí CỐ ĐỊNH (Giảm mật độ xuống còn 5 hạt mỗi 2 ticks)
                for (int i = 0; i < 5; i++) {
                    double offsetX = (Math.random() * radius * 2) - radius;
                    double offsetZ = (Math.random() * radius * 2) - radius;
                    double offsetY = Math.random() * 3.0;

                    Location leafLoc = staticCenter.clone().add(offsetX, offsetY, offsetZ);
                    leafLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, leafLoc, 1, 0.1, 0.1, 0.1, 0.01);

                    if (i == 0) { // Cứ mỗi lần chạy chỉ hiện 1 hạt bụi xanh cho nhẹ
                        leafLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, leafLoc, 1, 0, 0, 0, 0);
                    }
                }

                // B. Gây sát thương mỗi 1 giây tại vị trí CỐ ĐỊNH
                if (ticks % 20 == 0) {
                    // Dùng getNearbyEntities từ staticCenter thay vì từ attacker
                    Collection<Entity> nearby = staticCenter.getWorld().getNearbyEntities(staticCenter, radius, 3.0, radius);
                    for (Entity entity : nearby) {
                        if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                            victim.getWorld().spawnParticle(Particle.BLOCK_DUST, victim.getLocation().add(0, 1, 0), 5, 0.1, 0.1, 0.1, 0.05, Material.OAK_LEAVES.createBlockData());
                            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.6f, 1.2f);

                            victim.setNoDamageTicks(0);
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                            victim.damage(damagePerSecond, attacker);
                            victim.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }
                }

                ticks += 2;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }
}