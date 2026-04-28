package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Slow implements IAbility {
    @Override
    public String getName() {
        return "SLOW";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, level * 20 * 2, level));
    }
}