package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class FirstStrike implements IAbility {

    @Override
    public String getName() {
        return "FIRST_STRIKE";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (attacker == null || target == null || target.isDead()) return;

        
        double multiplier = 0.40 + (Math.max(0, level - 1) * 0.20);
        double extraDamage = baseDamage * multiplier;

        
        
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 60, 0));

        
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 99)); 

        
        applyAbilityDamage(attacker, target, extraDamage);

        
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.5f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2.0f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20 || target.isDead() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                
                Location headLoc = target.getLocation().add(0, target.getHeight() + 0.3, 0);
                double angle = ticks * 0.8;
                double x = Math.cos(angle) * 0.3;
                double z = Math.sin(angle) * 0.3;

                
                target.getWorld().spawnParticle(Particle.CRIT, headLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                if (ticks % 5 == 0) {
                    target.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, headLoc, 1, 0.1, 0.1, 0.1, 0);
                }

                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void applyAbilityDamage(Player attacker, LivingEntity victim, double damage) {
        victim.setNoDamageTicks(0);
        victim.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        victim.damage(damage, attacker);
        victim.removeMetadata("IS_ABILITY", Main.getInstance());
    }
}