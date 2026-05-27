package org.ThienNguyen.Listener;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCombatCache {
    private static final Map<UUID, CombatStats> cache = new ConcurrentHashMap<>();

    public static class CombatStats {
        public double totalCritDamageReduction = 0.0;
        public double totalMagicDamage = 0.0;
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
        
        public double totalHealthRegen = 0;
        public double totalKnockbackResist = 0; 
        public double totalMovementSpeed = 0;
        public double totalDamageReduction = 0.0;

        
        public Map<String, double[]> bestAbilities = new HashMap<>();
        public Map<String, Double> weaponElementDamage = new HashMap<>();
        public Map<String, Integer> weaponElementLevels = new HashMap<>();

        /**
         * Reset toàn bộ chỉ số về 0 và xóa sạch các Map
         */
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

            
            totalHealthRegen = 0;
            totalKnockbackResist = 0;
            totalMovementSpeed = 0;
            totalExpBonus = 0;
            totalMagicDamage = 0.0; 
            totalMagicDefense = 0.0;
            
            bestAbilities.clear();
            weaponElementDamage.clear();
            weaponElementLevels.clear();
        }

        /**
         * Clear riêng phần nguyên tố vũ khí (dùng khi đổi item trên tay)
         */
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