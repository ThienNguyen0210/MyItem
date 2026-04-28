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

        // 1. Tính toán sát thương: 20% + (level-1)*10%
        double multiplier = 0.20 + (Math.max(0, level - 1) * 0.10);
        double finalDamage = baseDamage * multiplier;

        // Danh sách các mục tiêu đã bị giật để không giật lặp lại
        Set<Integer> struckEntities = new HashSet<>();

        // THAY ĐỔI TẠI ĐÂY:
        // Thay vì: attacker.getLocation().add(0, 1, 0)
        // Chúng ta dùng: target.getLocation().add(0, 1, 0) làm điểm xuất phát (sourceLoc)
        // Điều này sẽ khiến tia Dust không vẽ từ người chơi ra nữa.
        chainLightning(attacker, target.getLocation().add(0, 1, 0), target, finalDamage, 4, struckEntities);

        // Âm thanh kích hoạt ban đầu tại vị trí mục tiêu
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.5f);
    }
    private void chainLightning(Player attacker, Location sourceLoc, LivingEntity currentTarget, double damage, int remainingJumps, Set<Integer> struckEntities) {
        if (currentTarget == null || remainingJumps < 0) return;

        // Đánh dấu mục tiêu hiện tại đã bị trúng
        struckEntities.add(currentTarget.getEntityId());

        // A. Hiệu ứng tia điện (Dust) từ nguồn đến mục tiêu hiện tại
        drawElectricArc(sourceLoc, currentTarget.getLocation().add(0, 1, 0));

        // B. Gây sát thương
        applyDamage(attacker, currentTarget, damage);
        currentTarget.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentTarget.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
        currentTarget.getWorld().playSound(currentTarget.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 0.8f, 2.0f);

        // C. Tìm mục tiêu tiếp theo để lan sang
        if (remainingJumps > 0) {
            LivingEntity nextTarget = findNextChainTarget(currentTarget, struckEntities, 8.0);
            if (nextTarget != null) {
                // Lan sang mục tiêu tiếp theo sau 2 ticks để tạo cảm giác tia điện đang chạy
                final double nextDamage = damage; // Giữ nguyên dame hoặc giảm dần tùy bạn
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
                // Không lan ngược lại người tấn công
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
        // Tạo hiệu ứng tia điện zic-zac nhẹ bằng cách thêm offset ngẫu nhiên
        for (double i = 0; i < distance; i += 0.2) {
            Vector point = vec.clone().normalize().multiply(i);
            Location particleLoc = start.clone().add(point);

            // Thêm một chút độ lệch để tia điện trông tự nhiên hơn
            double offsetX = (Math.random() - 0.5) * 0.1;
            double offsetY = (Math.random() - 0.5) * 0.1;
            double offsetZ = (Math.random() - 0.5) * 0.1;

            start.getWorld().spawnParticle(Particle.REDSTONE, particleLoc.add(offsetX, offsetY, offsetZ), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 100), 0.8f)); // Màu vàng điện
        }
    }

    private void applyDamage(Player attacker, LivingEntity victim, double damage) {
        victim.setNoDamageTicks(0);
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        victim.damage(damage, attacker);
        victim.removeMetadata("IS_ABILITY", Main.getInstance());
    }
}