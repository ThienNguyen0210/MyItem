package org.ThienNguyen.Ability;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class ShadowDevour implements IAbility {

    @Override
    public String getName() {
        return "SHADOW_DEVOUR";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null) return;

        double durationSeconds = 2.0 + (level * 0.3);
        long durationTicks = (long) (durationSeconds * 20);

        // Lấy vị trí cố định của target lúc bị trúng chiêu
        final Location center = target.getLocation();
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.8f);

        new BukkitRunnable() {
            long elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks) {
                    this.cancel();
                    return;
                }

                // Hiệu ứng bụi đen bao quanh vị trí
                for (int i = 0; i < 12; i++) {
                    double x = (Math.random() - 0.5) * 4.0;
                    double y = Math.random() * 2.5;
                    double z = (Math.random() - 0.5) * 4.0;
                    center.getWorld().spawnParticle(Particle.REDSTONE, center.clone().add(x, y, z), 1, blackDust);
                }

                // Quét thực thể đi vào vùng
                if (elapsed % 10 == 0) {
                    center.getWorld().playSound(center, Sound.AMBIENT_CAVE, 0.5f, 0.5f);
                    for (Entity entity : center.getWorld().getNearbyEntities(center, 2.5, 2.5, 2.5)) {
                        if (entity instanceof LivingEntity victim && !victim.equals(attacker)) {
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false));
                        }
                    }
                }
                elapsed += 2;
            }
            // Thay 'YourPluginInstance' bằng instance của plugin bạn đang chạy
        }.runTaskTimer(JavaPlugin.getProvidingPlugin(getClass()), 0L, 2L);

    }
}