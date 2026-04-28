package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class LilacBloomBomb implements IAbility {

    @Override
    public String getName() {
        return "LILAC_BLOOM_BOMB";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        double multiplier = 0.50 + (Math.max(0, level - 1) * 0.15);
        double damageToDeal = baseDamage * multiplier;

        Location center = target.getLocation();
        List<Block> placedFlowers = new ArrayList<>();

        // 2. Trồng hoa Lilac (Bán kính 3 block)
        for (double t = 0; t < 2 * Math.PI; t += Math.PI / 6) {
            double x = 3.0 * Math.cos(t);
            double z = 3.0 * Math.sin(t);
            Location flowerLoc = center.clone().add(x, 0, z);
            Block block = flowerLoc.getBlock();

            if (block.getType() == Material.AIR) {
                block.setType(Material.LILAC);

                // --- QUAN TRỌNG: Gắn nhãn để Listener chặn phá block ---
                block.setMetadata("UNBREAKABLE_FLOWER", new FixedMetadataValue(Main.getInstance(), true));

                placedFlowers.add(block);
                flowerLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, flowerLoc.clone().add(0, 1, 0), 3, 0.2, 0.5, 0.2, 0.01);
            }
        }

        center.getWorld().playSound(center, Sound.BLOCK_AZALEA_PLACE, 1.0f, 1.2f);

        // 3. Sau 2 giây phát nổ
        new BukkitRunnable() {
            @Override
            public void run() {
                // Xóa hoa và gỡ nhãn Metadata
                for (Block b : placedFlowers) {
                    if (b.getType() == Material.LILAC) {
                        b.removeMetadata("UNBREAKABLE_FLOWER", Main.getInstance());
                        b.setType(Material.AIR);
                    }
                }

                if (!target.isValid() || target.isDead()) return;

                Location explosionLoc = target.getLocation().add(0, 1, 0);
                explosionLoc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, explosionLoc, 1);
                explosionLoc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, explosionLoc, 30, 1, 1, 1, 0.1);

                explosionLoc.getWorld().playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                explosionLoc.getWorld().playSound(explosionLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 0.8f);

                // Gây sát thương chống loop
                target.setNoDamageTicks(0);
                target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
                target.damage(damageToDeal, attacker);
                target.removeMetadata("IS_ABILITY", Main.getInstance());
            }
        }.runTaskLater(Main.getInstance(), 40L);
    }
}