package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirShock implements IAbility {

    @Override
    public String getName() {
        return "AIR_SHOCK";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        // 1. Tính toán sát thương cộng thêm
        double percent = 5.0 + (Math.max(0, level - 1) * 3.0);
        double extraDamage = baseDamage * (percent / 100.0);

        // Lưu sát thương vào Metadata ngay lập tức
        double currentExtra = 0.0;
        if (target.hasMetadata("ABILITY_EXTRA_DAMAGE")) {
            currentExtra = target.getMetadata("ABILITY_EXTRA_DAMAGE").get(0).asDouble();
        }
        target.setMetadata("ABILITY_EXTRA_DAMAGE", new FixedMetadataValue(Main.getInstance(), currentExtra + extraDamage));

        // 2. THỰC THI VẬT LÝ (Delay 1 tick để không bị cái Knockback của kiếm chặn lại)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isDead() || !target.isValid()) return;

                // Công thức hất Y: 0.6 là cơ bản, level càng cao hất càng ác
                // Với level 100: 0.6 + (100 * 0.2) = 20.6 (Bay mất xác luôn)
                double yForce = 0.6 + (level * 0.2);

                // Giới hạn nhẹ để không lỗi Server nếu level quá ảo (vượt 500 chẳng hạn)
                if (yForce > 40.0) yForce = 40.0;

                // Lực đẩy ngang (lùi lại 1 chút)
                Vector pushBack = attacker.getLocation().getDirection().setY(0).normalize().multiply(0.3);

                // Tổng hợp vector
                Vector finalVelocity = new Vector(0, yForce, 0).add(pushBack);

                // Ghi đè hoàn toàn vận tốc
                target.setVelocity(finalVelocity);

                // Âm thanh bùng nổ khi cất cánh
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
            }
        }.runTaskLater(Main.getInstance(), 1L); // <--- CHÌA KHÓA: Trì hoãn 1 tick để hất tung ngay lập tức

        // 3. HIỆU ỨNG HẠT
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 5 || target.isDead()) {
                    this.cancel();
                    return;
                }
                // Hiệu ứng vòng tròn gió dưới chân
                Location loc = target.getLocation().add(0, 0.1, 0);
                target.getWorld().spawnParticle(Particle.CLOUD, loc, 5, 0.3, 0.1, 0.3, 0.05);
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 1L, 2L);
    }
}