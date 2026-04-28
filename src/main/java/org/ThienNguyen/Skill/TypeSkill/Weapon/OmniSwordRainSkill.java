package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OmniSwordRainSkill implements ISkill {
    @Override public String getName() { return "OmniSwordRain"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = (stats != null) ? stats.totalBonusDmg : player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        double damagePerSword = realPower * (0.10 + (level * 0.05));

        List<LivingEntity> targets = player.getNearbyEntities(10, 10, 10).stream()
                .filter(e -> e instanceof LivingEntity && !e.equals(player) && !(e instanceof ArmorStand))
                .map(e -> (LivingEntity) e)
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())))
                .limit(3)
                .collect(Collectors.toList());

        if (targets.isEmpty()) return;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 2.0f);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.1);

        new BukkitRunnable() {
            int count = 0;
            final int maxSwordsPerTarget = 10;

            @Override
            public void run() {
                if (count >= maxSwordsPerTarget || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                for (LivingEntity target : targets) {
                    if (target.isValid()) {
                        launchHomingSword(player, target, damagePerSword);
                    }
                }
                count++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L); // Tăng tốc độ bắn một chút (0.2s một đợt)
    }

    private void launchHomingSword(Player player, LivingEntity target, double damage) {
        // Vị trí xuất hiện ngẫu nhiên phía trên người chơi
        Location spawnLoc = player.getEyeLocation().add(
                (Math.random() - 0.5) * 5,
                2 + Math.random() * 2,
                (Math.random() - 0.5) * 5
        );

        ArmorStand as = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setGravity(false);
        as.setMarker(true);
        as.setArms(true);
        as.setSmall(true);
        as.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        as.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));

        new BukkitRunnable() {
            int life = 0;

            @Override
            public void run() {
                if (life > 50 || !target.isValid() || !as.isValid()) {
                    as.remove();
                    this.cancel();
                    return;
                }

                Vector dir = target.getEyeLocation().toVector().subtract(as.getLocation().toVector()).normalize();
                Location nextLoc = as.getLocation().add(dir.multiply(1.0)); // Tăng tốc độ bay cho mượt

                as.teleport(nextLoc.setDirection(dir));
                as.getWorld().spawnParticle(Particle.CRIT, as.getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);

                // Kiểm tra va chạm gần (khoảng cách 1.3 là đẹp nhất cho ArmorStand)
                if (as.getLocation().distanceSquared(target.getLocation().add(0, 1, 0)) < 1.8) {

                    // --- FIX DAMAGE CHIẾN THUẬT ---
                    target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                    // Reset bất tử ngay lập tức TRƯỚC khi gây sát thương
                    target.setNoDamageTicks(0);

                    // Gây sát thương trực tiếp
                    target.damage(damage, player);

                    // Sau khi damage xong, ép buộc reset lần nữa để cây kiếm sau có thể gây damage ngay
                    target.setNoDamageTicks(0);

                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.4f, 1.5f);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (target.isValid()) target.removeMetadata("IS_ABILITY", Main.getInstance());
                        }
                    }.runTaskLater(Main.getInstance(), 1L);

                    as.remove();
                    this.cancel();
                }
                life++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}