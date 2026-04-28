package org.ThienNguyen.Ability;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DarkImpact implements IAbility {

    @Override
    public String getName() {
        return "DARK_IMPACT";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());

        // 1. Tính toán sát thương: 80% + (level-1) * 15%
        double damageMultiplier = 0.8 + ((level - 1) * 0.15);
        double finalDamage = baseDamage * damageMultiplier;

        // 2. Gây sát thương an toàn
        applySafeDamage(target, attacker, finalDamage, plugin);

        // 3. Xử lý đảo tầm nhìn (Chỉ áp dụng nếu mục tiêu là Người chơi)
        if (target instanceof Player victim) {
            Location loc = victim.getLocation();

            // Đảo hướng 180 độ: Yaw hiện tại + 180
            float newYaw = loc.getYaw() + 180f;
            loc.setYaw(newYaw);

            // Teleport để cập nhật góc nhìn ngay lập tức
            victim.teleport(loc);

            // Thêm hiệu ứng âm thanh và hạt tối tăm ngay tại đầu mục tiêu
            victim.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.5f);
            victim.getWorld().spawnParticle(Particle.SMOKE_LARGE, victim.getEyeLocation(), 15, 0.2, 0.2, 0.2, 0.05);
        }

        // 4. Hiệu ứng hình ảnh Impact (Vụ nổ bóng tối)
        Location targetLoc = target.getLocation().add(0, 1, 0);
        targetLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, targetLoc, 1);
        targetLoc.getWorld().spawnParticle(Particle.SQUID_INK, targetLoc, 20, 0.3, 0.3, 0.3, 0.1);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
    }

    private void applySafeDamage(LivingEntity victim, Player attacker, double damage, Plugin plugin) {
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(plugin, true));
        victim.damage(damage, attacker);
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