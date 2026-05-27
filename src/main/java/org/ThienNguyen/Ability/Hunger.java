package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Hunger implements IAbility {
    @Override
    public String getName() {
        return "HUNGER";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (!(target instanceof Player victim)) return;
        
        victim.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, level * 20 * 3, level));
    }
}