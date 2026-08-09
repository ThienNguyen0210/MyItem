package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Đốt lửa lên target với damage/giây và thời gian cháy tuỳ chỉnh.
 *
 * yml:
 * - type: FLAME
 *   target: VICTIM
 *   damage-per-second: "%player_level% * 1.5"   # số, biểu thức, hoặc placeholder PAPI
 *   duration-seconds: "5"                          # tương tự
 *   visual-fire: true
 *
 * Cả "damage-per-second" và "duration-seconds" resolve lúc execute() để phản ánh
 * đúng stat actor tại thời điểm trigger (không phải lúc load config).
 */
public class FlameMechanic extends AbstractMechanic {

    private final String rawDamagePerSecond;
    private final String rawDurationSeconds;
    private final boolean visualFire;

    public FlameMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawDamagePerSecond = cfg.getString("damage-per-second", "0");
        this.rawDurationSeconds = cfg.getString("duration-seconds",  "5");
        this.visualFire         = cfg.getBoolean("visual-fire", true);
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity target = resolveTarget(ctx);
        if (target == null || target.isDead() || !target.isValid()) return false;

        double damagePerSecond = ExpressionResolver.resolve(rawDamagePerSecond, ctx.getActor(), 0);
        int    durationSeconds = ExpressionResolver.resolveInt(rawDurationSeconds, ctx.getActor(), 5);

        if (damagePerSecond <= 0 || durationSeconds <= 0) return false;

        if (visualFire) target.setFireTicks(durationSeconds * 20);

        Player damager = ctx.getActor();

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (target.isDead() || !target.isValid() || ticksElapsed >= durationSeconds) {
                    cancel();
                    return;
                }
                target.setMetadata("SKILL_DAMAGE_PROCESSED", new FixedMetadataValue(Main.getInstance(), true));
                target.setMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE, new FixedMetadataValue(Main.getInstance(), true));
                try {
                    if (damager != null) target.damage(damagePerSecond, damager);
                    else                 target.damage(damagePerSecond);
                } finally {
                    target.removeMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE, Main.getInstance());
                    target.removeMetadata("SKILL_DAMAGE_PROCESSED", Main.getInstance());
                }
                ticksElapsed++;
            }
        }.runTaskTimer(Main.getInstance(), 20L, 20L);

        return true;
    }
}