package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Poison implements IAbility {
    @Override
    public String getName() {
        return "POISON";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        
        int duration = level * 40;

        
        
        int amplifier = Math.max(0, level - 1);

        
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration, amplifier));

        
        
    }
}