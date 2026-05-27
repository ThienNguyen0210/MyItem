package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class Bleed implements IAbility {

    private final String METADATA_TASK = "BLEED_TASK";
    private final String METADATA_IS_BLEEDING = "IS_BLEEDING";

    @Override
    public String getName() {
        return "BLEED";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        
        
        if (target.hasMetadata("IS_ABILITY")) return;

        double tickPercent = 1.0 + (level * 0.5);
        double damagePerTick = baseDamage * (tickPercent / 100.0);

        
        if (target.hasMetadata(METADATA_IS_BLEEDING)) {
            if (target.hasMetadata(METADATA_TASK)) {
                Object old = target.getMetadata(METADATA_TASK).get(0).value();
                if (old instanceof BukkitRunnable task) {
                    task.cancel();
                }
            }
            startBleedTask(attacker, target, damagePerTick);
            return;
        }

        
        target.setMetadata(METADATA_IS_BLEEDING, new FixedMetadataValue(Main.getInstance(), true));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 1.0f, 0.8f);

        startBleedTask(attacker, target, damagePerTick);
    }

    private void startBleedTask(Player attacker, LivingEntity target, double damagePerTick) {
        BukkitRunnable bleedTask = new BukkitRunnable() {
            int count = 0;
            final int maxCount = 3;

            @Override
            public void run() {
                if (target == null || target.isDead() || count >= maxCount) {
                    cleanup(target);
                    this.cancel();
                    return;
                }

                
                target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

                
                target.damage(damagePerTick, attacker);

                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (target.isValid()) target.removeMetadata("IS_ABILITY", Main.getInstance());
                    }
                }.runTaskLater(Main.getInstance(), 1L);

                
                target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 1, 0), 10, 0.1, 0.2, 0.1,
                        Material.REDSTONE_BLOCK.createBlockData());
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 0.5f, 0.8f);

                count++;
            }
        };

        target.setMetadata(METADATA_TASK, new FixedMetadataValue(Main.getInstance(), bleedTask));
        bleedTask.runTaskTimer(Main.getInstance(), 20L, 20L);
    }

    private void cleanup(LivingEntity target) {
        if (target != null) {
            target.removeMetadata(METADATA_TASK, Main.getInstance());
            target.removeMetadata(METADATA_IS_BLEEDING, Main.getInstance());
            target.removeMetadata("IS_ABILITY", Main.getInstance());
        }
    }
}