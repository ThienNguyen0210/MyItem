package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Drop item tại vị trí target.
 *
 * yml:
 * - type: DROP_ITEM
 *   target: VICTIM
 *   material: GOLD_NUGGET
 *   amount: "1-3"                       # range "min-max", số cố định, biểu thức, hoặc placeholder PAPI
 *   amount: "%player_level% / 10"       # ví dụ: 1 nugget mỗi 10 level (làm tròn xuống)
 *
 * Với range "min-max": mỗi phần có thể là biểu thức/placeholder riêng (vd "1-%player_level%").
 * "amount" resolve lúc execute().
 */
public class DropItemMechanic extends AbstractMechanic {

    private final Material material;
    // Giữ nguyên chuỗi gốc; nếu là range sẽ chia theo "-" ở doExecute()
    private final String rawAmount;

    public DropItemMechanic(ConfigurationSection cfg) {
        super(cfg);
        String mat = cfg.getString("material", "GOLD_NUGGET").toUpperCase();
        Material matched = Material.matchMaterial(mat);
        this.material  = matched != null ? matched : Material.GOLD_NUGGET;
        this.rawAmount = cfg.getString("amount", "1");
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity ref = resolveTarget(ctx);
        if (ref == null || !ref.isValid()) return false;

        World world = ref.getWorld();
        if (world == null) return false;

        int qty = resolveAmount(rawAmount, ctx);
        if (qty <= 0) return false;

        world.dropItemNaturally(ref.getLocation(), new ItemStack(material, qty));
        return true;
    }

    /**
     * Resolve "amount" — hỗ trợ:
     *   • "5"                    → 5
     *   • "%player_level% / 5"  → expression
     *   • "1-3"                  → random [1, 3]
     *   • "1-%player_level%"     → random [1, level]
     */
    private int resolveAmount(String raw, PassiveContext ctx) {
        if (raw.contains("-")) {
            // Thử tách range (cẩn thận: expression có thể chứa dấu trừ trước số âm)
            // Heuristic: nếu có dấu '-' KHÔNG phải đầu chuỗi và không liền sau e/E → tách range
            int dashIdx = findRangeDash(raw);
            if (dashIdx > 0) {
                String minStr = raw.substring(0, dashIdx).trim();
                String maxStr = raw.substring(dashIdx + 1).trim();
                int min = ExpressionResolver.resolveInt(minStr, ctx.getActor(), 1);
                int max = ExpressionResolver.resolveInt(maxStr, ctx.getActor(), 1);
                if (min > max) { int tmp = min; min = max; max = tmp; }
                return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
            }
        }
        return ExpressionResolver.resolveInt(raw, ctx.getActor(), 1);
    }

    /**
     * Tìm vị trí dấu '-' phân cách range — bỏ qua dấu '-' ở đầu (số âm) và
     * dấu '-' bên trong expression con (vd "10-2" là range, "2+1-3" khó xác định
     * → ưu tiên xử lý như expression đơn, không phải range).
     * Logic đơn giản: tìm '-' đầu tiên ở vị trí > 0 mà ký tự trước không phải toán tử.
     */
    private int findRangeDash(String raw) {
        for (int i = 1; i < raw.length(); i++) {
            if (raw.charAt(i) == '-') {
                char prev = raw.charAt(i - 1);
                // Nếu ký tự trước là chữ số hoặc ký tự kết thúc placeholder (%), là range
                if (Character.isDigit(prev) || prev == '%' || prev == ' ') {
                    return i;
                }
            }
        }
        return -1;
    }
}