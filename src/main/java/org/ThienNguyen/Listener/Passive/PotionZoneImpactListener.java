package org.ThienNguyen.Listener.Passive;

import org.ThienNguyen.Listener.Passive.Mechanics.PotionZoneMechanic;
import org.ThienNguyen.Listener.Passive.Mechanics.PotionZoneMechanic.ZoneSpec;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;


/**
 * Khi potion được ném bởi {@link PotionZoneMechanic} (mechanic type
 * POTION_ZONE) chạm đất/entity, thay thế nó bằng một AreaEffectCloud với các
 * thông số đã cấu hình (bán kính, thời gian tồn tại, hiệu ứng, màu, particle).
 *
 * BẮT BUỘC: đăng ký listener này 1 lần trong Main#onEnable(), ví dụ:
 *   getServer().getPluginManager().registerEvents(new PotionZoneImpactListener(), this);
 */
public class PotionZoneImpactListener implements Listener {

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof ThrownPotion potion)) return;

        ZoneSpec spec = PotionZoneMechanic.consumePendingZone(potion.getUniqueId());
        if (spec == null) return;

        Location loc = potion.getLocation();
        if (loc.getWorld() == null) {
            potion.remove();
            return;
        }

        loc.getWorld().spawn(loc, AreaEffectCloud.class, aec -> {
            aec.setRadius((float) spec.radius);
            aec.setDuration(spec.durationTicks);
            aec.setReapplicationDelay(spec.reapplicationDelayTicks);
            if (spec.color != null)    aec.setColor(spec.color);
            if (spec.particle != null) aec.setParticle(spec.particle);
            if (potion.getShooter() instanceof Player shooter) aec.setSource(shooter);

            if (spec.effects != null) {
                for (PotionEffect effect : spec.effects) {
                    aec.addCustomEffect(effect, true);
                }
            }
        });

        potion.remove();
    }
}