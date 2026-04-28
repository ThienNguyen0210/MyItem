package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class SpiritWolf implements IAbility {

    @Override
    public String getName() {
        return "SPIRIT_WOLF";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        double multiplier = 0.25 + (Math.max(0, level - 1) * 0.10);
        double wolfDamage = baseDamage * multiplier;

        // Triệu hồi Sói
        Wolf wolf = (Wolf) attacker.getWorld().spawnEntity(attacker.getLocation(), EntityType.WOLF);

        // 1. Cấu hình Chỉ số
        if (wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(99999999.0);
            wolf.setHealth(99999999.0);
        }
        if (wolf.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            wolf.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(wolfDamage);
        }
        if (wolf.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            wolf.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.4);
        }
        // Tăng tầm nhìn/đuổi theo để sói không bị "ngáo" khi mục tiêu chạy xa
        if (wolf.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
            wolf.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(40.0);
        }

        // 2. Thiết lập trạng thái
        wolf.setTamed(true);
        wolf.setOwner(attacker);
        wolf.setCollarColor(DyeColor.CYAN);
        wolf.setAngry(true);
        wolf.setTarget(target);

        // Đảm bảo thực thể có AI hoạt động
        wolf.setAware(true);

        wolf.setCustomName(ChatColor.AQUA + "Sói Tâm Linh của " + attacker.getName());
        wolf.setCustomNameVisible(true);
        wolf.setMetadata("SPIRIT_WOLF", new FixedMetadataValue(Main.getInstance(), true));

        // Hiệu ứng triệu hồi
        wolf.getWorld().spawnParticle(Particle.CLOUD, wolf.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        wolf.getWorld().playSound(wolf.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.2f);

        // 3. Vòng lặp cưỡng ép và tự xóa
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 5;

                // Tự xóa sau 5 giây (100 ticks)
                if (ticks >= 100 || wolf.isDead() || !wolf.isValid()) {
                    if (wolf.isValid()) {
                        wolf.getWorld().spawnParticle(Particle.SMOKE_LARGE, wolf.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.05);
                        wolf.getWorld().playSound(wolf.getLocation(), Sound.ENTITY_WOLF_WHINE, 1.0f, 0.8f);
                        wolf.remove();
                    }
                    this.cancel();
                    return;
                }

                // Cập nhật mục tiêu
                if (target != null && target.isValid() && !target.isDead()) {
                    wolf.setTarget(target);

                    // Teleport nếu quá xa
                    if (wolf.getLocation().distance(target.getLocation()) > 12) {
                        wolf.teleport(target.getLocation().add(1, 0, 1));
                    }
                } else {
                    ticks = 100; // Kết thúc sớm nếu mục tiêu chết
                }

                // Hiệu ứng hạt
                wolf.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, wolf.getLocation().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0.01);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L);
    }
}