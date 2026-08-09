package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class LightningMechanic implements PassiveMechanic {

    private enum Target { ACTOR, VICTIM }

    private final Target target;
    private final double damage;
    private final boolean causesBurning;
    private final int fireTicks;
    private final boolean visualOnly;

    public LightningMechanic(ConfigurationSection section) {
        this.target        = parseTarget(section.getString("target", "VICTIM"));
        this.damage         = section.getDouble("damage", 0.0);
        this.causesBurning  = section.getBoolean("causes-burning", false);
        this.fireTicks      = Math.max(0, section.getInt("fire-ticks", 100));
        this.visualOnly     = section.getBoolean("visual-only", false);
    }

    private Target parseTarget(String raw) {
        try {
            return Target.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Target.VICTIM;
        }
    }

    @Override
    public boolean execute(PassiveContext ctx) {
        LivingEntity entity = resolveTarget(ctx);
        if (entity == null) return false;

        World world = entity.getWorld();
        Location loc = entity.getLocation();

        if (visualOnly) {
            
            world.strikeLightningEffect(loc);
        } else {
            
            world.strikeLightning(loc);
        }

        if (damage > 0) {
            entity.damage(damage);
        }

        if (causesBurning) {
            entity.setFireTicks(Math.max(entity.getFireTicks(), fireTicks));
        } else {
            
            
            entity.setFireTicks(0);
        }

        return true;
    }

    private LivingEntity resolveTarget(PassiveContext ctx) {
        return switch (target) {
            case ACTOR -> ctx.getActor();
            case VICTIM -> {
                LivingEntity victim = ctx.getVictim();
                yield victim != null ? victim : ctx.getActor();
            }
        };
    }
}