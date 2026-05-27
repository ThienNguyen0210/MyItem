package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Bubble implements IAbility {

    private final String METADATA_TASK = "BUBBLE_TASK";

    @Override
    public String getName() {
        return "BUBBLE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        
        if (target.hasMetadata("IS_ABILITY")) return;

        
        double percent = 3.0 + (level * 2.0);
        double extraDamage = baseDamage * (percent / 100.0);
        double currentExtra = target.hasMetadata("ABILITY_EXTRA_DAMAGE")
                ? target.getMetadata("ABILITY_EXTRA_DAMAGE").get(0).asDouble() : 0.0;
        target.setMetadata("ABILITY_EXTRA_DAMAGE", new FixedMetadataValue(Main.getInstance(), currentExtra + extraDamage));

        
        if (target.hasMetadata(METADATA_TASK)) {
            Object value = target.getMetadata(METADATA_TASK).get(0).value();
            if (value instanceof BukkitRunnable oldTask) {
                oldTask.cancel();
            }
        }

        
        
        target.setVelocity(new Vector(0, 1.5, 0));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.8f);

        
        int durationTicks = 40 + (level * 10);

        BukkitRunnable newTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                
                if (ticks >= durationTicks || target.isDead() || !target.isValid()) {
                    stopBubble(target);
                    this.cancel();
                    return;
                }

                
                if (ticks > 8) {
                    target.setGravity(false);
                    
                    target.setVelocity(new Vector(0, 0.05, 0));
                }

                
                Location loc = target.getLocation().add(0, 1, 0);
                target.getWorld().spawnParticle(Particle.WATER_BUBBLE, loc, 12, 0.4, 0.5, 0.4, 0.02);

                if (ticks % 5 == 0) {
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 0.5f, 1.2f);
                }

                ticks++;
            }
        };

        target.setMetadata(METADATA_TASK, new FixedMetadataValue(Main.getInstance(), newTask));

        
        newTask.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void stopBubble(LivingEntity target) {
        if (target != null && target.isValid()) {
            target.setGravity(true);
            target.removeMetadata(METADATA_TASK, Main.getInstance());
            
        }
    }
}