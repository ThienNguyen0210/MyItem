package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Poison implements IAbility {
    @Override
    public String getName() {
        return "POISON";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        // Thời gian tác dụng: level * 2 giây (20 ticks = 1 giây)
        int duration = level * 40;

        // Cấp độ thuốc độc: level 1 là POISON I, level 2 là POISON II...
        // (Lưu ý: Amplifier 0 là Level I trong game)
        int amplifier = Math.max(0, level - 1);

        // Áp dụng hiệu ứng Poison
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration, amplifier));

        // Bạn có thể thêm hiệu ứng âm thanh hoặc hạt nếu muốn
        // target.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, target.getLocation().add(0, 1, 0), 10);
    }
}