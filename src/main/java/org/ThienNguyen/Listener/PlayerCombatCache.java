package org.ThienNguyen.Listener;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCombatCache {
    private static final Map<UUID, CombatStats> cache = new ConcurrentHashMap<>();


    private static final Map<String, String> STAT_NAME_TO_FIELD = new HashMap<>();
    static {
        STAT_NAME_TO_FIELD.put("damage", "totalBonusDmg");
        STAT_NAME_TO_FIELD.put("pve_damage", "totalPveBonus");
        STAT_NAME_TO_FIELD.put("pvp_damage", "totalPvpBonus");
        STAT_NAME_TO_FIELD.put("critical_chance", "totalCritChance");
        STAT_NAME_TO_FIELD.put("critical_damage", "totalCritDamage");
        STAT_NAME_TO_FIELD.put("lifesteal", "totalLifesteal");
        STAT_NAME_TO_FIELD.put("dodge_rate", "totalDodge");
        STAT_NAME_TO_FIELD.put("block_rate", "totalBlock");
        STAT_NAME_TO_FIELD.put("penetration", "totalPenetration");
        STAT_NAME_TO_FIELD.put("true_damage", "totalTrueDamage");
        STAT_NAME_TO_FIELD.put("thorns", "totalThorns");
        STAT_NAME_TO_FIELD.put("max_mana", "totalMaxMana");
        STAT_NAME_TO_FIELD.put("mana_regen", "totalManaRegen");
        STAT_NAME_TO_FIELD.put("exp_bonus", "totalExpBonus");
        STAT_NAME_TO_FIELD.put("movement_speed", "totalMovementSpeed");
        STAT_NAME_TO_FIELD.put("armor_pen", "totalArmorPen");
        STAT_NAME_TO_FIELD.put("health_regen", "totalHealthRegen");
        STAT_NAME_TO_FIELD.put("all_damage", "totalAllDamage");
        STAT_NAME_TO_FIELD.put("all_defense", "totalAllDefense");
        STAT_NAME_TO_FIELD.put("bow_damage", "totalBowDamage");
        STAT_NAME_TO_FIELD.put("knockback_resistance", "totalKnockbackResist");
        STAT_NAME_TO_FIELD.put("death_damage", "totalDeathDamage");
        STAT_NAME_TO_FIELD.put("accuracy", "totalAccuracy");
        STAT_NAME_TO_FIELD.put("crit_damage_reduction", "totalCritDamageReduction");
        STAT_NAME_TO_FIELD.put("magic_damage", "totalMagicDamage");
        STAT_NAME_TO_FIELD.put("magic_defense", "totalMagicDefense");
        STAT_NAME_TO_FIELD.put("armor", "totalArmor");
        STAT_NAME_TO_FIELD.put("pve_defense", "totalPveDef");
        STAT_NAME_TO_FIELD.put("pvp_defense", "totalPvpDef");
        STAT_NAME_TO_FIELD.put("deep_wound", "totalDeepWound");
        STAT_NAME_TO_FIELD.put("damage_reduction", "totalDamageReduction");
        STAT_NAME_TO_FIELD.put("effect_resistance", "totalEffectResistance");
        STAT_NAME_TO_FIELD.put("health", "totalHealth");
        STAT_NAME_TO_FIELD.put("attack_speed", "totalAttackSpeed");
        STAT_NAME_TO_FIELD.put("cooldown_reduction", "totalCooldownReduction");

    }


    public static boolean isKnownStat(String statKey) {
        return STAT_NAME_TO_FIELD.containsKey(statKey);
    }


    public static java.util.Set<String> getKnownStatKeys() {
        return STAT_NAME_TO_FIELD.keySet();
    }


    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private static Field resolveField(String javaFieldName) {
        return FIELD_CACHE.computeIfAbsent(javaFieldName, name -> {
            try {
                Field f = CombatStats.class.getField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                return null;
            }
        });
    }

    public static boolean addToField(CombatStats stats, String statKey, double amount) {
        String javaField = STAT_NAME_TO_FIELD.get(statKey);
        if (javaField == null) return false;
        Field f = resolveField(javaField);
        if (f == null) return false;
        try {
            f.setDouble(stats, f.getDouble(stats) + amount);
            return true;
        } catch (IllegalAccessException e) { return false; }
    }


    public static boolean multiplyField(CombatStats stats, String statKey, double percent) {
        String javaField = STAT_NAME_TO_FIELD.get(statKey);
        if (javaField == null) return false;
        Field f = resolveField(javaField);
        if (f == null) return false;
        try {
            f.setDouble(stats, f.getDouble(stats) * (1.0 + percent / 100.0));
            return true;
        } catch (IllegalAccessException e) { return false; }
    }

    public static double getEffective(UUID uuid, String statKey, double baseValue) {
        CombatStats stats = cache.get(uuid);
        if (stats == null || stats.tempBuffs.isEmpty()) return baseValue;

        double bonus = 0.0;
        long now = System.currentTimeMillis();
        for (org.ThienNguyen.Listener.Passive.TempBuff buff : stats.tempBuffs.values()) {
            if (buff.expireAtMillis <= now) continue;
            if (buff.statKey.equals(statKey)) {
                bonus += buff.amount;
            }
        }
        return baseValue + bonus;
    }


    public static double getEffectiveByStatName(UUID uuid, String statKey) {
        CombatStats stats = cache.get(uuid);
        if (stats == null) return 0.0;

        String javaField = STAT_NAME_TO_FIELD.get(statKey);
        double base = 0.0;
        if (javaField != null) {
            Field f = resolveField(javaField);
            if (f != null) {
                try { base = f.getDouble(stats); } catch (IllegalAccessException ignored) {}
            }
        }
        return getEffective(uuid, statKey, base);
    }

    public static class CombatStats {
        public double totalCritDamageReduction = 0.0;
        public double totalMagicDamage = 1.0;
        public Map<String, Integer> elementDefenses = new HashMap<>();
        public double totalMagicDefense = 0.0;
        public double totalBonusDmg = 0;
        public double totalPveBonus = 0;
        public double totalPvpBonus = 0;
        public double totalAccuracy = 0;
        public double totalCritChance = 0;
        public double totalCritDamage = 0;
        public double totalLifesteal = 0;
        public double totalPenetration = 0;
        public double totalTrueDamage = 0;
        public double totalArmorPen = 0;
        public double totalElementDamage = 0;
        public double totalDeathDamage = 0;
        public double totalDeepWound = 0.0;
        public double totalBowDamage = 0;
        public double totalAllDamage = 0;
        public double totalMaxMana = 0;
        public double totalManaRegen = 0;

        public double totalArmor = 0;
        public double totalPveDef = 0;
        public double totalPvpDef = 0;
        public double totalDodge = 0;
        public double totalBlock = 0;
        public double totalThorns = 0;
        public double totalAllDefense = 0;
        public double totalExpBonus = 0.0;
        public double totalHealth = 0;

        public double totalHealthRegen = 0;
        public double totalKnockbackResist = 0;
        public double totalMovementSpeed = 0;
        public double totalDamageReduction = 0.0;
        public double totalEffectResistance = 0.0;
        public double totalAttackSpeed = 0;
        public double totalCooldownReduction = 0;


        public Map<String, double[]> bestAbilities = new HashMap<>();
        public Map<String, Double> weaponElementDamage = new HashMap<>();
        public Map<String, Integer> weaponElementLevels = new HashMap<>();


        public Map<String, org.ThienNguyen.Listener.Passive.TempBuff> tempBuffs = new ConcurrentHashMap<>();


        public void clear() {
            totalAccuracy = 0;
            totalDeepWound = 0.0;
            totalDamageReduction = 0.0;
            totalMaxMana = 0;
            totalManaRegen = 0;
            totalDeathDamage = 0;
            totalBonusDmg = totalPveBonus = totalPvpBonus = 0;
            totalCritChance = totalCritDamage = totalLifesteal = 0;
            totalPenetration = totalTrueDamage = totalArmorPen = 0;
            totalElementDamage = totalBowDamage = totalAllDamage = 0;
            elementDefenses.clear();
            totalCritDamageReduction = 0.0;
            totalArmor = totalPveDef = totalPvpDef = 0;
            totalDodge = totalBlock = totalThorns = totalAllDefense = 0;
            totalHealth = 0;
            totalHealthRegen = 0;
            totalKnockbackResist = 0;
            totalMovementSpeed = 0;
            totalExpBonus = 0;
            totalEffectResistance = 0.0;
            totalMagicDamage = 1.0;
            totalMagicDefense = 0.0;
            totalAttackSpeed = 0;
            totalCooldownReduction = 0;

            bestAbilities.clear();
            weaponElementDamage.clear();
            weaponElementLevels.clear();
        }


        public void clearWeaponElements() {
            weaponElementDamage.clear();
            weaponElementLevels.clear();
        }
    }
    public double getRealPower(Player player) {

        var stats = org.ThienNguyen.Listener.PlayerCombatCache.getStats(player.getUniqueId());


        double attackDmg = stats.totalBonusDmg;


        if (attackDmg <= 0) {
            attackDmg = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        }

        return attackDmg;
    }
    public static CombatStats getStats(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new CombatStats());
    }

    public static void updateCache(UUID uuid, CombatStats stats) {
        cache.put(uuid, stats);
    }

    public static void invalidate(UUID uuid) {
        cache.remove(uuid);
    }
}