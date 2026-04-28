package org.ThienNguyen.Skill.TypeSkill;

import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.ThienNguyen.Skill.ISkill;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MythicMobsSkill implements ISkill {
    private final String skillId;
    private final JavaPlugin plugin;

    public MythicMobsSkill(JavaPlugin plugin, String skillId) {
        this.plugin = plugin;
        this.skillId = skillId;
    }

    @Override public String getName() { return skillId; }
    @Override public String getType() { return "MythicMobs"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(@NotNull Player player, @Nullable LivingEntity target, int level, double baseDamage) {
        Optional<Skill> maybeSkill = MythicBukkit.inst().getSkillManager().getSkill(skillId);
        if (maybeSkill.isEmpty()) return;

        // Lưu trữ thông tin vào Metadata để các Event hoặc Mechanics khác có thể đọc trực tiếp giá trị số
        player.setMetadata("MYITEM_CASTING_SKILL", new FixedMetadataValue(plugin, skillId));
        player.setMetadata("MYITEM_SKILL_LEVEL", new FixedMetadataValue(plugin, level));

        // Quan trọng: Lưu trực tiếp baseDamage dạng double để tránh phải dùng PlaceholderAPI sau này
        player.setMetadata("MYITEM_SKILL_BASE_DAMAGE", new FixedMetadataValue(plugin, baseDamage));

        try {
            // Thực hiện cast skill từ MythicMobs
            // Power mặc định là 1.0, sát thương thực tế sẽ được xử lý dựa trên baseDamage lưu trong metadata
            if (target != null) {
                MythicBukkit.inst().getAPIHelper().castSkill(player, skillId, target, player.getLocation(), null, null, 1.0f);
            } else {
                MythicBukkit.inst().getAPIHelper().castSkill(player, skillId, player, player.getLocation(), null, null, 1.0f);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi cast MythicSkill " + skillId + ": " + e.getMessage());
        }
    }
}