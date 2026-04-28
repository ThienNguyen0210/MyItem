package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; // Cầu nối lấy stats xịn
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ElectricFieldSkill implements ISkill {
    @Override public String getName() { return "ElectricField"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        // --- BƯỚC 1: LẤY SÁT THƯƠNG THỰC TỪ CACHE (Stats 100k) ---
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        // --- BƯỚC 2: TÍNH TOÁN FINAL DAMAGE (20% + 10% mỗi cấp) ---
        double finalDamage = realPower * (0.20 + (level * 0.10));

        Location center = player.getLocation().add(0, 1.0, 0);
        player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
        player.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 2.0f);

        // Lưu danh sách mục tiêu đã trúng để không bị dính damage nhiều lần khi sóng lan ra
        Set<UUID> affected = new HashSet<>();

        new BukkitRunnable() {
            double currentRadius = 0.5;
            final double maxRadius = 8.0;

            @Override
            public void run() {
                if (currentRadius > maxRadius || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // --- BƯỚC 3: VẼ HÌNH CẦU ĐIỆN TỬ (360 độ) ---
                // Sử dụng thuật toán phân phối điểm trên hình cầu
                int particles = (int) (currentRadius * 20);
                for (int i = 0; i < particles; i++) {
                    double phi = Math.acos(1 - 2 * Math.random());
                    double theta = 2 * Math.PI * Math.random();

                    double x = currentRadius * Math.sin(phi) * Math.cos(theta);
                    double y = currentRadius * Math.sin(phi) * Math.sin(theta);
                    double z = currentRadius * Math.cos(phi);

                    Location pLoc = center.clone().add(x, y, z);

                    // Hạt điện xanh neon
                    player.getWorld().spawnParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.AQUA, 1.5f));

                    if (currentRadius % 2 == 0) {
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }

                // --- BƯỚC 4: QUÉT MỤC TIÊU VÀ GÂY DAMAGE ---
                for (Entity entity : center.getWorld().getNearbyEntities(center, currentRadius, currentRadius, currentRadius)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand) && !affected.contains(victim.getUniqueId())) {

                        // Chỉ gây damage nếu quái nằm gần rìa sóng xung kích đang lan ra
                        if (victim.getLocation().distance(center) >= currentRadius - 1.5) {
                            affected.add(victim.getUniqueId());

                            // Phá bất tử & Set Metadata
                            victim.setNoDamageTicks(0);
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                            // Gây sát thương khủng
                            victim.damage(finalDamage, player);

                            // Hiệu ứng làm chậm (Tăng theo level)
                            // Level 1: Slow 1, Level 3: Slow 3...
                            int slowLevel = Math.max(0, level - 1);
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 3 * 20, slowLevel));

                            // Hiệu ứng tia sét nhỏ trên nạn nhân
                            victim.getWorld().spawnParticle(Particle.SCRAPE, victim.getEyeLocation(), 5, 0.2, 0.2, 0.2, 0.1);
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.8f);

                            // Dọn dẹp metadata
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }.runTaskLater(Main.getInstance(), 2L);
                        }
                    }
                }

                currentRadius += 1.0; // Tốc độ tỏa ra của điện trường
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}