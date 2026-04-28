package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class StarRitual implements IAbility {

    @Override
    public String getName() {
        return "STAR_RITUAL";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        // 1. Tính toán damage
        double multiplier = 0.60 + (Math.max(0, level - 1) * 0.07);
        double damageToDeal = baseDamage * multiplier;
        Location origin = attacker.getLocation();
        double radius = 4.0;

        // 2. Vẽ hiệu ứng ngôi sao
        drawStarEffect(origin, radius);

        // 3. LẤY DANH SÁCH MỤC TIÊU NGAY LẬP TỨC (Không để trong Runnable)
        // Quét bán kính 4 block, chiều cao 3 block (để bao quát hơn)
        List<Entity> nearbyEnemies = attacker.getNearbyEntities(radius, 3.0, radius);

        // 4. XỬ LÝ GÂY DAMAGE
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : nearbyEnemies) {
                    // Kiểm tra: Phải là thực thể sống, không phải bản thân người dùng, và không phải ArmorStand trang trí
                    if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                        // CHẶN VÒNG LẶP: Nếu victim đang bị IS_ABILITY thì bỏ qua để tránh StackOverflow
                        if (victim.hasMetadata("IS_ABILITY")) continue;

                        // Đánh dấu bắt đầu xử lý Ability
                        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                        // Gây damage (Gọi trực tiếp để kích hoạt hệ thống Stats/Armor của server)
                        victim.damage(damageToDeal, attacker);

                        // Hiệu ứng hạt nhỏ tại mỗi mục tiêu trúng đòn
                        victim.getWorld().spawnParticle(Particle.CRIT_MAGIC, victim.getLocation().add(0, 1, 0), 3);

                        // Xóa nhãn sau 1 tick để sẵn sàng cho lần nhận damage tiếp theo
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (victim.isValid()) {
                                    victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }
                        }.runTaskLater(Main.getInstance(), 1L);
                    }
                }
            }
        }.runTask(Main.getInstance());

        origin.getWorld().playSound(origin, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
    }

    private void drawStarEffect(Location origin, double radius) {
        List<Location> points = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-90 + (i * 72));
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            points.add(origin.clone().add(x, 0.2, z));
        }
        int[] sequence = {0, 2, 4, 1, 3, 0};
        for (int i = 0; i < sequence.length - 1; i++) {
            drawLine(points.get(sequence[i]), points.get(sequence[i+1]));
        }
    }

    private void drawLine(Location start, Location end) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.3) {
            Location loc = start.clone().add(direction.clone().multiply(d));
            loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f));
        }
    }
}