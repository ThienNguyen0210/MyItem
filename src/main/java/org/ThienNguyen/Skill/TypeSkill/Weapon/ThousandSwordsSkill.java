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

import java.util.ArrayList;
import java.util.List;

public class ThousandSwordsSkill implements ISkill {
    @Override public String getName() { return "ThousandSwords"; }
    @Override public String getType() { return "Weapon"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity targetIgnored, int level, double baseDamageFromEvent) {
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(player.getUniqueId());
        double realPower = (stats != null) ? stats.totalBonusDmg : player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();

        double damagePerSword = realPower * (0.25 + (level * 0.05));

        // --- BƯỚC 1: LƯU TRỮ GÓC NHÌN NGAY LÚC CAST ---
        final Vector fixedDir = player.getEyeLocation().getDirection().normalize();

        List<ArmorStand> swords = new ArrayList<>();
        Location center = player.getLocation();

        // 2. Triệu hồi 8 cây Kiếm Sắt xoay sau lưng
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * (360.0 / 8));
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            Location swordLoc = center.clone().add(x, 1.5, z);

            ArmorStand as = (ArmorStand) center.getWorld().spawnEntity(swordLoc, EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setArms(true);
            as.setSmall(true);

            as.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            as.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));

            swords.add(as);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);

        // 3. Giai đoạn 2: Bắn kiếm theo hướng fixedDir đã lưu
        new BukkitRunnable() {
            int swordIndex = 0;

            @Override
            public void run() {
                if (swordIndex >= swords.size() || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                ArmorStand currentSword = swords.get(swordIndex);
                // Truyền fixedDir vào đây
                launchSword(player, currentSword, damagePerSword, fixedDir);

                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.8f, 1.2f);
                swordIndex++;
            }
        }.runTaskTimer(Main.getInstance(), 30L, 3L);
    }

    private void launchSword(Player player, ArmorStand as, double damage, Vector direction) {
        new BukkitRunnable() {
            int distance = 0;
            // Sử dụng direction được truyền vào từ lúc cast
            final Vector dir = direction.clone();
            Location currentPos = as.getLocation();

            @Override
            public void run() {
                if (distance > 25 || currentPos.getBlock().getType().isSolid()) {
                    as.remove();
                    this.cancel();
                    return;
                }

                currentPos.add(dir.clone().multiply(1.2));
                // Teleport kiếm theo hướng cố định
                as.teleport(currentPos.clone().setDirection(dir));

                currentPos.getWorld().spawnParticle(Particle.SWEEP_ATTACK, currentPos, 1, 0, 0, 0, 0);
                currentPos.getWorld().spawnParticle(Particle.CRIT, currentPos, 2, 0.1, 0.1, 0.1, 0.05);

                for (org.bukkit.entity.Entity e : currentPos.getWorld().getNearbyEntities(currentPos, 1.5, 1.5, 1.5)) {
                    if (e instanceof LivingEntity victim && !e.equals(player) && !(e instanceof ArmorStand)) {
                        victim.setNoDamageTicks(0);
                        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                        victim.damage(damage, player);

                        victim.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, victim.getLocation(), 1);
                        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);

                        new BukkitRunnable() {
                            @Override
                            public void run() { if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance()); }
                        }.runTaskLater(Main.getInstance(), 2L);

                        as.remove();
                        this.cancel();
                        return;
                    }
                }
                distance++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}