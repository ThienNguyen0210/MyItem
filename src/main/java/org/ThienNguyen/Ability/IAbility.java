package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface IAbility {
    String getName();
    
    void execute(Player attacker, LivingEntity target, int level, double baseDamage);
}