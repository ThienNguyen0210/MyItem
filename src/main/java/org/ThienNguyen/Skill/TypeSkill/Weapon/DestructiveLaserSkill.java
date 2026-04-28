package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; // Cầu nối lấy stats 100k
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DestructiveLaserSkill implements ISkill {
    @Override public String getName() { return "DestructiveLaser"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "LEFT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {

        // --- BƯỚC 1: LẤY SÁT THƯƠNG THỰC TỪ CACHE (Bỏ qua 1.2 của Event) ---
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        // --- BƯỚC 2: TÍNH TOÁN SÁT THƯƠNG MỖI NHỊP (TICK) ---
        // Sát thương mỗi nhịp nổ: 10% stats + 5% mỗi cấp
        double damagePerTick = realPower * (0.10 + (level * 0.05));

        double range = 15.0; // Tăng tầm xa cho laser bá đạo hơn
        int durationTicks = 3 * 20;
        long damageIntervalTicks = 4L;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.05);

        new BukkitRunnable() {
            private int ticksRun = 0;
            private final Set<UUID> damagedEntitiesThisTick = new HashSet<>();

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticksRun >= durationTicks) {
                    this.cancel();
                    return;
                }

                Location playerEyeLoc = player.getEyeLocation();
                Vector direction = playerEyeLoc.getDirection().normalize();

                boolean shouldDamage = (ticksRun % damageIntervalTicks == 0);
                if (shouldDamage) {
                    damagedEntitiesThisTick.clear();
                }

                // Vẽ tia laser
                for (double d = 0; d < range; d += 0.5) {
                    Location point = playerEyeLoc.clone().add(direction.clone().multiply(d));

                    // Hiệu ứng laser đỏ rực
                    player.getWorld().spawnParticle(Particle.REDSTONE, point, 2, 0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.RED, 1.2F));

                    // Thêm chút tia điện cho ngầu
                    if (ticksRun % 2 == 0) {
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.05, 0.05, 0.05, 0.01);
                    }

                    if (point.getBlock().getType().isSolid()) break; // Chạm tường thì dừng

                    if (shouldDamage) {
                        for (org.bukkit.entity.Entity entity : point.getWorld().getNearbyEntities(point, 1.2, 1.2, 1.2)) {
                            if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {
                                if (!damagedEntitiesThisTick.contains(victim.getUniqueId())) {

                                    // --- QUY TRÌNH GÂY SÁT THƯƠNG CHUẨN ---
                                    // 1. Phá bất tử (Để laser có thể đốt liên tục 4 ticks/lần)
                                    victim.setNoDamageTicks(0);

                                    // 2. Set Metadata lên NẠN NHÂN (Victim)
                                    victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                                    // 3. Gây damage thực tế (Con số từ 100k stats)
                                    victim.damage(damagePerTick, player);

                                    victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 1.8f);
                                    damagedEntitiesThisTick.add(victim.getUniqueId());

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
                    }
                }
                ticksRun++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}