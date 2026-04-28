package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DashSkill implements ISkill {
    @Override public String getName() { return "Dash"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamage) {
        // 1. Tính toán sức mạnh lướt (Hệ số nhân velocity)
        double power = 1.8 + (level * 0.2);

        // 2. Xử lý hướng lướt
        Vector direction = player.getLocation().getDirection();
        // Nhấc nhẹ Y lên 0.2 để tránh bị kẹt chân vào slab/stair khi lướt
        direction.setY(0.2).normalize();

        // 3. Thực hiện lướt
        player.setVelocity(direction.multiply(power));

        // 4. Hiệu ứng âm thanh & hình ảnh
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.2, 0.1, 0.2, 0.05);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getEyeLocation(), 1, 0, 0, 0, 0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.8f, 2.0f);

        // Chống sát thương rơi (Fall Distance)
        player.setFallDistance(-10.0f);

        // 5. [Thêm mới] Gây sát thương xung kích khi lướt (Tùy chọn)
        // Nếu bạn muốn Dash chỉ là di chuyển, có thể bỏ qua phần này.
        double shockDamage = baseDamage * 0.5; // Gây 50% sát thương gốc khi lướt qua

        for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
            if (entity instanceof LivingEntity victim && !entity.equals(player)) {

                // Đánh dấu sát thương kỹ năng để tránh vòng lặp
                player.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                victim.damage(shockDamage, player);

                // Hiệu ứng hạt khi va chạm
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);

                // Xóa metadata sau 1 tick
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) player.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 1L);
            }
        }
    }
}