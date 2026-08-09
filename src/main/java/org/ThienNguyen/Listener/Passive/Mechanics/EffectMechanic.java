package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class EffectMechanic extends AbstractMechanic {

    private final PotionEffectType effectType;
    private final String rawSeconds;
    private final String rawLevel;
    private final boolean ambient;
    private final boolean particles;

    public EffectMechanic(ConfigurationSection cfg) {
        super(cfg);
        String effectName = cfg.getString("effect", "").toUpperCase();
        this.effectType = PotionEffectType.getByName(effectName);

        this.rawSeconds  = cfg.getString("seconds", "5");
        this.rawLevel    = cfg.getString("level",   "1");
        this.ambient     = cfg.getBoolean("ambient",    false);
        this.particles   = cfg.getBoolean("particles",  true);
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (effectType == null) return false;

        LivingEntity entity = resolveTarget(ctx);
        if (entity == null || entity.isDead() || !entity.isValid()) return false;

        int seconds      = ExpressionResolver.resolveInt(rawSeconds, ctx.getActor(), 5);
        int level        = ExpressionResolver.resolveInt(rawLevel,   ctx.getActor(), 1);
        int durationTicks = Math.max(1, seconds * 20);
        int amplifier    = Math.max(0, level - 1);

        entity.addPotionEffect(new PotionEffect(effectType, durationTicks, amplifier, ambient, particles));
        return true;
    }
}