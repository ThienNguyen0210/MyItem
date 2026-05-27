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
        return "1.6.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "0.0";
        Player p = player.getPlayer();
        if (p == null) return "0.0";

        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(p.getUniqueId());

        // --- %myitem_lucchien% ---
        if (params.equalsIgnoreCase("lucchien")) {
            return formatLucChien(calculateLucChien(p, stats));
        }

        // --- %myitem_stats_<id>% ---
        if (params.toLowerCase().startsWith("stats_")) {
            String statId = params.substring(6).toLowerCase();
            return String.format("%.1f", getStatValue(stats, statId));
        }

        // --- %myitem_totaldamage% ---
        if (params.equalsIgnoreCase("totaldamage")) {
            // Lấy sát thương vật lý gốc (nếu không có trang bị thì lấy chỉ số vanilla)
            double basePhysDmg = stats.totalBonusDmg;
            if (basePhysDmg <= 0) {
                basePhysDmg = p.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
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

        return null;
    }

    /**
     * Tính toán Lực Chiến dựa trên logic từ class LucChien cũ
     */
    private int calculateLucChien(Player player, PlayerCombatCache.CombatStats stats) {
        // 1. Chỉ số cơ bản (Vanilla)
        double rawMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double effectiveHealth = Math.max(0, rawMaxHealth - 20);

        double baseAttackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        double vanillaArmor = player.getAttribute(Attribute.GENERIC_ARMOR).getValue();

        // 2. Chỉ số từ Fabled
        String manaStr = PlaceholderAPI.setPlaceholders(player, "%fabled_default_currentmaxmana%");
        double mana = 0;
        try { mana = Double.parseDouble(manaStr); } catch (Exception ignored) {}
        double effectiveMana = Math.max(0, mana - 200);

        String levelStr = PlaceholderAPI.setPlaceholders(player, "%fabled_default_currentlevel%");
        double level = 0;
        try { level = Double.parseDouble(levelStr); } catch (Exception ignored) {}

        double walkSpeedPercent = (player.getWalkSpeed() / 0.2) * 100;

        // Tính Base Lực Chiến
        double baseLucChien = (effectiveHealth * 0.37)
                + (baseAttackDamage * 1.87)
                + (vanillaArmor * 31.74)
                + (effectiveMana * 0.32)
                + (walkSpeedPercent * 3.89)
                + (level * 0.05);

        // 3. Chỉ số từ Trang bị & Tiềm năng (Lấy từ Cache stats)
        // Lưu ý: Đã bao gồm cả Magic Damage vào tính toán Lực chiến (hệ số tương đương Physical)
        double bonusLucChien = ((stats.totalBonusDmg + stats.totalMagicDamage) * 1.87)
                + (stats.totalCritChance * 21.48)
                + (stats.totalCritDamage * 12.01)
                + (stats.totalArmor * 25.74)
                + (stats.totalPvpBonus * 2.87)
                + (stats.totalPveBonus * 2.87)
                + (stats.totalPvpDef * 3.00)
                + (stats.totalPveDef * 3.00)
                + (stats.totalDodge * 37.21)
                + (stats.totalBlock * 32.21)
                + (stats.totalPenetration * 40.00);

        return (int) (baseLucChien + bonusLucChien);
    }

    private String formatLucChien(int lucChien) {
        return NumberFormat.getInstance(Locale.US).format(lucChien);
    }

    private double getStatValue(PlayerCombatCache.CombatStats stats, String id) {
        return switch (id) {
            case "damage" -> stats.totalBonusDmg;
            case "pve_damage" -> stats.totalPveBonus;
            case "pvp_damage" -> stats.totalPvpBonus;
            case "all_damage" -> stats.totalAllDamage;
            case "magic_damage" -> stats.totalMagicDamage;
            case "crit_chance", "critical_chance" -> stats.totalCritChance;
            case "crit_damage", "critical_damage" -> stats.totalCritDamage;
            case "armor" -> stats.totalArmor;
            case "dodge_rate" -> stats.totalDodge;
            case "block_rate" -> stats.totalBlock;
            case "penetration" -> stats.totalPenetration;
            case "true_damage" -> stats.totalTrueDamage;
            case "movement_speed" -> stats.totalMovementSpeed;
            case "exp_bonus" -> stats.totalExpBonus;

            default -> 0.0;
        };
    }
}