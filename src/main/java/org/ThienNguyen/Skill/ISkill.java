package org.ThienNguyen.Skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface ISkill {
    String getName();

    // Trả về loại skill: "Weapon", "Command", hoặc "MythicLib"
    String getType();

    // Trả về điều kiện kích hoạt: HIT, SNEAK, RIGHT_CLICK, v.v.
    String getTrigger();

    /**
     * Thực thi kỹ năng
     * @param player Người sử dụng kỹ năng
     * @param target Mục tiêu (có thể null nếu skill cast từ xa hoặc không cần mục tiêu)
     * @param level Cấp độ của kỹ năng
     * @param baseDamage Sát thương gốc từ vũ khí hoặc từ sự kiện gây ra
     */
    void execute(Player player, LivingEntity target, int level, double baseDamage);
}