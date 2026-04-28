package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Disarm implements IAbility {

    public static final String METADATA_DISARM = "DISARMED_STATUS";

    @Override
    public String getName() {
        return "DISARM";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead() || target.hasMetadata("IS_ABILITY")) return;

        // 1. Tính thời gian tước vũ khí: 2s (40 ticks) + 0.5s (10 ticks) mỗi level
        int durationTicks = 40 + (Math.max(0, level - 1) * 10);

        // 2. Đánh dấu mục tiêu bị Disarm
        target.setMetadata(METADATA_DISARM, new FixedMetadataValue(Main.getInstance(), true));

        // 3. Hiệu ứng Visual (Hạt sắt vụn và âm thanh rớt đồ)
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.5, 0), 20, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2.0f);

        // Hiển thị thông báo cho mục tiêu nếu là người chơi
        if (target instanceof Player victim) {
            victim.sendActionBar("§c§l✖ BẠN ĐÃ BỊ TƯỚC VŨ KHÍ!");
        }

        // 4. Task gỡ bỏ trạng thái sau thời gian quy định
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.hasMetadata(METADATA_DISARM)) {
                    target.removeMetadata(METADATA_DISARM, Main.getInstance());
                    if (target instanceof Player victim) {
                        victim.sendActionBar("§a§l✔ Bạn đã có thể tấn công lại!");
                    }
                }
            }
        }.runTaskLater(Main.getInstance(), durationTicks);
    }
}