package org.ThienNguyen.API;

import org.ThienNguyen.Listener.PlayerCombatCache;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerRefreshStatsEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final PlayerCombatCache.CombatStats stats;

    public PlayerRefreshStatsEvent(Player player, PlayerCombatCache.CombatStats stats) {
        this.player = player;
        this.stats = stats;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerCombatCache.CombatStats getStats() {
        return stats;
    }

    /**
     * Tiện ích giúp các plugin khác cộng chỉ số nhanh chóng mà không cần truy cập trực tiếp vào fields.
     * @param type Loại chỉ số (ví dụ: "damage", "crit_chance", "armor"...)
     * @param val Giá trị cần cộng thêm
     */
    public void addStat(String type, double val) {
        if (val == 0 || type == null) return;

        switch (type.toLowerCase()) {
            // TẤN CÔNG
            case "damage" -> stats.totalBonusDmg += val;
            case "pve_damage", "pve_dmg" -> stats.totalPveBonus += val;
            case "pvp_damage", "pvp_dmg" -> stats.totalPvpBonus += val;
            case "all_damage" -> stats.totalAllDamage += val;
            case "magic_damage" -> stats.totalMagicDamage += val;
            case "bow_damage" -> stats.totalBowDamage += val;
            case "true_damage" -> stats.totalTrueDamage += val;
            case "death_damage" -> stats.totalDeathDamage += val;

            // CHỈ SỐ PHỤ TẤN CÔNG
            case "critical_chance", "crit_chance" -> stats.totalCritChance += val;
            case "critical_damage", "crit_damage" -> stats.totalCritDamage += val;
            case "penetration" -> stats.totalPenetration += val;
            case "armor_pen" -> stats.totalArmorPen += val;
            case "lifesteal" -> stats.totalLifesteal += val;
            case "accuracy" -> stats.totalAccuracy += val;
            case "exp_bonus", "exp" -> stats.totalExpBonus += val;
            case "movement_speed", "speed" -> stats.totalMovementSpeed += val;

            // PHÒNG THỦ
            case "armor" -> stats.totalArmor += val;
            case "pve_defense", "pve_def" -> stats.totalPveDef += val;
            case "pvp_defense", "pvp_def" -> stats.totalPvpDef += val;
            case "all_defense" -> stats.totalAllDefense += val;
            case "magic_defense", "magic_def" -> stats.totalMagicDefense += val;
            case "dodge_rate", "dodge" -> stats.totalDodge += val;
            case "block_rate", "block" -> stats.totalBlock += val;
            case "thorns" -> stats.totalThorns += val;
            case "knockback_resistance", "kb_resist" -> stats.totalKnockbackResist += val;
        }
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}