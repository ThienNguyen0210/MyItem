package org.ThienNguyen.Skill.TypeSkill.Weapon;

import org.ThienNguyen.Skill.ISkill;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.PlayerCombatCache; // Cầu nối lấy stats 100k
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SoulReleaseSkill implements ISkill {
    @Override public String getName() { return "SoulRelease"; }
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

        // TÍNH TOÁN FINAL DAMAGE (60% + 10% mỗi cấp)
        final double explosionDmg = realPower * (0.60 + (level * 0.10));

        Location oldLocation = player.getLocation().clone();

        // 1. Dash lướt đi
        Vector dir = player.getLocation().getDirection().setY(0.1).normalize();
        player.setVelocity(dir.multiply(1.5));

        // 2. Tạo Thân xác (ArmorStand)
        ArmorStand body = (ArmorStand) oldLocation.getWorld().spawnEntity(oldLocation, EntityType.ARMOR_STAND);
        body.setCustomName("§bLinh ảnh của §f" + player.getName());
        body.setCustomNameVisible(true);
        body.setGravity(false);
        body.setBasePlate(false);
        body.setArms(true);
        body.setInvulnerable(true);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            body.addDisabledSlots(slot);
        }

        body.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        body.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        body.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        body.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
        body.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand().clone());

        // 3. Tăng tốc chạy cho "Linh hồn"
        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "SoulReleaseSpeed", 0.05, AttributeModifier.Operation.ADD_NUMBER);
        if (speedAttr != null) speedAttr.addModifier(modifier);

        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.5f);

        // 4. Task đếm ngược
        new BukkitRunnable() {
            int seconds = 5;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || seconds <= 0) {

                    Location currentLoc = player.getLocation();

                    // Hiệu ứng nổ linh hồn cực mạnh
                    currentLoc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, currentLoc, 2);
                    currentLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 50, 1.5, 1.5, 1.5, 0.1);
                    currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

                    // --- QUY TRÌNH GÂY SÁT THƯƠNG DIỆN RỘNG ---
                    for (org.bukkit.entity.Entity e : player.getNearbyEntities(4, 4, 4)) {
                        if (e instanceof LivingEntity victim && !e.equals(player) && !(e instanceof ArmorStand)) {

                            // 1. Phá bất tử
                            victim.setNoDamageTicks(0);

                            // 2. Set Metadata lên NẠN NHÂN
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                            // 3. Gây damage thực thi
                            victim.damage(explosionDmg, player);

                            // 4. Dọn dẹp metadata sau 2 ticks
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (victim.isValid()) victim.removeMetadata("IS_ABILITY", Main.getInstance());
                                }
                            }.runTaskLater(Main.getInstance(), 2L);
                        }
                    }

                    // Nhập xác: Biến về vị trí Armor Stand
                    player.teleport(oldLocation);
                    body.remove();

                    if (speedAttr != null) speedAttr.removeModifier(modifier);

                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

                    this.cancel();
                    return;
                }

                // Hiệu ứng hạt linh hồn
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
                seconds--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}