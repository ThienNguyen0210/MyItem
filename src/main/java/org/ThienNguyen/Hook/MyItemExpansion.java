package org.ThienNguyen.Hook;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.ThienNguyen.Main;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.Locale;

public class MyItemExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getAuthor() {
        return "ThienNguyen";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "myitem";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.8.1";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "0.0";
        Player p = player.getPlayer();
        if (p == null) return "0.0";

        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(p.getUniqueId());

        // Chuyển toàn bộ params sang chữ thường để kiểm tra đồng bộ, tránh lỗi viết hoa viết thường
        String lowerParams = params.toLowerCase();

        // Hậu tố "_rounded" áp dụng cho mọi placeholder bên dưới: thay vì số thập phân
        // đầy đủ, trả về dạng rút gọn "1.2k" / "1.5m". Tách hậu tố ra trước rồi tính
        // toán như bình thường, chỉ khác bước format ở cuối.
        boolean rounded = lowerParams.endsWith("_rounded");
        if (rounded) {
            lowerParams = lowerParams.substring(0, lowerParams.length() - "_rounded".length());
        }

        // --- %myitem_lucchien% / %myitem_lucchien_rounded% ---
        if (lowerParams.equals("lucchien")) {
            int lucChien = calculateLucChien(p, stats);
            return rounded ? formatRounded(lucChien) : formatLucChien(lucChien);
        }

        // --- %myitem_skill_damage% / %myitem_skill_damage_rounded% ---
        if (lowerParams.equals("skill_damage")) {
            double skillDmg = computeSkillDamage(p, stats);
            return rounded ? formatRounded(skillDmg) : String.format("%.1f", skillDmg);
        }

        // --- %myitem_totaldamage% / %myitem_totaldamage_rounded% ---
        if (lowerParams.equals("totaldamage")) {
            TotalDamageResult result = computeTotalDamage(p, stats);
            if (rounded) {
                return formatRounded(result.value);
            }
            return "§f" + String.format("%.1f", result.value);
        }

        // --- %myitem_stats_<id>% / %myitem_stats_<id>_rounded% ---
        if (lowerParams.startsWith("stats_")) {
            String statId = lowerParams.substring(6);
            double value = getStatValue(p, stats, statId);
            return rounded ? formatRounded(value) : String.format("%.1f", value);
        }

