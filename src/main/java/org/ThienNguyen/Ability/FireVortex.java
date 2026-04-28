package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class FireVortex implements IAbility {

    @Override
    public String getName() {
        return "FIRE_VORTEX";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead() || target.hasMetadata("IS_ABILITY")) return;

        // 1. Tính toán sát thương
        double tickPercent = 2.0 + (level * 1.0);
        double damagePerTick = baseDamage * (tickPercent / 100.0);
        final Location center = target.getLocation();

        // 2. Hiệu ứng âm thanh tối giản
        center.getWorld().playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);

        // 3. Vòng lặp xoáy lửa (3 giây)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60 || attacker == null) {
                    this.cancel();
                    return;
                }

                // HIỆU ỨNG HẠT TỐI GIẢN (Chỉ 2 hạt mỗi tick đối xứng nhau)
                for (int i = 0; i < 2; i++) {
                    double angle = (ticks * 0.4) + (i * Math.PI); // Xoay mượt mà
                    double x = Math.cos(angle) * 1.0;
                    double z = Math.sin(angle) * 1.0;

                    Location particleLoc = center.clone().add(x, 0.1, z);
                    // Giảm count xuống 1, speed cực thấp
                    center.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0.01);
                }

                // Gây sát thương mỗi 1 giây (20 ticks)
                if (ticks % 20 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.5f);

                    for (Entity entity : center.getWorld().getNearbyEntities(center, 1.2, 1.5, 1.2)) {
                        if (entity instanceof LivingEntity victim && !entity.equals(attacker)) {

                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                            victim.damage(damagePerTick, attacker);
                            victim.setFireTicks(30);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }.runTaskLater(Main.getInstance(), 1L);
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}