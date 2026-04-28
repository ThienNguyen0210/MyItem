package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; // Cầu nối lấy stats xịn
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SongAm implements ISkill {
    @Override public String getName() { return "SongAm"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "HIT"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        // --- BƯỚC 1: LẤY SÁT THƯƠNG THỰC TỪ CACHE (Bỏ qua 1.2 của Event) ---
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = stats.totalBonusDmg;

        if (realPower <= 0) {
            realPower = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        // --- BƯỚC 2: TÍNH TOÁN FINAL DAMAGE (0.7 + 0.1 mỗi cấp) ---
        double skillMultiplier = 0.7 + (level * 0.1);
        double finalSkillDamage = realPower * skillMultiplier;

        double range = 15.0;
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        // Âm thanh đặc trưng của Warden
        player.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.0f);

        // 3. Thực hiện bắn tia Sóng Âm (Ray-tracing)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (double d = 0; d < range; d += 0.5) {
                    Location point = startLoc.clone().add(direction.clone().multiply(d));

                    // Hiệu ứng hạt Sonic Boom cực ngầu
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);

                    // Kiểm tra va chạm thực thể
                    for (Entity entity : point.getWorld().getNearbyEntities(point, 1.2, 1.2, 1.2)) {
                        if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand)) {

                            // --- QUY TRÌNH GÂY SÁT THƯƠNG CHUẨN ---
                            // 1. Phá bất tử để sóng âm xuyên thấu mọi lớp bảo vệ
                            victim.setNoDamageTicks(0);

                            // 2. Set Metadata lên NẠN NHÂN (Victim)
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                            // 3. Gây damage thực thi (Từ stats 100k)
                            victim.damage(finalSkillDamage, player);

                            // 4. Hiệu ứng va chạm
                            victim.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, victim.getLocation(), 1);
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

                            // 5. Dọn dẹp metadata sau 2 ticks
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (victim.isValid()) {
                                        victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                    }
                                }
                            }.runTaskLater(Main.getInstance(), 2L);

                            return; // Chỉ trúng 1 mục tiêu đầu tiên trên đường đi
                        }
                    }

                    if (point.getBlock().getType().isSolid()) break;
                }
            }
        }.runTask(Main.getInstance());
    }
}