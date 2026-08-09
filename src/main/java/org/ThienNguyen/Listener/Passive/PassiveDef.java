package org.ThienNguyen.Listener.Passive;

import org.ThienNguyen.Listener.Passive.Trigger.PassiveTrigger;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class PassiveDef {

    private final String id;
    private final String displayName;
    private final PassiveTrigger trigger;
    private final String rawChance;       
    private final int cooldownSeconds;
    private final List<PassiveMechanic> mechanics;

    
    private final String rawTargetHpPercentBelow; 
    private final boolean mustBeCrit;
    private final TargetType targetType;
    private final List<String> conditionExpressions;

    private PassiveDef(Builder b) {
        this.id                      = b.id;
        this.displayName             = b.displayName;
        this.trigger                 = b.trigger;
        this.rawChance               = b.rawChance;
        this.cooldownSeconds         = b.cooldownSeconds;
        this.mechanics               = b.mechanics;
        this.rawTargetHpPercentBelow = b.rawTargetHpPercentBelow;
        this.mustBeCrit              = b.mustBeCrit;
        this.targetType              = b.targetType;
        this.conditionExpressions    = b.conditionExpressions;
    }

    

    public String getId()                       { return id; }
    public String getDisplayName()              { return displayName; }
    public PassiveTrigger getTrigger()          { return trigger; }
    public int getCooldownSeconds()             { return cooldownSeconds; }
    public List<PassiveMechanic> getMechanics() { return mechanics; }
    public TargetType getTargetType()           { return targetType; }
    public boolean isMustBeCrit()               { return mustBeCrit; }

    
    public int getChance(PassiveContext ctx) {
        int resolved = ExpressionResolver.resolveInt(rawChance, ctx.getActor(), 100);
        return Math.max(0, Math.min(100, resolved));
    }

    

    
    public boolean checkConditions(PassiveContext ctx, boolean isCrit) {
        Player actor  = ctx.getActor();
        LivingEntity victim = ctx.getVictim();

        
        if (targetType == TargetType.SELF) {
            
            return victim != null && victim.equals(actor);
        }
        if (victim != null && targetType != TargetType.BOTH) {
            boolean victimIsPlayer = victim instanceof Player;
            if (targetType == TargetType.PLAYER && !victimIsPlayer) return false;
            if (targetType == TargetType.MOB    &&  victimIsPlayer) return false;
        }

        
        if (mustBeCrit && !isCrit) return false;

        
        if (!"-1".equals(rawTargetHpPercentBelow.trim())) {
            double threshold = ExpressionResolver.resolve(rawTargetHpPercentBelow, actor, -1);
            if (threshold >= 0 && victim != null) {
                var maxHpAttr = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (maxHpAttr != null) {
                    double hpPercent = (victim.getHealth() / maxHpAttr.getValue()) * 100.0;
                    if (hpPercent >= threshold) return false;
                }
            }
        }

        
        for (String expr : conditionExpressions) {
            if (!evalConditionExpression(expr, ctx)) return false;
        }

        return true;
    }

    
    public boolean checkConditions(PassiveContext ctx) {
        return checkConditions(ctx, false);
    }

    

    
    private static boolean evalConditionExpression(String raw, PassiveContext ctx) {
        if (raw == null || raw.isBlank()) return true;

        String expr = substitutePlaceholders(raw, ctx);

        
        String[] operators = { ">=", "<=", "==", "!=", ">", "<" };
        for (String op : operators) {
            int idx = expr.indexOf(op);
            if (idx <= 0) continue;

            String leftRaw  = expr.substring(0, idx).trim();
            String rightRaw = expr.substring(idx + op.length()).trim();
            if (leftRaw.isEmpty() || rightRaw.isEmpty()) continue;

            
            double left  = ExpressionResolver.resolve(leftRaw,  ctx.getActor(), 0);
            double right = ExpressionResolver.resolve(rightRaw, ctx.getActor(), 0);

            return switch (op) {
                case ">=" -> left >= right;
                case "<=" -> left <= right;
                case "==" -> left == right;
                case "!=" -> left != right;
                case ">"  -> left >  right;
                case "<"  -> left <  right;
                default   -> false;
            };
        }

        org.ThienNguyen.Main.getInstance().getLogger()
                .warning("[Passive] condition.expressions: không tìm thấy toán tử trong \"" + raw + "\"");
        return false;
    }

    
    public static String substitutePlaceholders(String expr, PassiveContext ctx) {
        expr = expr.replace("{damage}", String.valueOf(ctx.getDamage()));

        Player actor = ctx.getActor();
        if (actor != null) {
            expr = expr.replace("{actor_level}", String.valueOf(actor.getLevel()));
            expr = expr.replace("{actor_hp}",    String.valueOf(actor.getHealth()));

            var actorMaxHpAttr = actor.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (actorMaxHpAttr != null) {
                double maxHp = actorMaxHpAttr.getValue();
                expr = expr.replace("{actor_max_hp}", String.valueOf(maxHp));

                
                double missingHp = Math.max(0, maxHp - actor.getHealth());
                expr = expr.replace("{actor_missing_hp}", String.valueOf(missingHp));
            }
        }

        LivingEntity victim = ctx.getVictim();
        if (victim != null) {
            expr = expr.replace("{victim_hp}", String.valueOf(victim.getHealth()));
            var maxHpAttr = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHpAttr != null) {
                double maxHp = maxHpAttr.getValue();
                expr = expr.replace("{victim_max_hp}", String.valueOf(maxHp));

                double pct = (victim.getHealth() / maxHp) * 100.0;
                expr = expr.replace("{victim_hp_percent}", String.valueOf(pct));

                
                double missingHp = Math.max(0, maxHp - victim.getHealth());
                expr = expr.replace("{victim_missing_hp}", String.valueOf(missingHp));
            }
        }

        return expr;
    }

    
    private static void convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map) {
                convertMapsToSections((Map<?, ?>) value, section.createSection(key));
            } else {
                section.set(key, value);
            }
        }
    }

    

    public static PassiveDef fromYaml(YamlConfiguration cfg, String fileName) {
        Builder b = new Builder();
        b.id = cfg.getString("id", "");
        if (b.id.isEmpty()) {
            org.ThienNguyen.Main.getInstance().getLogger()
                    .warning("[Passive] File " + fileName + " thiếu key 'id', bỏ qua.");
            return null;
        }

        b.displayName     = cfg.getString("display-name", b.id);
        b.rawChance       = cfg.getString("chance", "100");
        b.cooldownSeconds = cfg.getInt("cooldown", 0);

        String triggerStr = cfg.getString("trigger", "").toUpperCase();
        try {
            b.trigger = PassiveTrigger.valueOf(triggerStr);
        } catch (IllegalArgumentException e) {
            org.ThienNguyen.Main.getInstance().getLogger()
                    .warning("[Passive] File " + fileName + " trigger không hợp lệ: " + triggerStr);
            return null;
        }

        
        ConfigurationSection cond = cfg.getConfigurationSection("condition");

        b.rawTargetHpPercentBelow = cond != null
                ? cond.getString("target-hp-percent-below", "-1")
                : "-1";

        b.mustBeCrit = cond != null && cond.getBoolean("must-be-crit", false);

        String targetTypeStr = cond != null
                ? cond.getString("target-type", "BOTH").toUpperCase()
                : "BOTH";
        try {
            b.targetType = TargetType.valueOf(targetTypeStr);
        } catch (IllegalArgumentException e) {
            org.ThienNguyen.Main.getInstance().getLogger()
                    .warning("[Passive] File " + fileName + " target-type không hợp lệ: "
                            + targetTypeStr + ". Trở về BOTH.");
            b.targetType = TargetType.BOTH;
        }

        b.conditionExpressions = new ArrayList<>();
        if (cond != null) {
            List<?> exprList = cond.getList("expressions");
            if (exprList != null) {
                for (Object obj : exprList) {
                    if (obj instanceof String s && !s.isBlank()) {
                        b.conditionExpressions.add(s.trim());
                    }
                }
            }
        }

        
        b.mechanics = new ArrayList<>();
        List<?> actionList = cfg.getList("actions");
        if (actionList != null) {
            for (Object obj : actionList) {
                if (obj instanceof ConfigurationSection cs) {
                    PassiveMechanic m = MechanicRegistry.create(cs);
                    if (m != null) b.mechanics.add(m);
                } else if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    ConfigurationSection actionCfg = new org.bukkit.configuration.MemoryConfiguration();
                    convertMapsToSections(map, actionCfg);
                    PassiveMechanic m = MechanicRegistry.create(actionCfg);
                    if (m != null) b.mechanics.add(m);
                }
            }
        }

        return new PassiveDef(b);
    }

    
    private static class Builder {
        String id, displayName;
        PassiveTrigger trigger;
        String rawChance = "100";
        int cooldownSeconds;
        List<PassiveMechanic> mechanics;
        String rawTargetHpPercentBelow = "-1";
        boolean mustBeCrit = false;
        TargetType targetType = TargetType.BOTH;
        List<String> conditionExpressions = new ArrayList<>();
    }
}