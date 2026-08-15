package org.ThienNguyen.Listener.Passive;

import org.ThienNguyen.Listener.Passive.Mechanics.ProjectileShotMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;


/**
 * Áp dụng damage tùy chỉnh cho các projectile được bắn ra bởi
 * {@link ProjectileShotMechanic} (mechanic type PROJECTILE_SHOT).
 *
 * Projectile được gắn metadata {@link ProjectileShotMechanic#DAMAGE_METADATA_KEY}
 * lúc bắn ra; khi trúng một LivingEntity, listener này đọc metadata và gọi
 * damage() với giá trị đã cấu hình.
 *
 * BẮT BUỘC: đăng ký listener này 1 lần trong Main#onEnable(), ví dụ:
 *   getServer().getPluginManager().registerEvents(new ProjectileShotDamageListener(), this);
 */
public class ProjectileShotDamageListener implements Listener {

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        if (!proj.hasMetadata(ProjectileShotMechanic.DAMAGE_METADATA_KEY)) return;

        if (!(e.getHitEntity() instanceof LivingEntity victim)) return;

        List<MetadataValue> values = proj.getMetadata(ProjectileShotMechanic.DAMAGE_METADATA_KEY);
        if (values.isEmpty()) return;

        double damage = values.get(0).asDouble();
        if (damage <= 0) return;

        if (proj.getShooter() instanceof Player shooter) {
            victim.damage(damage, shooter);
        } else {
            victim.damage(damage);
        }
    }
}