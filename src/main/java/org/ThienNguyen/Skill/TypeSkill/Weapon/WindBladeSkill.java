package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WindBladeSkill implements ISkill {
    @Override public String getName() { return "WindBlade"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = (stats != null) ? stats.totalBonusDmg : player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();

        double finalDamage = realPower * (0.60 + (level * 0.15));

        // Đẩy vị trí bắt đầu ra phía trước 1 block để tránh kẹt vào người chơi hoặc block chân
        Location startLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.0));
        Vector direction = player.getLocation().getDirection().normalize();

        player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.2f);
        player.getWorld().playSound(startLoc, Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1.0f, 1.5f);

        Set<UUID> affected = new HashSet<>();

        new BukkitRunnable() {
            double distance = 0;
            final double maxRange = 15.0;

            @Override
            public void run() {
                if (distance >= maxRange || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Tăng tốc độ bay (kiếm khí bay nhanh nhìn mới phê)
                for (int i = 0; i < 3; i++) {
                    distance += 0.4;
                    Location currentLoc = startLoc.clone().add(direction.clone().multiply(distance));

                    // Vẽ hiệu ứng (Đã thay hạt dễ nhìn hơn)
                    drawWindEffect(currentLoc, direction);

                    // Kiểm tra block trước khi quét quái
                    if (currentLoc.getBlock().getType().isSolid()) {
                        currentLoc.getWorld().spawnParticle(Particle.CLOUD, currentLoc, 5, 0.1, 0.1, 0.1, 0.05);
                        this.cancel();
                        return;
                    }

                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                        if (entity instanceof LivingEntity victim && !entity.equals(player) && !(entity instanceof ArmorStand) && !affected.contains(victim.getUniqueId())) {
                            affected.add(victim.getUniqueId());

                            victim.setNoDamageTicks(0);
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                            victim.damage(finalDamage, player);

                            Vector knockup = direction.clone().multiply(0.3).setY(0.6);
                            victim.setVelocity(knockup);

                            victim.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, victim.getLocation(), 5, 0.2, 0.2, 0.2, 0.1);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }.runTaskLater(Main.getInstance(), 2L);
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void drawWindEffect(Location loc, Vector dir) {
        // Tạo vector vuông góc để vẽ lưỡi kiếm dọc
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        // Vẽ lưỡi kiếm bằng hạt lửa pháo hoa (màu trắng sáng) và khói
        for (double i = -0.8; i <= 0.8; i += 0.4) {
            Location pLoc = loc.clone().add(right.clone().multiply(i * 0.3)).add(0, i, 0);

            // Hạt FIREWORKS_SPARK cực kỳ dễ nhìn thấy
            loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, pLoc, 1, 0.02, 0.02, 0.02, 0.01);
            loc.getWorld().spawnParticle(Particle.CLOUD, pLoc, 1, 0, 0, 0, 0.01);
        }
    }
}