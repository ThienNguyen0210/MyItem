package org.ThienNguyen.Listener.Passive;

import me.clip.placeholderapi.PlaceholderAPI;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Player;


public final class ExpressionResolver {

    private ExpressionResolver() {}

    
    public static double resolve(String raw, Player player, double fallback) {
        if (raw == null || raw.isBlank()) return fallback;

        String expr = raw.trim();
        if (player != null && expr.contains("%")) {
            expr = PlaceholderAPI.setPlaceholders(player, expr);
        }
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException ignored) {
        }

        try {
            Expression e = new ExpressionBuilder(expr).build();
            if (!e.validate(false).isValid()) return fallback;
            return e.evaluate();
        } catch (Exception ex) {
            return fallback;
        }
    }

    
    public static int resolveInt(String raw, Player player, int fallback) {
        double val = resolve(raw, player, fallback);
        return (int) val;
    }
}