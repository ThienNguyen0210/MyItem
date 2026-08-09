package org.ThienNguyen.Listener.Passive;

import me.clip.placeholderapi.PlaceholderAPI;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Player;

/**
 * Tiện ích giải quyết giá trị số từ 1 chuỗi có thể chứa:
 *   • Placeholder PlaceholderAPI  (%player_level%, %player_health%, v.v.)
 *   • Biểu thức toán học          (%player_level% * 2 + 10, %player_health% / 2, v.v.)
 *   • Số nguyên / thực thông thường  (100, 3.5, ...)
 *
 * Cách dùng trong mechanic:
 *   double amount = ExpressionResolver.resolve(rawAmount, ctx.getActor(), fallback);
 *
 * Nếu giá trị không phân giải được (placeholder sai, chia 0, ...) thì trả về fallback.
 */
public final class ExpressionResolver {

    private ExpressionResolver() {}

    /**
     * Phân giải chuỗi thành double.
     *
     * @param raw      chuỗi gốc từ yml (ví dụ "100", "%player_level% * 2", "5.5")
     * @param player   player dùng để resolve placeholder (có thể null — nếu null, bỏ qua PAPI)
     * @param fallback giá trị mặc định nếu phân giải thất bại
     * @return giá trị double sau khi resolve + evaluate
     */
    public static double resolve(String raw, Player player, double fallback) {
        if (raw == null || raw.isBlank()) return fallback;

        String expr = raw.trim();

        // 1. Resolve PlaceholderAPI nếu có placeholder (%...%)
        if (player != null && expr.contains("%")) {
            expr = PlaceholderAPI.setPlaceholders(player, expr);
        }

        // 2. Thử parse trực tiếp thành double trước (nhanh, tránh overhead exp4j)
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException ignored) {
            // Không phải số thuần — thử evaluate như biểu thức toán học
        }

        // 3. Dùng exp4j để tính biểu thức (+, -, *, /, ^, hàm toán học cơ bản)
        try {
            Expression e = new ExpressionBuilder(expr).build();
            if (!e.validate(false).isValid()) return fallback;
            return e.evaluate();
        } catch (Exception ex) {
            return fallback;
        }
    }

    /**
     * Overload trả về int (làm tròn xuống từ double — phù hợp cho duration, level, ...).
     */
    public static int resolveInt(String raw, Player player, int fallback) {
        double val = resolve(raw, player, fallback);
        return (int) val;
    }

    /**
     * Overload cho giá trị KHÔNG cần player (hằng số hay biểu thức thuần toán, không có placeholder).
     */
    public static double resolve(String raw, double fallback) {
        return resolve(raw, null, fallback);
    }
}