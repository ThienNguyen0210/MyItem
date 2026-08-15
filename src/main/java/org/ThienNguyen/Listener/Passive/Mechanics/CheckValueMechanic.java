package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.MechanicRegistry;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CheckValueMechanic extends AbstractMechanic {

    private enum Operator { GTE, GT, LTE, LT, EQ, NEQ }

    private final String key;
    private final String rawThreshold;
    private final Operator operator;
    private final boolean consume;
    private final String targetKeyRaw;
    private final List<PassiveMechanic> children;

    public CheckValueMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.key          = cfg.getString("key", "");

        this.rawThreshold = cfg.getString("value", "1");
        this.operator     = parseOperator(cfg.getString("operator", ">="));
        this.consume      = cfg.getBoolean("consume", false);
        this.targetKeyRaw = cfg.getString("target", "VICTIM").toUpperCase();
        this.children     = parseChildren(cfg);
    }

    private Operator parseOperator(String raw) {
        return switch (raw.trim()) {
            case ">="       -> Operator.GTE;
            case ">"        -> Operator.GT;
            case "<="       -> Operator.LTE;
            case "<"        -> Operator.LT;
            case "==", "="  -> Operator.EQ;
            case "!="       -> Operator.NEQ;
            default          -> Operator.GTE;
        };
    }


    private List<PassiveMechanic> parseChildren(ConfigurationSection cfg) {
        List<?> rawList = cfg.getList("actions");
        List<PassiveMechanic> result = new ArrayList<>();
        if (rawList == null) return result;

        for (Object obj : rawList) {
            ConfigurationSection childCfg = null;

            if (obj instanceof ConfigurationSection section) {
                childCfg = section;
            } else if (obj instanceof Map<?, ?> map) {
                MemoryConfiguration mem = new MemoryConfiguration();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    mem.set(String.valueOf(e.getKey()), e.getValue());
                }
                childCfg = mem;
            }

            if (childCfg == null) {
                Main.getInstance().getLogger()
                        .warning("[Passive] CHECK_VALUE 'actions': 1 entry không phải ConfigurationSection lẫn Map (obj class = "
                                + (obj == null ? "null" : obj.getClass().getName()) + ") → bỏ qua.");
                continue;
            }

            String childType = childCfg.getString("type", "?");
            PassiveMechanic m = MechanicRegistry.create(childCfg);
            if (m == null) {
                Main.getInstance().getLogger()
                        .warning("[Passive] CHECK_VALUE 'actions': MechanicRegistry.create() trả về NULL cho type '"
                                + childType + "' → type này có thể chưa được đăng ký, hoặc config thiếu field bắt buộc.");
                continue;
            }
            result.add(m);
        }
        return result;
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (key.isEmpty()) return false;

        LivingEntity target = resolveValueTarget(ctx);
        if (target == null) return false;

        String stored = PlayerValueStore.get(target.getUniqueId(), key);
        if (stored == null) return false;

        Player actor = ctx.getActor();
        double current   = parseNumber(stored);
        double threshold = ExpressionResolver.resolve(rawThreshold, actor, 1);

        if (!compare(current, threshold)) return false;

        if (consume) PlayerValueStore.remove(target.getUniqueId(), key);

        if (children.isEmpty()) return true;

        boolean anySuccess = false;
        for (PassiveMechanic m : children) {
            if (m.execute(ctx)) anySuccess = true;
        }
        return anySuccess;
    }

    private boolean compare(double current, double threshold) {
        return switch (operator) {
            case GTE -> current >= threshold;
            case GT  -> current > threshold;
            case LTE -> current <= threshold;
            case LT  -> current < threshold;
            case EQ  -> current == threshold;
            case NEQ -> current != threshold;
        };
    }

    private double parseNumber(String raw) {
        if (raw == null) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private LivingEntity resolveValueTarget(PassiveContext ctx) {
        return switch (targetKeyRaw) {
            case "ACTOR", "SELF" -> ctx.getActor();
            case "VICTIM"        -> ctx.getVictim();
            default               -> ctx.getActor();
        };
    }
}