        return null;
    }

    /**
     * Tính toán Lực Chiến tổng hợp dựa trên TOÀN BỘ chỉ số từ Vanilla, Fabled và Cache Hệ Thống (đã bao gồm tempBuffs hiệu lực)
     */
    private int calculateLucChien(Player player, PlayerCombatCache.CombatStats stats) {
        java.util.UUID uuid = player.getUniqueId();

        // ==========================================
        // 1. CHỈ SỐ CƠ BẢN (VANILLA MINECRAFT)
        // ==========================================
        var hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double rawMaxHealth = (hpAttr != null) ? hpAttr.getValue() : 20.0;
        double effectiveHealth = Math.max(0, rawMaxHealth - 20); // Chỉ tính lượng máu cộng thêm từ trang bị/hệ thống

        // GENERIC_ATTACK_DAMAGE tự động bao gồm sát thương cơ bản của vũ khí đang cầm
        var dmgAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        double baseAttackDamage = (dmgAttr != null) ? dmgAttr.getValue() : 1.0;

        var armorAttr = player.getAttribute(Attribute.GENERIC_ARMOR);
        double vanillaArmor = (armorAttr != null) ? armorAttr.getValue() : 0.0;

        // ==========================================
        // 2. CHỈ SỐ HỆ THỐNG PHỤ (FABLED RPG)
        // ==========================================
        String manaStr = PlaceholderAPI.setPlaceholders(player, "%fabled_default_currentmaxmana%");
        double mana = 0;
        try { mana = Double.parseDouble(manaStr); } catch (Exception ignored) {}
        double effectiveMana = Math.max(0, mana - 200);

        String levelStr = PlaceholderAPI.setPlaceholders(player, "%fabled_default_currentlevel%");
        double level = 0;
        try { level = Double.parseDouble(levelStr); } catch (Exception ignored) {}

        double walkSpeedPercent = (player.getWalkSpeed() / 0.2) * 100;

        // Tính Base Lực Chiến từ Vanilla và Fabled
        double baseLucChien = (effectiveHealth * 0.37)
                + (baseAttackDamage * 1.87)
                + (vanillaArmor * 31.74)
                + (effectiveMana * 0.32)
                + (walkSpeedPercent * 3.89)
                + (level * 0.05);

        // ==========================================
        // 3. CHỈ SỐ TỪ CACHE CUSTOM (TẤN CÔNG & PHÒNG THỦ - CÓ TÍNH TEMPFETCH HIỆU LỰC)
        // ==========================================
        double totalCooldownReduction = PlayerCombatCache.getEffective(uuid, "cooldown_reduction", stats.totalCooldownReduction);
        double totalBonusDmg = PlayerCombatCache.getEffective(uuid, "damage", stats.totalBonusDmg);
        double totalMagicDamage = PlayerCombatCache.getEffective(uuid, "magic_damage", stats.totalMagicDamage);
        double totalBowDamage = PlayerCombatCache.getEffective(uuid, "bow_damage", stats.totalBowDamage);
        double totalTrueDamage = PlayerCombatCache.getEffective(uuid, "true_damage", stats.totalTrueDamage);
        double totalDeathDamage = PlayerCombatCache.getEffective(uuid, "death_damage", stats.totalDeathDamage);
        double totalElementDamage = PlayerCombatCache.getEffective(uuid, "element_damage", stats.totalElementDamage);
        double totalDeepWound = PlayerCombatCache.getEffective(uuid, "deep_wound", stats.totalDeepWound);
        double totalAllDamage = PlayerCombatCache.getEffective(uuid, "all_damage", stats.totalAllDamage);
        double totalPvpBonus = PlayerCombatCache.getEffective(uuid, "pvp_damage", stats.totalPvpBonus);
        double totalPveBonus = PlayerCombatCache.getEffective(uuid, "pve_damage", stats.totalPveBonus);
        double totalCritChance = PlayerCombatCache.getEffective(uuid, "critical_chance", stats.totalCritChance);
        double totalCritDamage = PlayerCombatCache.getEffective(uuid, "critical_damage", stats.totalCritDamage);
        double totalPenetration = PlayerCombatCache.getEffective(uuid, "penetration", stats.totalPenetration);
        double totalArmorPen = PlayerCombatCache.getEffective(uuid, "armor_pen", stats.totalArmorPen);
        double totalAccuracy = PlayerCombatCache.getEffective(uuid, "accuracy", stats.totalAccuracy);

        double totalArmor = PlayerCombatCache.getEffective(uuid, "armor", stats.totalArmor);
        double totalMagicDefense = PlayerCombatCache.getEffective(uuid, "magic_defense", stats.totalMagicDefense);
        double totalAllDefense = PlayerCombatCache.getEffective(uuid, "all_defense", stats.totalAllDefense);
        double totalPvpDef = PlayerCombatCache.getEffective(uuid, "pvp_defense", stats.totalPvpDef);
        double totalPveDef = PlayerCombatCache.getEffective(uuid, "pve_defense", stats.totalPveDef);
        double totalDamageReduction = PlayerCombatCache.getEffective(uuid, "damage_reduction", stats.totalDamageReduction);
        double totalCritDamageReduction = PlayerCombatCache.getEffective(uuid, "crit_damage_reduction", stats.totalCritDamageReduction);
        double totalDodge = PlayerCombatCache.getEffective(uuid, "dodge_rate", stats.totalDodge);
        double totalBlock = PlayerCombatCache.getEffective(uuid, "block_rate", stats.totalBlock);
        double totalThorns = PlayerCombatCache.getEffective(uuid, "thorns", stats.totalThorns);

        double totalLifesteal = PlayerCombatCache.getEffective(uuid, "lifesteal", stats.totalLifesteal);
        double totalHealthRegen = PlayerCombatCache.getEffective(uuid, "health_regen", stats.totalHealthRegen);
        double totalMaxMana = PlayerCombatCache.getEffective(uuid, "max_mana", stats.totalMaxMana);
        double totalManaRegen = PlayerCombatCache.getEffective(uuid, "mana_regen", stats.totalManaRegen);
        double totalMovementSpeed = PlayerCombatCache.getEffective(uuid, "movement_speed", stats.totalMovementSpeed);
        double totalKnockbackResist = PlayerCombatCache.getEffective(uuid, "knockback_resistance", stats.totalKnockbackResist);
        double totalExpBonus = PlayerCombatCache.getEffective(uuid, "exp_bonus", stats.totalExpBonus);

        // --- NHÓM 1: SÁT THƯƠNG & KHẢ NĂNG TẤN CÔNG ---
        double attackStats = (totalBonusDmg + totalMagicDamage) * 1.87;

        double specialAttackStats = (totalBowDamage * 2.15)
                + (totalTrueDamage * 3.50)
                + (totalDeathDamage * 2.50)
                + (totalElementDamage * 2.00)
                + (totalDeepWound * 3.00);

        double percentAttackStats = (totalAllDamage * 5.50)
                + (totalPvpBonus * 2.87)
                + (totalPveBonus * 2.87);

        double critAndPenetrationStats = (totalCritChance * 21.48)
                + (totalCritDamage * 12.01)
                + (totalPenetration * 40.00)
                + (totalArmorPen * 35.00)
                + (totalAccuracy * 15.00);

        // --- NHÓM 2: GIÁP & PHÒNG THỦ KHÁNG TÍNH ---
        double defenseStats = (totalArmor * 25.74)
                + (totalMagicDefense * 28.50);

        double percentDefenseStats = (totalAllDefense * 6.00)
                + (totalPvpDef * 3.00)
                + (totalPveDef * 3.00)
                + (totalDamageReduction * 8.50)
                + (totalCritDamageReduction * 5.00);

        double utilityDefenseStats = (totalDodge * 37.21)
                + (totalBlock * 32.21)
                + (totalThorns * 18.50);

        // --- NHÓM 3: TIỆN ÍCH & HỒI PHỤC ---
        double utilityAndRegenStats = (totalLifesteal * 22.50)
                + (totalHealthRegen * 4.50)
                + (totalMaxMana * 0.32)
                + (totalManaRegen * 5.00)
                + (totalMovementSpeed * 3.89)
                + (totalKnockbackResist * 45.00)
                + (totalExpBonus * 1.50)
                + (totalCooldownReduction * 10.00);;

        double bonusLucChien = attackStats
                + specialAttackStats
                + percentAttackStats
                + critAndPenetrationStats
                + defenseStats
                + percentDefenseStats
                + utilityDefenseStats
                + utilityAndRegenStats;

        return (int) (baseLucChien + bonusLucChien);
    }

    /**
     * Tính sát thương kỹ năng (vật lý + ma pháp), tách riêng để dùng chung
     * cho cả dạng số đầy đủ và dạng rút gọn (_rounded).
     */
    private double computeSkillDamage(Player p, PlayerCombatCache.CombatStats stats) {
        var dmgAttr = p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        double weaponDmg = (dmgAttr != null) ? dmgAttr.getValue() : 1.0;

        double basePhysDmg = weaponDmg + PlayerCombatCache.getEffective(p.getUniqueId(), "damage", stats.totalBonusDmg);
        double magicDmgEffective = PlayerCombatCache.getEffective(p.getUniqueId(), "magic_damage", stats.totalMagicDamage);
        return basePhysDmg + magicDmgEffective;
    }

    private static class TotalDamageResult {
        final double value;
        final boolean crit;
        TotalDamageResult(double value, boolean crit) {
            this.value = value;
            this.crit = crit;
        }
    }

    /**
     * Tính tổng sát thương (bao gồm cơ chế hiển thị Crit ngẫu nhiên theo chu kỳ 5 giây),
     * tách riêng để dùng chung cho cả dạng số đầy đủ và dạng rút gọn (_rounded).
     */
    private TotalDamageResult computeTotalDamage(Player p, PlayerCombatCache.CombatStats stats) {
        var dmgAttr = p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        double weaponDmg = (dmgAttr != null) ? dmgAttr.getValue() : 1.0;

        double basePhysDmg = weaponDmg + PlayerCombatCache.getEffective(p.getUniqueId(), "damage", stats.totalBonusDmg);
        double magicDmgEffective = PlayerCombatCache.getEffective(p.getUniqueId(), "magic_damage", stats.totalMagicDamage);
        double combinedBaseDmg = basePhysDmg + magicDmgEffective;

        double allDamageEffective = PlayerCombatCache.getEffective(p.getUniqueId(), "all_damage", stats.totalAllDamage);
        double finalDmg = combinedBaseDmg * (1 + allDamageEffective / 100.0);

        long cycle = (System.currentTimeMillis() / 1000) / 5;
        java.util.Random r = new java.util.Random(p.getUniqueId().getMostSignificantBits() + cycle);

        double critChanceEffective = PlayerCombatCache.getEffective(p.getUniqueId(), "critical_chance", stats.totalCritChance);
        if (r.nextDouble() * 100 <= critChanceEffective) {
            double baseCritMult = Main.getInstance().getCustomConfig().getDouble("crit-multiplier", 1.5);
            double critDamageEffective = PlayerCombatCache.getEffective(p.getUniqueId(), "critical_damage", stats.totalCritDamage);
            double totalCritDmgMult = baseCritMult + (critDamageEffective / 100.0);
            return new TotalDamageResult(finalDmg * totalCritDmgMult, true);
        }

        return new TotalDamageResult(finalDmg, false);
    }

    private String formatLucChien(int lucChien) {
        return NumberFormat.getInstance(Locale.US).format(lucChien);
    }

    /**
     * Rút gọn số lớn thành dạng "1.2k" (nghìn) hoặc "1.5m" (triệu).
     * Bỏ phần thập phân khi nó là ".0" (ví dụ "10k" thay vì "10.0k").
     */
    private String formatRounded(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000) {
            return trimZeroDecimal(value / 1_000_000.0) + "m";
        }
        if (abs >= 1_000) {
            return trimZeroDecimal(value / 1_000.0) + "k";
        }
        return trimZeroDecimal(value);
    }

    private String trimZeroDecimal(double value) {
        String formatted = String.format(Locale.US, "%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }

    /**
     * Map toàn bộ các ID stats thành giá trị tương ứng trong Cache (đã bao gồm tempBuffs hiệu lực)
     */
    private double getStatValue(Player player, PlayerCombatCache.CombatStats stats, String id) {
        java.util.UUID uuid = player.getUniqueId();
        return switch (id) {
            // --- SÁT THƯƠNG & TẤN CÔNG ---
            case "damage" -> {
                // Lấy sát thương gốc từ vũ khí (Attribute tự động thay đổi theo item cầm trên tay)
                var attr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                double weaponDmg = (attr != null) ? attr.getValue() : 1.0;
                // Cộng thêm sát thương từ hệ thống custom
                double cachedDmg = PlayerCombatCache.getEffective(uuid, "damage", stats.totalBonusDmg);
                yield weaponDmg + cachedDmg;
            }
            case "pve_damage" -> PlayerCombatCache.getEffective(uuid, "pve_damage", stats.totalPveBonus);
            case "pvp_damage" -> PlayerCombatCache.getEffective(uuid, "pvp_damage", stats.totalPvpBonus);
            case "all_damage" -> PlayerCombatCache.getEffective(uuid, "all_damage", stats.totalAllDamage);
            case "magic_damage" -> PlayerCombatCache.getEffective(uuid, "magic_damage", stats.totalMagicDamage);
            case "bow_damage" -> PlayerCombatCache.getEffective(uuid, "bow_damage", stats.totalBowDamage);
            case "true_damage" -> PlayerCombatCache.getEffective(uuid, "true_damage", stats.totalTrueDamage);
            case "death_damage" -> PlayerCombatCache.getEffective(uuid, "death_damage", stats.totalDeathDamage);

            // --- BẠO KÍCH ---
            case "crit_chance", "critical_chance" -> PlayerCombatCache.getEffective(uuid, "critical_chance", stats.totalCritChance);
            case "crit_damage", "critical_damage" -> PlayerCombatCache.getEffective(uuid, "critical_damage", stats.totalCritDamage);
            // Sửa lỗi case mismatch: ánh xạ cả 2 biến sang khóa chuẩn "crit_damage_reduction" trùng khớp với PlayerCombatCache
            case "critical_damage_reduction", "crit_damage_reduction" -> PlayerCombatCache.getEffective(uuid, "crit_damage_reduction", stats.totalCritDamageReduction);

            // --- PHÒNG THỦ & KHÁNG SÁT THƯƠNG ---
            case "armor" -> PlayerCombatCache.getEffective(uuid, "armor", stats.totalArmor);
            case "magic_defense" -> PlayerCombatCache.getEffective(uuid, "magic_defense", stats.totalMagicDefense);
            case "pve_defense", "pve_def" -> PlayerCombatCache.getEffective(uuid, "pve_defense", stats.totalPveDef);
            case "pvp_defense", "pvp_def" -> PlayerCombatCache.getEffective(uuid, "pvp_defense", stats.totalPvpDef);
            case "all_defense" -> PlayerCombatCache.getEffective(uuid, "all_defense", stats.totalAllDefense);
            case "damage_reduction" -> PlayerCombatCache.getEffective(uuid, "damage_reduction", stats.totalDamageReduction);
            case "armor_pen" -> PlayerCombatCache.getEffective(uuid, "armor_pen", stats.totalArmorPen);
            case "penetration" -> PlayerCombatCache.getEffective(uuid, "penetration", stats.totalPenetration);

            // --- ĐẶC TÍNH CHIẾN ĐẤU (DODGE, BLOCK, LIFE STEAL, THORN) ---
            case "dodge_rate" -> PlayerCombatCache.getEffective(uuid, "dodge_rate", stats.totalDodge);
            case "block_rate" -> PlayerCombatCache.getEffective(uuid, "block_rate", stats.totalBlock);
            case "lifesteal" -> PlayerCombatCache.getEffective(uuid, "lifesteal", stats.totalLifesteal);
            case "thorns" -> PlayerCombatCache.getEffective(uuid, "thorns", stats.totalThorns);
            case "accuracy" -> PlayerCombatCache.getEffective(uuid, "accuracy", stats.totalAccuracy);
            case "knockback_resistance" -> PlayerCombatCache.getEffective(uuid, "knockback_resistance", stats.totalKnockbackResist);

            // --- TỐC ĐỘ, ĐA DỤNG VÀ HỒI PHỤC ---
            case "movement_speed" -> PlayerCombatCache.getEffective(uuid, "movement_speed", stats.totalMovementSpeed);
            case "attack_speed" -> PlayerCombatCache.getEffective(uuid, "attack_speed", stats.totalAttackSpeed);
            case "exp_bonus" -> PlayerCombatCache.getEffective(uuid, "exp_bonus", stats.totalExpBonus);
            case "max_mana" -> PlayerCombatCache.getEffective(uuid, "max_mana", stats.totalMaxMana);
            case "mana_regen" -> PlayerCombatCache.getEffective(uuid, "mana_regen", stats.totalManaRegen);
            case "health_regen" -> PlayerCombatCache.getEffective(uuid, "health_regen", stats.totalHealthRegen);
            case "cooldown_reduction", "cooldown", "cd_reduction" -> PlayerCombatCache.getEffective(uuid, "cooldown_reduction", stats.totalCooldownReduction);
            default -> 0.0;
        };
    }
}