package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Listener.Passive.PlayerAware;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class HitCounterMechanic implements PassiveMechanic, PlayerAware {

    
    private final Map<UUID, Integer> hitMap = new ConcurrentHashMap<>();

    private final String rawEvery;
    private final String rawResetAfter;
    private final List<PassiveMechanic> children;

    public HitCounterMechanic(ConfigurationSection cfg) {
        this.rawEvery      = cfg.getString("every",       "5");
        this.rawResetAfter = cfg.getString("reset-after", rawEvery); 
        this.children      = MechanicChildrenParser.parse(cfg, "children");
    }

    @Override
    public boolean execute(PassiveContext ctx) {
        if (children.isEmpty()) return false;

        UUID id = ctx.getActor().getUniqueId();

        int every      = ExpressionResolver.resolveInt(rawEvery,      ctx.getActor(), 5);
        int resetAfter = ExpressionResolver.resolveInt(rawResetAfter, ctx.getActor(), every);
        if (every < 1)      every      = 1;
        if (resetAfter < 1) resetAfter = every;

        
        int current = hitMap.merge(id, 1, Integer::sum);

        
        if (current >= resetAfter) {
            hitMap.put(id, 0);
        }

        
        if (current % every != 0) return false;

        
        boolean anySuccess = false;
        for (PassiveMechanic m : children) {
            if (m.execute(ctx)) anySuccess = true;
        }
        return anySuccess;
    }

    
    @Override
    public void onPlayerQuit(UUID playerId) {
        hitMap.remove(playerId);
    }

    
    public int getCount(UUID playerId) {
        return hitMap.getOrDefault(playerId, 0);
    }
}