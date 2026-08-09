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
    // Dedicated lock tag owned by this ability, set on activation and cleared only when the
    // effect actually ends. Replaces the old guard that piggybacked on "IS_ABILITY" — a flag
    // owned by EventDamage's per-event bookkeeping — so this ability's re-entrancy protection
    // no longer depends on, or requires touching, EventDamage's damage-processing logic.
    private static final String METADATA_LOCK = "ABILITY_LOCK_BUBBLE";

    @Override
    public String getName() {
        return "BUBBLE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        // While this tag is present, Bubble is already running on this target — bail out
        // instead of retriggering, which is what prevented recursive/duplicate activation.
        if (target.hasMetadata(METADATA_LOCK)) return;
        target.setMetadata(METADATA_LOCK, new FixedMetadataValue(Main.getInstance(), true));

        double percent = 3.0 + (level * 2.0);
        double extraDamage = baseDamage * (percent / 100.0);
        double currentExtra = target.hasMetadata("ABILITY_EXTRA_DAMAGE")
                ? target.getMetadata("ABILITY_EXTRA_DAMAGE").get(0).asDouble() : 0.0;
        target.setMetadata("ABILITY_EXTRA_DAMAGE", new FixedMetadataValue(Main.getInstance(), currentExtra + extraDamage));

        // Xóa toàn bộ momentum hiện tại để bắt đầu hiệu ứng mượt mà
        target.setVelocity(new Vector(0, 0, 0));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.8f);

        int durationTicks = 40 + (level * 10);

        BukkitRunnable newTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || target.isDead() || !target.isValid()) {
                    stopBubble(target);
                    this.cancel();
                    return;
                }

                // FIX: Dùng PotionEffectType.LEVITATION (cơ chế nổi có sẵn của vanilla) thay vì
                // Entity#teleport() mỗi tick. teleport() gửi một gói vị trí TUYỆT ĐỐI tới mọi
                // client xung quanh; spam gói này 20 lần/giây khiến client của NGƯỜI CHƠI KHÁC
                // không kịp nội suy hitbox để raycast/target trúng mục tiêu, nên đòn đánh của họ
                // không bao giờ đăng ký — trong khi mob tính damage hoàn toàn theo vị trí phía
                // server (không phụ thuộc render client) nên vẫn trúng bình thường. Đó chính là
                // nguyên nhân "bất tử với người chơi nhưng không bất tử với mob". Levitation dùng
                // đúng pipeline di chuyển gốc của vanilla nên replicate đúng cho mọi client và
                // không phá vỡ hit detection.
                int levitationLevel;
                if (ticks < 8) {
                    levitationLevel = 3; // Gia tốc ban đầu
                } else if (ticks < 20) {
                    levitationLevel = 1; // Giảm tốc dần
                } else {
                    levitationLevel = 0; // Lơ lửng nhẹ nhàng
                }
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 6, levitationLevel, false, false, false), true);

                // Hiệu ứng Particle
                Location particleLoc = target.getLocation().add(0, 1, 0);
                target.getWorld().spawnParticle(Particle.BUBBLE, particleLoc, 12, 0.4, 0.5, 0.4, 0.02);

                if (ticks % 5 == 0) {
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 0.5f, 1.2f);
                }

                ticks++;
            }
        };

        target.setMetadata(METADATA_TASK, new FixedMetadataValue(Main.getInstance(), newTask));
        newTask.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void stopBubble(LivingEntity target) {
        if (target != null && target.isValid()) {
            target.removePotionEffect(PotionEffectType.LEVITATION);
            // Reset momentum để entity rơi xuống tự nhiên khi Levitation kết thúc
            target.setVelocity(new Vector(0, 0, 0));
            target.removeMetadata(METADATA_TASK, Main.getInstance());
            target.removeMetadata(METADATA_LOCK, Main.getInstance());
        }
    }
}