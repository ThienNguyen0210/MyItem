package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Blind implements IAbility {
    @Override
    public String getName() {
        return "Blind";
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (target == null || target.isDead()) return;
        // Nausea cấp độ 1 là đủ để rung màn hình
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, level * 20 * 4, 0));
    }
}