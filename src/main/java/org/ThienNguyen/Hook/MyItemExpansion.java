package org.ThienNguyen.Hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.ThienNguyen.Main;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class MyItemExpansion extends PlaceholderExpansion {

    @Override public @NotNull String getAuthor() { return "ThienNguyen"; }
    // Bạn có thể đổi identifier thành windycore nếu muốn dùng %windycore_...%
    @Override public @NotNull String getIdentifier() { return "windycore"; }
    @Override public @NotNull String getVersion() { return "1.3.0"; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "0.0";
        Player p = player.getPlayer();
        if (p == null) return "0.0";

        // --- %windycore_totaldamage% ---
        if (params.equalsIgnoreCase("totaldamage")) {
            PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(p.getUniqueId());

            double baseDmg = stats.totalBonusDmg;
            if (baseDmg <= 0) {
                baseDmg = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).getValue();
            }

            double finalDmg = baseDmg * (1 + stats.totalAllDamage / 100.0);

            // CƠ CHẾ MỚI: Dùng giây thực tế để tạo ra sự khác biệt
            // Thay vì dùng Random phức tạp, ta dùng modulo thời gian để "quay số"
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            long cycle = currentTimeSeconds / 5; // Chu kỳ 5 giây đổi 1 lần

            // Tạo ra một con số may mắn từ 0-99 dựa trên seed
            // Kết hợp UUID và cycle để mỗi người mỗi khác
            java.util.Random r = new java.util.Random(p.getUniqueId().getMostSignificantBits() + cycle);
            double luckScore = r.nextDouble() * 100;

            // Kiểm tra tỉ lệ chí mạng
            if (luckScore <= stats.totalCritChance) {
                double baseCritMult = Main.getInstance().getCustomConfig().getDouble("crit-multiplier", 1.5);
                double totalCritDmgMult = baseCritMult + (stats.totalCritDamage / 100.0);

                // Trả về màu ĐỎ để Thiên biết là ĐÃ CRIT
                return "§f" + String.format("%.1f", finalDmg * totalCritDmgMult);
            }

            // Trả về màu trắng nếu không Crit
            return "§f" + String.format("%.1f", finalDmg);
        }

        // Giữ lại các placeholder cũ nếu bạn vẫn cần dùng
        if (params.equalsIgnoreCase("attack") || params.equalsIgnoreCase("total_attack")) {
            if (p.hasMetadata("VALUE_FINAL_DAMAGE")) {
                double lastDmg = p.getMetadata("VALUE_FINAL_DAMAGE").get(0).asDouble();
                return String.format("%.1f", lastDmg);
            }
            return "1.0";
        }

        return null;
    }
}