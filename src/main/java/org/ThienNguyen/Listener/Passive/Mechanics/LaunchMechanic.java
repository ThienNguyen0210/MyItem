package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Phóng entity theo vector tự do dựa theo hướng nhìn của entity tham chiếu.
 *
 * yml:
 * - type: LAUNCH
 *   target: VICTIM
 *   reference: SELF
 *   forward: "%player_level% / 20"   # số, biểu thức, hoặc placeholder PAPI
 *   side: "0"
 *   up: "0.8"
 *   reset-velocity: true
 *
 * Tất cả vector component resolve lúc execute().
 */
public class LaunchMechanic extends AbstractMechanic {

    private final String referenceKey;
    private final String rawForward;
    private final String rawSide;
    private final String rawUp;
    private final boolean resetVelocity;

    public LaunchMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.referenceKey   = cfg.getString("reference", "SELF").toUpperCase();
        this.rawForward     = cfg.getString("forward", "0");
        this.rawSide        = cfg.getString("side",    "0");
        this.rawUp          = cfg.getString("up",      "0");
        this.resetVelocity  = cfg.getBoolean("reset-velocity", true);
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity target = resolveTarget(ctx);
        if (target == null || target.isDead() || !target.isValid()) return false;

        double forward = ExpressionResolver.resolve(rawForward, ctx.getActor(), 0);
        double side    = ExpressionResolver.resolve(rawSide,    ctx.getActor(), 0);
        double up      = ExpressionResolver.resolve(rawUp,      ctx.getActor(), 0);

        if (forward == 0.0 && side == 0.0 && up == 0.0) return false;

        LivingEntity reference = "VICTIM".equals(referenceKey) ? ctx.getVictim() : ctx.getActor();
        if (reference == null) reference = target;

        float yaw    = reference.getLocation().getYaw();
        double yawRad = Math.toRadians(yaw);

        double dirX  = -Math.sin(yawRad);
        double dirZ  =  Math.cos(yawRad);
        double sideX = -Math.sin(yawRad + Math.PI / 2);
        double sideZ =  Math.cos(yawRad + Math.PI / 2);

        Vector launchVector = new Vector(
                dirX * forward + sideX * side,
                up,
                dirZ * forward + sideZ * side
        );

        if (resetVelocity) target.setVelocity(launchVector);
        else               target.setVelocity(target.getVelocity().add(launchVector));

        return true;
    }
}