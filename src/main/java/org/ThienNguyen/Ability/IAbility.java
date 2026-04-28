package org.ThienNguyen.Ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface IAbility {
    String getName();
    // Thêm tham số double damage để nhận sát thương thực tế từ đòn đánh
    void execute(Player attacker, LivingEntity target, int level, double baseDamage);
}