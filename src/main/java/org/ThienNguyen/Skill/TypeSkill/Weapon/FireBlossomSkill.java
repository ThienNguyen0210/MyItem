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
import org.bukkit.scheduler.BukkitRunnable;

public class FireBlossomSkill implements ISkill {
    @Override public String getName() { return "FireBlossom"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        // --- BƯỚC 1: LẤY SÁT THƯƠNG THỰC TỪ CACHE (Bỏ qua 1.2 của Event) ---
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        // --- BƯỚC 2: TÍNH TOÁN SÁT THƯƠNG MỖI NHỊP (Pulse) ---
        // Công thức: 10% stats + 5% mỗi cấp
        final double damagePerPulse = realPower * (0.10 + (level * 0.05));

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.5f);

        // 3. Vòng lặp gây sát thương và hiệu ứng
        new BukkitRunnable() {
            int pulses = 0;
            double rotation = 0;

            @Override
            public void run() {
                if (pulses >= 25 || !player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }

                Location center = player.getLocation().add(0, 0.2, 0);

                // --- HIỆU ỨNG HÌNH ẢNH: BÔNG HOA LỬA XOAY ---
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    for (double r = 0.5; r <= 3.5; r += 0.5) { // Tăng nhẹ bán kính cho đẹp
                        double x = Math.cos(angle) * r;
                        double z = Math.sin(angle) * r;
                        Location partLoc = center.clone().add(x, 0, z);

                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, partLoc, 1, 0.05, 0.1, 0.05, 0.02);
                        if (r >= 2.5) {
                            player.getWorld().spawnParticle(Particle.FLAME, partLoc, 1, 0.02, 0.02, 0.02, 0.01);
                        }
                    }
                }

                // --- XỬ LÝ SÁT THƯƠNG CHUẨN ---
                for (Entity entity : player.getNearbyEntities(3.5, 2, 3.5)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                        // 1. PHÁ BẤT TỬ: Để mỗi nhịp hoa nở đều gây được sát thương
                        victim.setNoDamageTicks(0);

                        // 2. SET METADATA LÊN NẠN NHÂN (Victim)
                        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                        // 3. GÂY SÁT THƯƠNG THỰC TẾ (Tính từ 100k)
                        victim.damage(damagePerPulse, player);

                        victim.getWorld().spawnParticle(Particle.LAVA, victim.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.1);

                        // 4. DỌN DẸP METADATA SAU 2 TICKS
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (victim.isValid()) {
                                    victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }
                        }.runTaskLater(Main.getInstance(), 2L);
                    }
                }

                rotation += 15;
                pulses++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L);
    }
}