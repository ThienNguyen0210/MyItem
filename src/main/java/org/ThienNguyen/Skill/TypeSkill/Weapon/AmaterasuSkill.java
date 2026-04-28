package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; // Import cache của bạn
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.attribute.Attribute;

public class AmaterasuSkill implements ISkill {
    @Override public String getName() { return "Amaterasu"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        // 1. Tìm mục tiêu thực tế bằng cách nhìn
        LivingEntity target = getTargetEntity(player, 15);

        if (target == null || target.isDead()) {
            return;
        }

        // 2. TỰ LẤY SÁT THƯƠNG TỪ CACHE (Bỏ qua baseDamageFromEvent bị lỗi 1.2)
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        // Tính sát thương đốt mỗi giây: 25% + 10% mỗi cấp dựa trên stats 100k
        double damagePerSecond = realPower * (0.25 + (level * 0.10));

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);

        // 3. Vòng lặp lửa đen (5 giây = 100 ticks)
        new BukkitRunnable() {
            private int ticks = 0;
            @Override
            public void run() {
                // Kiểm tra mục tiêu còn hợp lệ không
                if (target == null || target.isDead() || !target.isValid() || ticks >= 100) {
                    this.cancel();
                    return;
                }

                // Hiệu ứng hạt lửa đen (Sử dụng REDSTONE/DUST với màu đen)
                Location loc = target.getLocation().add(0, 1, 0);
                target.getWorld().spawnParticle(Particle.REDSTONE, loc, 15, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.BLACK, 1.5F));
                target.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 3, 0.2, 0.4, 0.2, 0.02);

                // Gây sát thương mỗi giây (mỗi 20 ticks)
                if (ticks % 20 == 0) {

                    // --- QUY TRÌNH GÂY DAMAGE CHUẨN ---
                    // 1. Phá bất tử để không bị cản bởi đòn đánh thường
                    target.setNoDamageTicks(0);

                    // 2. Set Metadata lên NẠN NHÂN để EventDamage bỏ qua
                    target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                    // 3. Gây damage
                    target.damage(damagePerSecond, player);

                    // 4. Xóa metadata sau 2 ticks để an toàn
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (target.isValid()) {
                                target.removeMetadata("IS_ABILITY", Main.getInstance());
                            }
                        }
                    }.runTaskLater(Main.getInstance(), 2L);

                    // Hiệu ứng âm thanh đốt cháy
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    // Hàm hỗ trợ tìm quái trong tầm nhìn
    private LivingEntity getTargetEntity(Player player, int range) {
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity target && player.hasLineOfSight(e)) {
                // Kiểm tra xem mục tiêu có nằm trong góc nhìn hẹp phía trước không
                Vector toTarget = target.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
                double dot = toTarget.dot(player.getEyeLocation().getDirection());

                if (dot > 0.98) { // Góc nhìn rất hẹp để nhắm chuẩn
                    return target;
                }
            }
        }
        return null;
    }
}