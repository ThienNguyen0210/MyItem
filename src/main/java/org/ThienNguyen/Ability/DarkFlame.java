package org.ThienNguyen.Ability;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class DarkFlame implements IAbility {

    @Override
    public String getName() {
        return "DARK_FLAME";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());

        // Chống chồng lặp hiệu ứng Dark Flame trên cùng 1 mục tiêu
        if (target.hasMetadata("DARK_FLAME_ACTIVE")) return;
        target.setMetadata("DARK_FLAME_ACTIVE", new FixedMetadataValue(plugin, true));

        // Tính toán sát thương: 10% mặc định + (level-1)*5%
        double damagePercent = 10.0 + ((level - 1) * 5.0);
        double damagePerSecond = baseDamage * (damagePercent / 100.0);

        Random random = new Random();
        // Tạo Dust màu đen (Size 1.5)
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f);

        new BukkitRunnable() {
            int elapsedSeconds = 0;
            int ticks = 0;

            @Override
            public void run() {
                // Kết thúc sau 5 giây (100 ticks) hoặc mục tiêu chết
                if (elapsedSeconds >= 5 || target.isDead() || !target.isValid()) {
                    target.removeMetadata("DARK_FLAME_ACTIVE", plugin);
                    this.cancel();
                    return;
                }

                // --- 1. Hiệu ứng khói lửa đen bao trùm mục tiêu (chạy mỗi tick) ---
                Location loc = target.getLocation().add(0, 1, 0);
                for (int i = 0; i < 4; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 1.2;
                    double offsetY = (random.nextDouble() - 0.5) * 1.8;
                    double offsetZ = (random.nextDouble() - 0.5) * 1.2;

                    // Hạt bụi đen
                    loc.getWorld().spawnParticle(Particle.REDSTONE, loc.clone().add(offsetX, offsetY, offsetZ), 1, blackDust);

                    // Thêm một chút hạt khói lớn cho cảm giác "làn khói"
                    if (ticks % 2 == 0) {
                        loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(offsetX, offsetY, offsetZ), 1, 0.02, 0.02, 0.02, 0.01);
                    }
                }

                // --- 2. Gây sát thương mỗi giây (mỗi 20 ticks) ---
                if (ticks % 20 == 0) {
                    applySafeDamage(target, attacker, damagePerSecond, plugin);
                    loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, 0.5f, 0.5f);
                    elapsedSeconds++;
                }

                ticks += 2; // Chạy mỗi 2 ticks để tiết kiệm hiệu năng (0.1 giây/lần lặp)
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Phương thức gây sát thương an toàn để tránh StackOverflow
     */
    private void applySafeDamage(LivingEntity victim, Player attacker, double damage, Plugin plugin) {
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(plugin, true));
        victim.damage(damage, attacker);

        // Xóa đánh dấu sau 1 tick để EventDamage có thể nhận diện
        new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isValid()) {
                    victim.removeMetadata("IS_ABILITY", plugin);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}