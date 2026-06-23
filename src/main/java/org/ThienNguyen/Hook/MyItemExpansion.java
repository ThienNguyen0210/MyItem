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
        return "1.8.0"; // Nâng cấp phiên bản vì đã cập nhật thêm placeholder skill_damage mới
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "0.0";
        Player p = player.getPlayer();
        if (p == null) return "0.0";

        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(p.getUniqueId());

        // Chuyển toàn bộ params sang chữ thường để kiểm tra đồng bộ, tránh lỗi viết hoa viết thường
        String lowerParams = params.toLowerCase();

        // --- %myitem_lucchien% ---
        if (lowerParams.equals("lucchien")) {
            return formatLucChien(calculateLucChien(p, stats));
        }

        // --- %myitem_skill_damage% ---
        if (lowerParams.equals("skill_damage")) {
            // Lấy sát thương vật lý gốc (mặc định tối thiểu là 1.0 nếu tay không)
            double basePhysDmg = stats.totalBonusDmg;
            if (basePhysDmg <= 0) {
                var attr = p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                basePhysDmg = (attr != null) ? attr.getValue() : 1.0;
            }
            // Sát thương kỹ năng = Sát thương vật lý cộng thêm + Sát thương ma pháp
            double skillDmg = basePhysDmg + stats.totalMagicDamage;
            return String.format("%.1f", skillDmg);
        }

        // --- %myitem_totaldamage% ---
        if (lowerParams.equals("totaldamage")) {
            // Lấy sát thương vật lý gốc (nếu không có trang bị gán mặc định là 1 hoặc lấy từ vanilla)
            double basePhysDmg = stats.totalBonusDmg;
            if (basePhysDmg <= 0) {
                var attr = p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                basePhysDmg = (attr != null) ? attr.getValue() : 1.0;
            }

            // Cộng thêm Magic Damage vào sát thương gốc
            double combinedBaseDmg = basePhysDmg + stats.totalMagicDamage;

            // Áp dụng % Tăng sát thương toàn phần (totalAllDamage)
            double finalDmg = combinedBaseDmg * (1 + stats.totalAllDamage / 100.0);

            // Cơ chế hiển thị Crit ngẫu nhiên theo chu kỳ 5 giây
            long cycle = (System.currentTimeMillis() / 1000) / 5;
            java.util.Random r = new java.util.Random(p.getUniqueId().getMostSignificantBits() + cycle);

            if (r.nextDouble() * 100 <= stats.totalCritChance) {
                double baseCritMult = Main.getInstance().getCustomConfig().getDouble("crit-multiplier", 1.5);
                double totalCritDmgMult = baseCritMult + (stats.totalCritDamage / 100.0);
                return "§f" + String.format("%.1f", finalDmg * totalCritDmgMult);
            }

            return "§f" + String.format("%.1f", finalDmg);
        }

        // --- %myitem_stats_<id>% ---
        if (lowerParams.startsWith("stats_")) {
            String statId = lowerParams.substring(6);
            return String.format("%.1f", getStatValue(stats, statId));
        }

        return null;
    }

    /**
     * Tính toán Lực Chiến tổng hợp dựa trên TOÀN BỘ chỉ số từ Vanilla, Fabled và Cache Hệ Thống
     */
    private int calculateLucChien(Player player, PlayerCombatCache.CombatStats stats) {
        // ==========================================
        // 1. CHỈ SỐ CƠ BẢN (VANILLA MINECRAFT)
        // ==========================================
        var hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double rawMaxHealth = (hpAttr != null) ? hpAttr.getValue() : 20.0;
        double effectiveHealth = Math.max(0, rawMaxHealth - 20); // Chỉ tính lượng máu cộng thêm từ trang bị/hệ thống

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
        // 3. CHỈ SỐ TỪ CACHE CUSTOM (TẤN CÔNG & PHÒNG THỦ)
        // ==========================================

        // --- NHÓM 1: SÁT THƯƠNG & KHẢ NĂNG TẤN CÔNG ---
        // Sát thương vật lý & Ma pháp (Mỗi 1 điểm = 1.87 Lực chiến)
        double attackStats = (stats.totalBonusDmg + stats.totalMagicDamage) * 1.87;

        // Sát thương đặc biệt: Sát thương cung, chuẩn, chí tử, nguyên tố (Mỗi 1 điểm = 2.0 -> 3.5 Lực chiến)
        double specialAttackStats = (stats.totalBowDamage * 2.15)
                + (stats.totalTrueDamage * 3.50)
                + (stats.totalDeathDamage * 2.50)
                + (stats.totalElementDamage * 2.00)
                + (stats.totalDeepWound * 3.00);

        // Phần trăm tăng sát thương toàn phần, PvP, PvE (Mỗi 1% = 2.87 -> 5.50 Lực chiến)
        double percentAttackStats = (stats.totalAllDamage * 5.50)
                + (stats.totalPvpBonus * 2.87)
                + (stats.totalPveBonus * 2.87);

        // Chỉ số Bạo Kích & Xuyên Giáp (Mỗi 1% / 1 điểm = hệ số rất cao)
        double critAndPenetrationStats = (stats.totalCritChance * 21.48)
                + (stats.totalCritDamage * 12.01)
                + (stats.totalPenetration * 40.00)
                + (stats.totalArmorPen * 35.00)
                + (stats.totalAccuracy * 15.00); // Độ chính xác

        // --- NHÓM 2: GIÁP & PHÒNG THỦ KHÁNG TÍNH ---
        // Chỉ số Giáp custom & Kháng phép (Mỗi 1 điểm = 25.74 -> 28.50 Lực chiến)
        double defenseStats = (stats.totalArmor * 25.74)
                + (stats.totalMagicDefense * 28.50);

        // Kháng PvP, Kháng PvE và Kháng Toàn Phần (Mỗi 1% = 3.00 -> 6.00 Lực chiến)
        double percentDefenseStats = (stats.totalAllDefense * 6.00)
                + (stats.totalPvpDef * 3.00)
                + (stats.totalPveDef * 3.00)
                + (stats.totalDamageReduction * 8.50)       // Giảm sát thương nhận vào (% rất mạnh)
                + (stats.totalCritDamageReduction * 5.00);  // Giảm sát thương bạo kích nhận vào

        // Khả năng đặc biệt: Né tránh, Đỡ đòn, Phản sát thương (Mỗi 1% = hệ số cao)
        double utilityDefenseStats = (stats.totalDodge * 37.21)
                + (stats.totalBlock * 32.21)
                + (stats.totalThorns * 18.50);

        // --- NHÓM 3: TIỆN ÍCH & HỒI PHỤC ---
        double utilityAndRegenStats = (stats.totalLifesteal * 22.50) // Hút máu %
                + (stats.totalHealthRegen * 4.50)                    // Hồi máu flat
                + (stats.totalMaxMana * 0.32)                        // Max Mana từ custom trang bị
                + (stats.totalManaRegen * 5.00)                      // Hồi mana
                + (stats.totalMovementSpeed * 3.89)                  // Tốc độ di chuyển thêm
                + (stats.totalKnockbackResist * 45.00)               // Kháng giật lùi (Giá trị gốc tối đa chỉ từ 0.0 -> 1.0)
                + (stats.totalExpBonus * 1.50);                      // % Tăng EXP nhận được

        // Tổng hợp toàn bộ lực chiến từ cache đồ đạc
        double bonusLucChien = attackStats
                + specialAttackStats
                + percentAttackStats
                + critAndPenetrationStats
                + defenseStats
                + percentDefenseStats
                + utilityDefenseStats
                + utilityAndRegenStats;

        // Trả về tổng điểm Lực Chiến (Ép về kiểu Số Nguyên int)
        return (int) (baseLucChien + bonusLucChien);
    }

    private String formatLucChien(int lucChien) {
        return NumberFormat.getInstance(Locale.US).format(lucChien);
    }

    /**
     * Map toàn bộ các ID stats thành giá trị tương ứng trong Cache gộp danh sách gợi ý
     */
    private double getStatValue(PlayerCombatCache.CombatStats stats, String id) {
        return switch (id) {
            // --- SÁT THƯƠNG & TẤN CÔNG ---
            case "damage" -> {
                double cachedDmg = stats.totalBonusDmg;
                double pctBonusDmg = stats.totalAllDamage; // Hoặc biến phần trăm sát thương tương ứng trong hệ thống của bạn
                if (pctBonusDmg != 0.0) {
                    double originalFlatDmg = ((cachedDmg + 1.0) / (1.0 + (pctBonusDmg / 100.0))) - 1.0;
                    yield originalFlatDmg + 1.0;
                }
                yield cachedDmg + 1.0;
            }
            case "pve_damage" -> stats.totalPveBonus;
            case "pvp_damage" -> stats.totalPvpBonus;
            case "all_damage" -> stats.totalAllDamage;
            case "magic_damage" -> stats.totalMagicDamage;
            case "bow_damage" -> stats.totalBowDamage;
            case "true_damage" -> stats.totalTrueDamage;
            case "death_damage" -> stats.totalDeathDamage;

            // --- BẠO KÍCH ---
            case "crit_chance", "critical_chance" -> stats.totalCritChance;
            case "crit_damage", "critical_damage" -> stats.totalCritDamage;
            case "critical_damage_reduction" -> stats.totalCritDamageReduction;

            // --- PHÒNG THỦ & KHÁNG SÁT THƯƠNG ---
            case "armor" -> stats.totalArmor;
            case "magic_defense" -> stats.totalMagicDefense;
            case "pve_defense", "pve_def" -> stats.totalPveDef;
            case "pvp_defense", "pvp_def" -> stats.totalPvpDef;
            case "all_defense" -> stats.totalAllDefense;
            case "damage_reduction" -> stats.totalDamageReduction;
            case "armor_pen" -> stats.totalArmorPen;
            case "penetration" -> stats.totalPenetration;

            // --- ĐẶC TÍNH CHIẾN ĐẤU (DODGE, BLOCK, LIFE STEAL, THORN) ---
            case "dodge_rate" -> stats.totalDodge;
            case "block_rate" -> stats.totalBlock;
            case "lifesteal" -> stats.totalLifesteal;
            case "thorns" -> stats.totalThorns;
            case "accuracy" -> stats.totalAccuracy;
            case "knockback_resistance" -> stats.totalKnockbackResist;

            // --- TỐC ĐỘ, ĐA DỤNG VÀ HỒI PHỤC ---
            case "movement_speed" -> stats.totalMovementSpeed;
            case "exp_bonus" -> stats.totalExpBonus;
            case "max_mana" -> stats.totalMaxMana;
            case "mana_regen" -> stats.totalManaRegen;
            case "health_regen" -> stats.totalHealthRegen;

            // Mặc định trả về 0.0 nếu ghi sai ID hoặc các stats hệ thống chưa phân tích (như yêu cầu level/class)
            default -> 0.0;
        };
    }
}