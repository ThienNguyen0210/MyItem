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

import java.util.HashSet;
import java.util.Set;

public class WindTornado implements IAbility {

    @Override
    public String getName() {
        return "WIND_TORNADO";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        // 1. Tính toán sát thương: 10% + (level-1)*5%
        double multiplier = 0.10 + (Math.max(0, level - 1) * 0.05);
        double finalDamage = baseDamage * multiplier;

        Location startLoc = attacker.getLocation().add(0, 0.5, 0);
        Vector direction = startLoc.getDirection().normalize();

        // Hiệu ứng âm thanh khi khởi tạo lốc
        startLoc.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);

        new BukkitRunnable() {
            int distanceTraveled = 0;
            final int maxDistance = 15;
            final Set<Integer> hitEntities = new HashSet<>(); // Tránh gây sát thương nhiều lần trên 1 mục tiêu

            @Override
            public void run() {
                if (distanceTraveled >= maxDistance) {
                    this.cancel();
                    return;
                }

                // Di chuyển lốc tới 1 block
                startLoc.add(direction);

                // A. Hiệu ứng Visual: Lốc xoáy mini bằng hạt Cloud và Spell
                for (int i = 0; i < 5; i++) {
                    double angle = i * Math.PI / 2.5;
                    double x = Math.cos(angle) * 0.5;
                    double z = Math.sin(angle) * 0.5;
                    startLoc.getWorld().spawnParticle(Particle.CLOUD, startLoc.clone().add(x, i * 0.3, z), 1, 0, 0, 0, 0.02);
                }
                startLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, startLoc, 1, 0.1, 0.1, 0.1, 0);

                // B. Quét mục tiêu trong bán kính 1.5 block tại mỗi điểm lốc đi qua
                for (Entity entity : startLoc.getWorld().getNearbyEntities(startLoc, 1.5, 2.0, 1.5)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof ArmorStand)) {

                        if (!hitEntities.contains(victim.getEntityId())) {
                            hitEntities.add(victim.getEntityId());

                            // Hất tung mục tiêu (Vector Y dương)
                            victim.setVelocity(new Vector(0, 4.6, 0));

                            // Gây sát thương
                            victim.setNoDamageTicks(0);
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                            victim.damage(finalDamage, attacker);
                            victim.removeMetadata("IS_ABILITY", Main.getInstance());

                            // Hiệu ứng âm thanh khi trúng
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.2f);
                        }
                    }
                }

                distanceTraveled++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L); // Chạy mỗi 2 ticks để lốc mượt mà
    }
}