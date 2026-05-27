package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BadLuck implements IAbility {
    @Override
    public String getName() {
        return "BadLuck";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;

        
        
        int durationTicks = 40 + (int)((level - 1) * 0.3 * 20);

        
        target.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, durationTicks, 0));
    }
}