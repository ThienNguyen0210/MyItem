package org.ThienNguyen.Ability;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Angel implements IAbility {

    @Override
    public String getName() {
        return "ANGEL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        // Trong hệ thống của bạn, khi trigger là SELF, attacker chính là người mang đồ
        if (!(attacker instanceof Player player)) return;

        // 1. Tính toán lượng máu hồi: 5% + (level * 2%)
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double healPercent = 5.0 + (level * 2.0);
        double healAmount = maxHealth * (healPercent / 100.0);

        // 2. Thực hiện hồi máu (không vượt quá máu tối đa)
        double currentHealth = player.getHealth();
        player.setHealth(Math.min(maxHealth, currentHealth + healAmount));

        // 3. Hiệu ứng Particle (Village Happy)
        player.getWorld().spawnParticle(
                Particle.VILLAGER_HAPPY,
                player.getLocation().add(0, 1, 0),
                15,
                0.4, 0.5, 0.4,
                0.05
        );

        // 4. Âm thanh
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

        // 5. Gửi thông báo vào khung chat thay vì ActionBar
    }
}