package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Curse implements IAbility {

    // Tên Metadata dùng chung để EventDamage nhận diện
    public static final String METADATA_REDUCE = "CURSED_REDUCTION";
    private final String METADATA_TASK = "CURSE_TASK";

    @Override
    public String getName() {
        return "CURSE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        // --- 1. Sát thương tức thời ---
        double percent = 5.0 + (Math.max(0, level - 1) * 3.0);
        double extraDamage = baseDamage * (percent / 100.0);

        double currentExtra = 0.0;
        if (target.hasMetadata("ABILITY_EXTRA_DAMAGE")) {
            currentExtra = target.getMetadata("ABILITY_EXTRA_DAMAGE").get(0).asDouble();
        }
        target.setMetadata("ABILITY_EXTRA_DAMAGE", new FixedMetadataValue(Main.getInstance(), currentExtra + extraDamage));

        // --- 2. Xử lý Lời nguyền (Debuff giảm damage) ---
        // Công thức của ThienNguyen: 5% mặc định + 3% mỗi cấp
        double reducePercent = 5.0 + (level * 3.0);

        // Cập nhật Metadata giá trị giảm
        target.setMetadata(METADATA_REDUCE, new FixedMetadataValue(Main.getInstance(), reducePercent));

        // --- 3. Quản lý Task (Để gia hạn lời nguyền nếu đánh liên tục) ---
        if (target.hasMetadata(METADATA_TASK)) {
            Object old = target.getMetadata(METADATA_TASK).get(0).value();
            if (old instanceof BukkitRunnable task) {
                task.cancel(); // Hủy đếm ngược cũ để bắt đầu 3 giây mới
            }
        }

        BukkitRunnable removeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isValid()) {
                    target.removeMetadata(METADATA_REDUCE, Main.getInstance());
                    target.removeMetadata(METADATA_TASK, Main.getInstance());
                }
            }
        };

        // Lưu task vào metadata và chạy (3 giây = 60 ticks)
        target.setMetadata(METADATA_TASK, new FixedMetadataValue(Main.getInstance(), removeTask));
        removeTask.runTaskLater(Main.getInstance(), 60L);

        // --- 4. Hiệu ứng ---
        target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 1.2, 0), 25, 0.3, 0.5, 0.3, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.2f);
    }
}