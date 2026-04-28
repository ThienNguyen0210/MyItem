package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Wither implements IAbility {
    @Override
    public String getName() {
        return "WITHER";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;
        // Cấp độ: level - 1, Thời gian: level * 2 giây (40 ticks)
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, level * 20 * 2, level - 1));
    }
}