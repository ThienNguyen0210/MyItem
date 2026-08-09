package org.ThienNguyen.Listener.Passive;

import org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Đại diện 1 passive được đọc từ <id>.yml.
 */
public class PassiveDef {

    private final String id;
    private final String displayName;
    private final PassiveTrigger trigger;
    private final int chance;          // 0-100
    private final int cooldownSeconds;
    private final List<PassiveMechanic> mechanics;

    // Conditions
    private final double targetHpPercentBelow; // -1 = không check
    private final boolean mustBeCrit;
    private final TargetType targetType;

    private PassiveDef(Builder b) {
        this.id                    = b.id;
        this.displayName           = b.displayName;
        this.trigger               = b.trigger;
        this.chance                = b.chance;
        this.cooldownSeconds       = b.cooldownSeconds;
        this.mechanics             = b.mechanics;
        this.targetHpPercentBelow  = b.targetHpPercentBelow;
        this.mustBeCrit            = b.mustBeCrit;
        this.targetType            = b.targetType;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getId()                     { return id; }
    public String getDisplayName()            { return displayName; }
    public PassiveTrigger getTrigger()        { return trigger; }
    public int getChance()                    { return chance; }
    public int getCooldownSeconds()           { return cooldownSeconds; }
    public List<PassiveMechanic> getMechanics() { return mechanics; }
    public double getTargetHpPercentBelow()   { return targetHpPercentBelow; }
    public boolean isMustBeCrit()             { return mustBeCrit; }
    public TargetType getTargetType()         { return targetType; }

    // ── Factory ──────────────────────────────────────────────────────────────
    /**
     * Đọc 1 file yml và trả về PassiveDef.
     * Trả null + log warning nếu yml thiếu key bắt buộc.
     *
     * Hỗ trợ ON_DEATH tự động: vì trigger chỉ đọc qua PassiveTrigger.valueOf(triggerStr),
     * thêm ON_DEATH vào enum là đủ, không cần sửa gì ở đây.
     */
    public static PassiveDef fromYaml(YamlConfiguration cfg, String fileName) {
        Builder b = new Builder();
        b.id = cfg.getString("id", "");
        if (b.id.isEmpty()) {
            org.ThienNguyen.Main.getInstance().getLogger()
                    .warning("[Passive] File " + fileName + " thiếu key 'id', bỏ qua.");
            return null;
        }

        b.displayName     = cfg.getString("display-name", b.id);
        b.chance          = cfg.getInt("chance", 100);
        b.cooldownSeconds = cfg.getInt("cooldown", 0);

        String triggerStr = cfg.getString("trigger", "").toUpperCase();
        try {
            b.trigger = PassiveTrigger.valueOf(triggerStr);
        } catch (IllegalArgumentException e) {
            org.ThienNguyen.Main.getInstance().getLogger()
                    .warning("[Passive] File " + fileName + " trigger không hợp lệ: " + triggerStr);
            return null;
        }

        // Conditions
        ConfigurationSection cond = cfg.getConfigurationSection("condition");
        b.targetHpPercentBelow = cond != null ? cond.getDouble("target-hp-percent-below", -1) : -1;
        b.mustBeCrit           = cond != null && cond.getBoolean("must-be-crit", false);

        // Đọc target-type từ config (Mặc định là BOTH nếu không điền hoặc rỗng)
        String targetTypeStr = cond != null ? cond.getString("target-type", "BOTH").toUpperCase() : "BOTH";
        try {
            b.targetType = TargetType.valueOf(targetTypeStr);
        } catch (IllegalArgumentException e) {
            org.ThienNguyen.Main.getInstance().getLogger()
                    .warning("[Passive] File " + fileName + " target-type không hợp lệ: " + targetTypeStr + ". Trở về BOTH.");
            b.targetType = TargetType.BOTH;
        }

        // Actions → Mechanics
        b.mechanics = new ArrayList<>();

        // Cách đọc List Map chuẩn của Bukkit/Spigot Configuration
        List<?> actionList = cfg.getList("actions");
        if (actionList != null) {
            for (Object obj : actionList) {
                if (obj instanceof ConfigurationSection) {
                    PassiveMechanic m = MechanicRegistry.create((ConfigurationSection) obj);
                    if (m != null) b.mechanics.add(m);
                } else if (obj instanceof Map) {
                    // Trường hợp danh sách được bóc tách ra thành LinkedHashMap trong Memory
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    ConfigurationSection actionCfg = new org.bukkit.configuration.MemoryConfiguration();
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        actionCfg.set(entry.getKey(), entry.getValue());
                    }
                    PassiveMechanic m = MechanicRegistry.create(actionCfg);
                    if (m != null) b.mechanics.add(m);
                }
            }
        }

        return new PassiveDef(b);
    }

    // ── Builder (internal) ───────────────────────────────────────────────────
    private static class Builder {
        String id, displayName;
        PassiveTrigger trigger;
        int chance, cooldownSeconds;
        List<PassiveMechanic> mechanics;
        double targetHpPercentBelow = -1;
        boolean mustBeCrit = false;
        TargetType targetType = TargetType.BOTH;
    }
}