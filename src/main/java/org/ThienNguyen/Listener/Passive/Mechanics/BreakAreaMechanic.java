package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Đào thêm block xung quanh block actor vừa phá (trigger ON_BLOCK_BREAK) — kiểu
 * "cuốc TNT"/"khoan vùng". KHÔNG dùng nổ vanilla (khác ExplodeMechanic) — đào trực
 * tiếp từng block bằng block.breakNaturally(tool), tự rớt item đúng như đào tay,
 * tự tôn trọng WorldGuard/claim plugin vì breakNaturally() vẫn đi qua hành vi khai
 * thác chuẩn (drop theo enchant Fortune/Silk Touch của tool nếu truyền vào).
 *
 * yml:
 * - type: BREAK_AREA
 *   size: "3"                  # kích thước cạnh vùng đào (3 = 3x3, 5 = 5x5...) — số lẻ,
 *                                  số cố định hoặc placeholder/biểu thức (xem ExpressionResolver)
 *   depth: "1"                  # độ sâu đào thêm theo trục Y (1 = chỉ đào ngang quanh block gốc)
 *   use-tool-drops: true          # true = tính rớt item theo enchant của tool actor đang cầm
 *                                    (Fortune/Silk Touch), false = rớt mặc định không enchant
 *   excluded-materials: "BEDROCK,BARRIER,END_PORTAL_FRAME,COMMAND_BLOCK"  # danh sách block
 *                                    KHÔNG được đào thêm dù nằm trong vùng (phân cách bởi dấu phẩy)
 *
 * KHÔNG đào lại chính block đã bị BlockBreakEvent xử lý (đã phá rồi, tự nhiên không còn ở đó).
 *
 * "Thành công" = đào được ít nhất 1 block trong vùng (không tính block gốc, không tính
 * block đã là AIR từ trước, không tính block trong excluded-materials).
 */
public class BreakAreaMechanic extends AbstractMechanic {

    private final String rawSize;
    private final String rawDepth;
    private final boolean useToolDrops;
    private final List<Material> excludedMaterials;

    public BreakAreaMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawSize  = cfg.getString("size",  "3");
        this.rawDepth = cfg.getString("depth", "1");
        this.useToolDrops = cfg.getBoolean("use-tool-drops", true);

        this.excludedMaterials = new ArrayList<>();
        String excludedRaw = cfg.getString("excluded-materials", "BEDROCK,BARRIER,END_PORTAL_FRAME,COMMAND_BLOCK");
        for (String name : excludedRaw.split(",")) {
            Material m = Material.matchMaterial(name.trim().toUpperCase());
            if (m != null) excludedMaterials.add(m);
        }
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        Block origin = ctx.getBrokenBlock();
        Player actor = ctx.getActor();
        if (origin == null || actor == null) return false;

        int size = (int) ExpressionResolver.resolve(rawSize, actor, 3);
        int depth = (int) ExpressionResolver.resolve(rawDepth, actor, 1);
        if (size < 1) size = 1;
        if (depth < 1) depth = 1;

        int half = size / 2; // size=3 -> half=1 (đào -1..+1 quanh tâm = đúng 3x3)

        ItemStack tool = useToolDrops ? actor.getInventory().getItemInMainHand() : null;

        boolean anySuccess = false;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dy = 0; dy < depth; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue; // bỏ qua block gốc, đã bị phá bởi BlockBreakEvent rồi

                    Block target = origin.getRelative(dx, dy, dz);
                    if (target.getType() == Material.AIR || target.getType() == Material.CAVE_AIR) continue;
                    if (excludedMaterials.contains(target.getType())) continue;

                    // breakNaturally(tool) -> tự rớt item đúng theo enchant Fortune/Silk Touch
                    // của tool (nếu use-tool-drops: true), giống hành vi đào tay thật. WorldGuard/
                    // claim plugin tự can thiệp được vì đây vẫn là hành vi phá block chuẩn Bukkit.
                    boolean broke = (tool != null) ? target.breakNaturally(tool) : target.breakNaturally();
                    if (broke) anySuccess = true;
                }
            }
        }

        return anySuccess;
    }
}