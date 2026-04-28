package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; // Cầu nối lấy stats
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
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HakiBaVuongSkill implements ISkill {
    @Override public String getName() { return "HakiBaVuong"; }
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

        // --- BƯỚC 2: TÍNH TOÁN FINAL DAMAGE (30% + 10% mỗi cấp) ---
        double finalSkillDamage = realPower * (0.30 + (level * 0.10));
        Location center = player.getLocation();

        // Âm thanh uy lực
        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.8f);

        Set<UUID> affected = new HashSet<>();

        new BukkitRunnable() {
            double currentRadius = 1.0;

            @Override
            public void run() {
                if (currentRadius > 10.0 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Vẽ vòng tròn Haki
                for (int i = 0; i < 360; i += 6) {
                    double angle = Math.toRadians(i);
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    Location particleLoc = center.clone().add(x, 0.5, z);

                    player.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 2, 0.1, 0.2, 0.1, 0,
                            new Particle.DustOptions(Color.WHITE, 2.0F));

                    if (i % 24 == 0) {
                        player.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0.1, 0, 0.02);
                    }
                }

                // Quét thực thể
                for (Entity entity : center.getWorld().getNearbyEntities(center, currentRadius, 3, currentRadius)) {
                    if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand) && !affected.contains(victim.getUniqueId())) {

                        if (victim.getLocation().distance(center) >= currentRadius - 1.8) {
                            affected.add(victim.getUniqueId());

                            // --- QUY TRÌNH GÂY SÁT THƯƠNG CHUẨN ---
                            // 1. Phá bất tử
                            victim.setNoDamageTicks(0);

                            // 2. Set Metadata lên NẠN NHÂN (Victim) để EventDamage bỏ qua
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                            // 3. Gây damage thực tế (Con số từ 100k stats)
                            victim.damage(finalSkillDamage, player);

                            // Hiệu ứng khống chế
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 255));
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
                            victim.setVelocity(new Vector(0, 0, 0));

                            victim.getWorld().spawnParticle(Particle.REDSTONE, victim.getEyeLocation().add(0, 0.5, 0), 15, 0.1, 0.3, 0.1, 0,
                                    new Particle.DustOptions(Color.WHITE, 1.5F));
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.5f);

                            // 4. Dọn dẹp metadata sau 2 ticks
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
                }
                currentRadius += 1.5;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}