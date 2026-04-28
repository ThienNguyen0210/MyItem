package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Bubble implements IAbility {

    private final String METADATA_TASK = "BUBBLE_TASK";

    @Override
    public String getName() {
        return "BUBBLE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        // --- CHẶN VÒNG LẶP ---
        if (target.hasMetadata("IS_ABILITY")) return;

        // 1. Sát thương Metadata
        double percent = 3.0 + (level * 2.0);
        double extraDamage = baseDamage * (percent / 100.0);
        double currentExtra = target.hasMetadata("ABILITY_EXTRA_DAMAGE")
                ? target.getMetadata("ABILITY_EXTRA_DAMAGE").get(0).asDouble() : 0.0;
        target.setMetadata("ABILITY_EXTRA_DAMAGE", new FixedMetadataValue(Main.getInstance(), currentExtra + extraDamage));

        // 2. Xử lý Gia hạn (Hủy task cũ)
        if (target.hasMetadata(METADATA_TASK)) {
            Object value = target.getMetadata(METADATA_TASK).get(0).value();
            if (value instanceof BukkitRunnable oldTask) {
                oldTask.cancel();
            }
        }

        // 3. HẤT LÊN MẠNH (Để đạt độ cao ~5 block nhanh chóng)
        // Lực 1.5 là tầm 5-6 block
        target.setVelocity(new Vector(0, 1.5, 0));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.8f);

        // 4. Task xử lý lơ lửng
        int durationTicks = 40 + (level * 10);

        BukkitRunnable newTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Kiểm tra điều kiện dừng
                if (ticks >= durationTicks || target.isDead() || !target.isValid()) {
                    stopBubble(target);
                    this.cancel();
                    return;
                }

                // KHÓA VỊ TRÍ: Khi đã bay lên đủ cao (sau 8-10 ticks)
                if (ticks > 8) {
                    target.setGravity(false);
                    // Giữ vận tốc cực nhỏ để quái không bị giật lag (Zero velocity đôi khi gây bug di chuyển)
                    target.setVelocity(new Vector(0, 0.05, 0));
                }

                // Hiệu ứng hạt bong bóng
                Location loc = target.getLocation().add(0, 1, 0);
                target.getWorld().spawnParticle(Particle.WATER_BUBBLE, loc, 12, 0.4, 0.5, 0.4, 0.02);

                if (ticks % 5 == 0) {
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 0.5f, 1.2f);
                }

                ticks++;
            }
        };

        target.setMetadata(METADATA_TASK, new FixedMetadataValue(Main.getInstance(), newTask));

        // Chạy ngay lập tức (0L) để theo dõi mục tiêu từ lúc bắt đầu bay
        newTask.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void stopBubble(LivingEntity target) {
        if (target != null && target.isValid()) {
            target.setGravity(true);
            target.removeMetadata(METADATA_TASK, Main.getInstance());
            // Cho rơi xuống tự nhiên
        }
    }
}