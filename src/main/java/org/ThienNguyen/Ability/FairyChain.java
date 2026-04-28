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

public class FairyChain implements IAbility {

    @Override
    public String getName() {
        return "FAIRY_CHAIN";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null) return;

        // 1. Chỉ số
        double radius = 4.0;
        double damageToDeal = baseDamage * (0.10 + (Math.max(0, level - 1) * 0.04));

        // 2. Quét mục tiêu trong bán kính 4 block
        List<Entity> nearby = attacker.getNearbyEntities(radius, 3.0, radius);

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity victim && !entity.equals(attacker) && !(entity instanceof org.bukkit.entity.ArmorStand)) {

                // Chạy Runnable xử lý sợi xích cho từng mục tiêu
                new BukkitRunnable() {
                    int ticks = 0;
                    boolean isBroken = false;

                    @Override
                    public void run() {
                        // Kiểm tra nếu mục tiêu chết hoặc biến mất
                        if (!victim.isValid() || victim.isDead() || !attacker.isOnline()) {
                            this.cancel();
                            return;
                        }

                        // Kiểm tra khoảng cách: Nếu xa quá 4 block thì đứt xích
                        double distance = attacker.getLocation().distance(victim.getLocation());
                        if (distance > 4.5) { // Cho sai số 0.5 để mượt hơn
                            isBroken = true;
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.5f);
                            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                            this.cancel();
                            return;
                        }

                        // Vẽ sợi xích Dust (Màu hồng Fairy)
                        drawChain(attacker.getLocation().add(0, 1, 0), victim.getLocation().add(0, 1, 0));

                        // Hiệu ứng âm thanh xích leng keng
                        if (ticks % 10 == 0) {
                            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.8f);
                        }

                        // Sau 3 giây (60 ticks) - Kích nổ sát thương và làm chậm
                        if (ticks >= 60) {
                            // Gây sát thương chống loop
                            victim.setNoDamageTicks(0);
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                            victim.damage(damageToDeal, attacker);
                            victim.removeMetadata("IS_ABILITY", Main.getInstance());

                            // Làm chậm level 100 (bất động hoàn toàn) trong 3 giây
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 100));

                            // Hiệu ứng hạt nổ
                            victim.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, victim.getLocation().add(0, 1, 0), 1);
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);

                            this.cancel();
                            return;
                        }

                        ticks += 2;
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 2L);
            }
        }
    }

    private void drawChain(Location start, Location end) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double d = 0; d < distance; d += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(d));
            // Dust màu hồng nhạt hệ Tiên
            point.getWorld().spawnParticle(Particle.REDSTONE, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 150, 200), 1.0f));
            // Thêm hạt Glow nhỏ cho lấp lánh
            if (d % 1.2 == 0) {
                point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0.01);
            }
        }
    }
}