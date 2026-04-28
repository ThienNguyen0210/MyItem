package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Explode implements IAbility {

    @Override
    public String getName() {
        return "EXPLODE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead() || target.hasMetadata("IS_ABILITY")) return;

        // 1. Tính toán sát thương vụ nổ: 8% + (level * 4%)
        double explodePercent = 8.0 + (level * 4.0);
        double explosionDamage = baseDamage * (explodePercent / 100.0);

        Location loc = target.getLocation();

        // 2. Tạo hiệu ứng vụ nổ vật lý (Không phá block để bảo vệ map)
        loc.getWorld().createExplosion(loc, 1.5f, false, false);

        // Hiệu ứng Visual
        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.1);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 25, 0.5, 0.5, 0.5, 0.15);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // 3. Quét sát thương diện rộng (AOE) và gây cháy
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity victim && !entity.equals(attacker)) {

                // Đánh dấu để tránh đệ quy
                victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                // Gây sát thương
                victim.damage(explosionDamage, attacker);

                // --- THÊM DÒNG NÀY ĐỂ GÂY CHÁY ---
                // 40 ticks = 2 giây cháy
                victim.setFireTicks(40);

                // Gỡ nhãn sau 1 tick
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 1L);
            }
        }
    }
}