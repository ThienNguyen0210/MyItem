package org.ThienNguyen.Listener.Passive;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;


public abstract class AbstractMechanic implements PassiveMechanic {

    
    protected final String targetKey;

    private final List<PassiveMechanic> children;

    protected AbstractMechanic(ConfigurationSection cfg) {
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

    
    protected LivingEntity resolveTarget(PassiveContext ctx) {
        return "SELF".equals(targetKey) ? ctx.getActor() : ctx.getVictim();
    }

    
    protected abstract boolean doExecute(PassiveContext ctx);

    @Override
    public final boolean execute(PassiveContext ctx) {
        boolean success = doExecute(ctx);
        if (success && !children.isEmpty()) {
            for (PassiveMechanic child : children) {
                child.execute(ctx);
                
                
                
            }
        }
        return success;
    }

    
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