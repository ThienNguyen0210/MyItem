package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.ThienNguyen.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Buff tạm thời 1 stat — cộng thẳng vào CombatStats, trừ ngược lại sau duration.
 *
 * yml:
 * - type: BUFF_STAT
 *   target: SELF
 *   stat: critical_damage
 *   amount: "%player_level% * 0.5"   # số, biểu thức, hoặc placeholder PAPI
 *   duration-seconds: "5"              # tương tự
 *
 * Cả "amount" và "duration-seconds" resolve lúc execute() — buff lượng và thời gian
 * phản ánh đúng stat actor tại thời điểm trigger.
 */
public class BuffStatMechanic extends AbstractMechanic {

    private final String stat;
    private final String rawAmount;
    private final String rawDuration;

    public BuffStatMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.stat        = cfg.getString("stat", "").toLowerCase().trim();
        this.rawAmount   = cfg.getString("amount",           "0");
        this.rawDuration = cfg.getString("duration-seconds", "5");
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity targetEntity = resolveTarget(ctx);
        if (!(targetEntity instanceof Player p) || stat.isEmpty()) return false;

        double amount   = ExpressionResolver.resolve(rawAmount,   ctx.getActor(), 0);
        int    duration = ExpressionResolver.resolveInt(rawDuration, ctx.getActor(), 5);

        if (amount == 0) return false;

        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(p.getUniqueId());
        if (!applyStatDelta(stats, stat, amount)) return false;

        final double buffAmount = amount; // effectively final for lambda
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                PlayerCombatCache.CombatStats current = PlayerCombatCache.getStats(p.getUniqueId());
                applyStatDelta(current, stat, -buffAmount);
            }
        }.runTaskLater(Main.getInstance(), duration * 20L);

        return true;
    }

    private static boolean applyStatDelta(PlayerCombatCache.CombatStats s, String stat, double delta) {
        switch (stat) {
            case "damage"                    -> s.totalBonusDmg            += delta;
            case "pve_damage"                -> s.totalPveBonus            += delta;
            case "pvp_damage"                -> s.totalPvpBonus            += delta;
            case "all_damage"                -> s.totalAllDamage           += delta;
            case "bow_damage"                -> s.totalBowDamage           += delta;
            case "magic_damage"              -> s.totalMagicDamage         += delta;
            case "true_damage"               -> s.totalTrueDamage          += delta;
            case "death_damage"              -> s.totalDeathDamage         += delta;
            case "critical_chance"           -> s.totalCritChance          += delta;
            case "critical_damage"           -> s.totalCritDamage          += delta;
            case "critical_damage_reduction" -> s.totalCritDamageReduction += delta;
            case "lifesteal"                 -> s.totalLifesteal           += delta;
            case "penetration"               -> s.totalPenetration         += delta;
            case "armor_pen"                 -> s.totalArmorPen            += delta;
            case "accuracy"                  -> s.totalAccuracy            += delta;
            case "damage_reduction"          -> s.totalDamageReduction     += delta;
            case "armor"                     -> s.totalArmor               += delta;
            case "pve_defense"               -> s.totalPveDef              += delta;
            case "pvp_defense"               -> s.totalPvpDef              += delta;
            case "all_defense"               -> s.totalAllDefense          += delta;
            case "magic_defense"             -> s.totalMagicDefense        += delta;
            case "dodge_rate"                -> s.totalDodge               += delta;
            case "block_rate"                -> s.totalBlock               += delta;
            case "thorns"                    -> s.totalThorns              += delta;
            case "knockback_resistance"      -> s.totalKnockbackResist     += delta;
            case "max_mana"                  -> s.totalMaxMana             += delta;
            case "mana_regen"                -> s.totalManaRegen           += delta;
            case "health_regen"              -> s.totalHealthRegen         += delta;
            case "exp_bonus"                 -> s.totalExpBonus            += delta;
            case "movement_speed"            -> s.totalMovementSpeed       += delta;
            default -> { return false; }
        }
        return true;
    }
}