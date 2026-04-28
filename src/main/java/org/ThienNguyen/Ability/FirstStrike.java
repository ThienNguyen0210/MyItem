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

        // 1. Tính toán sát thương: 40% + (level-1)*20%
        double multiplier = 0.40 + (Math.max(0, level - 1) * 0.20);
        double extraDamage = baseDamage * multiplier;

        // 2. Áp dụng hiệu ứng khống chế
        // Say sóng (Nausea) 3 giây (60 ticks)
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 60, 0));

        // Làm chậm (Slowness) level 100 trong 1 giây (20 ticks)
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 99)); // Level 100 = amplifier 99

        // 3. Gây sát thương
        applyAbilityDamage(attacker, target, extraDamage);

        // 4. Hiệu ứng Visual: Particle choáng (Vòng tròn trên đầu)
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

                // Vẽ vòng tròn hạt "choáng" trên đầu mục tiêu
                Location headLoc = target.getLocation().add(0, target.getHeight() + 0.3, 0);
                double angle = ticks * 0.8;
                double x = Math.cos(angle) * 0.3;
                double z = Math.sin(angle) * 0.3;

                // Particle CRIT và VILLAGER_ANGRY tạo cảm giác bị choáng nặng
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