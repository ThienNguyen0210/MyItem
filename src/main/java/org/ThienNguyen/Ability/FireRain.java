package org.ThienNguyen.Ability;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FireRain implements IAbility {

    @Override
    public String getName() {
        return "FIRE_RAIN";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null) return;

        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());

        // Chống chồng lặp kỹ năng trên cùng 1 mục tiêu
        if (target.hasMetadata("FIRERAIN_ACTIVE")) return;
        target.setMetadata("FIRERAIN_ACTIVE", new FixedMetadataValue(plugin, true));

        double damagePercent = 4.0 + (level * 2.0);
        double finalDamage = baseDamage * (damagePercent / 100.0);
        Random random = new Random();

        // Map lưu Location và Material ban đầu
        Map<Location, Material> originalBlocks = new HashMap<>();
        Location center = target.getLocation().clone();

        // 1. Biến đổi mặt đất (Bán kính 2, tỉ lệ 50%)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (random.nextDouble() < 0.5) {
                    Block block = center.clone().add(x, -1, z).getBlock();
                    // Chỉ biến các block đặc và không phải Magma sẵn có
                    if (block.getType().isSolid() && block.getType() != Material.MAGMA_BLOCK) {
                        // QUAN TRỌNG: Phải clone location để làm key trong Map
                        originalBlocks.put(block.getLocation().clone(), block.getType());
                        block.setType(Material.MAGMA_BLOCK);
                        // Đánh dấu để Listener không cho phá
                        block.setMetadata("UNBREAKABLE_MAGMA", new FixedMetadataValue(plugin, true));
                    }
                }
            }
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Kết thúc sau 2 giây hoặc khi mục tiêu chết
                if (ticks >= 40 || target.isDead()) {
                    // Trả lại block ban đầu
                    originalBlocks.forEach((loc, material) -> {
                        Block b = loc.getBlock();
                        b.setType(material);
                        b.removeMetadata("UNBREAKABLE_MAGMA", plugin);
                    });

                    target.removeMetadata("FIRERAIN_ACTIVE", plugin);
                    this.cancel();
                    return;
                }

                Location currentLoc = target.getLocation();

                // Hiệu ứng hạt (Lite version)
                for (int i = 0; i < 2; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 4;
                    double offsetZ = (random.nextDouble() - 0.5) * 4;
                    currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc.clone().add(offsetX, 3, offsetZ), 3, 0.1, 1, 0.1, 0.05);
                    if (ticks % 4 == 0) {
                        currentLoc.getWorld().spawnParticle(Particle.LAVA, currentLoc.clone().add(offsetX/2, 0.2, offsetZ/2), 1, 0.1, 0.1, 0.1, 0);
                    }
                }

                // Tìm đến đoạn này trong Runnable của FireRain
                if (ticks % 10 == 0) {
                    currentLoc.getWorld().playSound(currentLoc, Sound.BLOCK_LAVA_AMBIENT, 0.6f, 1.2f);
                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 3, 2, 3)) {
                        if (entity instanceof LivingEntity victim && !victim.equals(attacker)) {

                            // --- BẮT ĐẦU SỬA Ở ĐÂY ---
                            // 1. Đánh dấu mục tiêu đang dính sát thương từ Ability
                            victim.setMetadata("IS_ABILITY", new FixedMetadataValue(plugin, true));

                            // 2. Gây sát thương (Lúc này EventDamage sẽ thấy Metadata và return ngay)
                            victim.damage(finalDamage, attacker);

                            // 3. Xóa đánh dấu sau 1 tick (giống bên Bleed của bạn)
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (victim.isValid()) {
                                        victim.removeMetadata("IS_ABILITY", plugin);
                                    }
                                }
                            }.runTaskLater(plugin, 1L);
                            // --- KẾT THÚC SỬA ---

                        }
                    }
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}