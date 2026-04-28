package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlasmaOrb implements IAbility {

    @Override
    public String getName() {
        return "PLASMA_ORB";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        // 1. Tính toán sát thương: 8% + (level-1)*4%
        double damageMultiplier = 0.08 + (Math.max(0, level - 1) * 0.04);
        double plasmaDamage = baseDamage * damageMultiplier;

        // Vị trí bắt đầu: trước mặt người chơi 1 block, cao 1 block
        Location startLoc = attacker.getLocation().add(attacker.getLocation().getDirection().multiply(1)).add(0, 1, 0);
        Vector direction = attacker.getLocation().getDirection().normalize().multiply(0.15); // Tốc độ di chuyển chậm

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 160; // 5 giây
            final Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                // A. Di chuyển quả cầu plasma
                currentLoc.add(direction);

                // B. Visual: Quả cầu Plasma trung tâm (Kết hợp Electric Spark và Dust tím/xanh)
                currentLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentLoc, 5, 0.1, 0.1, 0.1, 0.05);
                currentLoc.getWorld().spawnParticle(Particle.REDSTONE, currentLoc, 10, 0.2, 0.2, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(138, 43, 226), 1.5f)); // Màu tím Plasma

                // C. Quét kẻ địch xung quanh bán kính 3 block
                for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 3, 3, 3)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof ArmorStand)) {

                        // Hiệu ứng tia sét điện giật (Tạo đường kẻ bằng Particle Dust từ tâm cầu đến victim)
                        drawElectricArc(currentLoc, victim.getLocation().add(0, 1, 0));

                        // Gây sát thương mỗi giây (20 ticks)
                        if (ticks % 20 == 0) {
                            applyPlasmaDamage(attacker, victim, plasmaDamage);
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
                        }
                    }
                }

                // Âm thanh rè rè của điện
                if (ticks % 10 == 0) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_BEEHIVE_WORK, 0.3f, 0.5f);
                }

                ticks += 2;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    /**
     * Vẽ tia điện nối giữa quả cầu và mục tiêu bằng Particle Dust
     */
    private void drawElectricArc(Location start, Location end) {
        Vector vec = end.toVector().subtract(start.toVector());
        double distance = start.distance(end);
        for (double i = 0; i < distance; i += 0.3) {
            Vector point = vec.clone().normalize().multiply(i);
            start.getWorld().spawnParticle(Particle.REDSTONE, start.clone().add(point), 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(0, 191, 255), 0.6f)); // Tia điện màu xanh nhạt
        }
    }

    private void applyPlasmaDamage(Player attacker, LivingEntity victim, double damage) {
        victim.setNoDamageTicks(0);
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        victim.damage(damage, attacker);
        victim.removeMetadata("IS_ABILITY", Main.getInstance());
    }
}