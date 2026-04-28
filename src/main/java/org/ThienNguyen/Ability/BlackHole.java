package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class BlackHole implements IAbility {

    @Override
    public String getName() {
        return "BLACK_HOLE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        // 1. Tính toán thời gian: 3 giây (60 ticks) + (level-1) * 0.5 giây (10 ticks)
        int durationTicks = 60 + (Math.max(0, level - 1) * 10);
        double radius = 5.0;

        // Vị trí hố đen (tại vị trí kẻ địch bị đánh hoặc trước mặt người dùng)
        Location holeLoc = (target != null) ? target.getLocation().add(0, 1, 0) : attacker.getLocation().add(attacker.getLocation().getDirection().multiply(3)).add(0, 1, 0);

        // 2. Chạy hiệu ứng và lực hút
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    this.cancel();
                    return;
                }

                // A. Vẽ quả cầu bụi đen (Dust đen)
                drawBlackSphere(holeLoc);

                // B. Quét và hút Entity
                List<Entity> nearby = (List<Entity>) holeLoc.getWorld().getNearbyEntities(holeLoc, radius, radius, radius);
                for (Entity entity : nearby) {
                    if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                        // Tính Vector từ nạn nhân về tâm hố đen
                        Vector pull = holeLoc.toVector().subtract(victim.getLocation().toVector()).normalize().multiply(0.4);

                        // Áp dụng lực hút
                        victim.setVelocity(pull);

                        // Hiệu ứng hạt nhỏ bị hút vào
                        victim.getWorld().spawnParticle(Particle.SMOKE_NORMAL, victim.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0.01);
                    }
                }

                // Âm thanh hố đen gầm rú (mỗi 10 ticks)
                if (ticks % 10 == 0) {
                    holeLoc.getWorld().playSound(holeLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
                }

                ticks += 2; // Chạy mỗi 2 ticks để mượt mà và giảm lag
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    private void drawBlackSphere(Location loc) {
        // Vẽ các hạt dust đen xung quanh tâm để tạo thành quả cầu
        for (int i = 0; i < 8; i++) {
            Vector v = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).normalize().multiply(0.8);
            Location p = loc.clone().add(v);

            // Dust màu đen (Color.BLACK)
            loc.getWorld().spawnParticle(Particle.REDSTONE, p, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.BLACK, 1.5f));

            // Thêm hạt Portal để nhìn "hư không" hơn
            loc.getWorld().spawnParticle(Particle.PORTAL, p, 1, 0, 0, 0, 0.1);
        }
    }
}