package org.ThienNguyen.Listener.Passive;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class cho mọi mechanic — xử lý 2 phần CHUNG mà mọi mechanic đều cần,
 * để class con chỉ cần lo đúng 1 việc (doExecute):
 *
 * 1. Field "target: SELF|VICTIM" — đồng nhất tên cho mọi mechanic (trước đây DropItemMechanic
 *    dùng "location" riêng, không khớp 3 mechanic khác dùng "target" — đã chuẩn hoá lại).
 * 2. "children" — list mechanic con trong yml, CHỈ chạy nối tiếp khi doExecute() trả true.
 *
 * yml mẫu (DAMAGE rồi nếu thành công thì HEAL theo sau):
 *   - type: DAMAGE
 *     target: VICTIM
 *     amount: 50
 *     children:
 *       - type: HEAL
 *         target: SELF
 *         percent: 10
 *
 * Mechanic mới: kế thừa AbstractMechanic, gọi super(cfg) trong constructor, implement doExecute().
 * KHÔNG override execute() — AbstractMechanic đã lo target-resolve + children, override sẽ làm
 * mất 2 phần đó.
 */
public abstract class AbstractMechanic implements PassiveMechanic {

    /** "SELF" hoặc "VICTIM", đọc từ key "target" (alias "location" để tương thích yml cũ). */
    protected final String targetKey;

    private final List<PassiveMechanic> children;

    protected AbstractMechanic(ConfigurationSection cfg) {
        // "location" là alias cũ (DropItemMechanic) — vẫn đọc được để không phá yml cũ đã viết,
        // nhưng "target" được ưu tiên nếu cả hai cùng xuất hiện.
        this.targetKey = cfg.getString("target", cfg.getString("location", "VICTIM")).toUpperCase();

        List<PassiveMechanic> built = new ArrayList<>();
        List<?> childList = cfg.getList("children");
        if (childList != null) {
            for (Object obj : childList) {
                ConfigurationSection childCfg = toSection(obj);
                if (childCfg == null) continue;
                PassiveMechanic child = MechanicRegistry.create(childCfg);
                if (child != null) built.add(child);
            }
        }
        this.children = built;
    }

    /** Resolve "target: SELF|VICTIM" thành entity thật từ context. */
    protected LivingEntity resolveTarget(PassiveContext ctx) {
        return "SELF".equals(targetKey) ? ctx.getActor() : ctx.getVictim();
    }

    /**
     * Class con implement đúng hành động của mechanic, trả về true/false thành công.
     * KHÔNG tự gọi children ở đây — AbstractMechanic.execute() lo phần đó.
     */
    protected abstract boolean doExecute(PassiveContext ctx);

    @Override
    public final boolean execute(PassiveContext ctx) {
        boolean success = doExecute(ctx);
        if (success && !children.isEmpty()) {
            for (PassiveMechanic child : children) {
                child.execute(ctx);
                // Lưu ý: kết quả của child KHÔNG ảnh hưởng kết quả trả về của cha —
                // cha đã "thành công" rồi, child thất bại chỉ đơn giản là child đó dừng
                // (không lan ngược lên, không chặn các child khác cùng cấp chạy tiếp).
            }
        }
        return success;
    }

    /** Hỗ trợ cả 2 dạng Bukkit hay trả về khi đọc list lồng nhau: ConfigurationSection hoặc Map. */
    @SuppressWarnings("unchecked")
    private ConfigurationSection toSection(Object obj) {
        if (obj instanceof ConfigurationSection section) {
            return section;
        }
        if (obj instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
            ConfigurationSection sec = new org.bukkit.configuration.MemoryConfiguration();
            for (java.util.Map.Entry<String, Object> e : map.entrySet()) {
                sec.set(e.getKey(), e.getValue());
            }
            return sec;
        }
        return null;
    }
}