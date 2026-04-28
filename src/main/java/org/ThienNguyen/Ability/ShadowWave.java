package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class ShadowWave implements IAbility {

    @Override
    public String getName() {
        return "SHADOW_WAVE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        // 1. Tính toán thời gian hiệu ứng: 3 giây (60 ticks) + (level-1)*1s (20 ticks)
        int effectDuration = 60 + (Math.max(0, level - 1) * 20);
        double radius = 10.0;

        Location center = attacker.getLocation();

        // 2. Phát âm thanh Wither Spawn cực mạnh
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);

        // 3. Hiệu ứng làn sóng bóng tối lan tỏa
        new BukkitRunnable() {
            double r = 1.0;
            @Override
            public void run() {
                if (r > radius) {
                    this.cancel();
                    return;
                }

                // Vẽ vòng tròn bóng tối tại bán kính r hiện tại
                for (int i = 0; i < 40; i++) {
                    double angle = i * 2 * Math.PI / 40;
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    Location pLoc = center.clone().add(x, 0.2, z);

                    // Hạt khói đen đậm
                    pLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, pLoc, 1, 0, 0, 0, 0.02);
                    // Hạt Dust đen
                    pLoc.getWorld().spawnParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.BLACK, 1.5f));
                }
                r += 1.5; // Tốc độ lan tỏa của sóng
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        // 4. Quét và áp dụng hiệu ứng cho các mục tiêu trong bán kính 10
        List<Entity> targets = attacker.getNearbyEntities(radius, 5.0, radius);
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                // A. Add hiệu ứng Darkness (Bóng tối)
                victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, effectDuration, 0));

                // B. Add hiệu ứng Wither I (Khô héo)
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, effectDuration, 0));

                // C. Gây một chút sát thương tức thời (Chống loop)
                // Giả sử gây 5% base damage ngay lập tức
                double instantDmg = baseDamage * 0.05;
                victim.setNoDamageTicks(0);
                victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                victim.damage(instantDmg, attacker);
                victim.removeMetadata("IS_ABILITY", Main.getInstance());

                // Hiệu ứng hạt tại nạn nhân
                victim.getWorld().spawnParticle(Particle.SQUID_INK, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }
}