package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class PlagueSpread implements IAbility {

    @Override
    public String getName() {
        return "PLAGUE_SPREAD";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        // Bắt đầu lây nhiễm cho mục tiêu đầu tiên
        infect(attacker, target, level, baseDamage);
    }

    private void infect(Player attacker, LivingEntity victim, int level, double baseDamage) {
        if (victim == null || victim.isDead()) return;

        // 1. Tính toán sát thương: 5% + (level-1)*5%
        double multiplier = 0.05 + (Math.max(0, level - 1) * 0.05);
        double damagePerSecond = baseDamage * multiplier;

        // Hiệu ứng âm thanh khi dính bệnh
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.8f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; // 5 giây

            @Override
            public void run() {
                // KIỂM TRA NẾU MỤC TIÊU CHẾT TRƯỚC KHI HẾT 5 GIÂY -> LÂY LAN
                if (!victim.isValid() || victim.isDead()) {
                    spreadToNearby(attacker, victim.getLocation(), level, baseDamage);
                    this.cancel();
                    return;
                }

                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // A. Hiệu ứng Particle màu xanh nước biển (Dịch hạch)
                Location loc = victim.getLocation().add(0, 1, 0);
                // Sử dụng hạt SNEEZE (hắt xì) hoặc WATER_SPLASH kết hợp Dust xanh nước
                victim.getWorld().spawnParticle(Particle.REDSTONE, loc, 8, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(0, 128, 255), 1.2f));
                victim.getWorld().spawnParticle(Particle.SNEEZE, loc, 3, 0.2, 0.2, 0.2, 0.01);

                // B. Gây sát thương mỗi 1 giây (20 ticks)
                if (ticks % 20 == 0) {
                    victim.setNoDamageTicks(0);
                    victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                    victim.damage(damagePerSecond, attacker);
                    victim.removeMetadata("IS_ABILITY", Main.getInstance());

                    // Âm thanh ăn mòn
                    victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 0.5f);
                }

                ticks += 2;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    private void spreadToNearby(Player attacker, Location deathLoc, int level, double baseDamage) {
        double spreadRadius = 8.0;
        List<Entity> nearby = (List<Entity>) deathLoc.getWorld().getNearbyEntities(deathLoc, spreadRadius, spreadRadius, spreadRadius);

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity nextVictim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {
                // Chỉ lây cho 1 con gần nhất để tránh lag server (hoặc lây hết nếu bạn muốn)
                infect(attacker, nextVictim, level, baseDamage);

                // Hiệu ứng hạt bay từ xác con cũ sang con mới
                deathLoc.getWorld().spawnParticle(Particle.SOUL, deathLoc, 10, 0.5, 0.5, 0.5, 0.1);
                break; // Xóa dòng này nếu muốn lây cho TẤT CẢ quái xung quanh
            }
        }
    }
}