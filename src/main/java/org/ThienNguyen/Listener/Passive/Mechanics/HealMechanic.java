package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;


public class HealMechanic extends AbstractMechanic {

    private final String rawFlat;
    private final String rawPercent;

    public HealMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawFlat    = cfg.getString("amount",  "0");
        this.rawPercent = cfg.getString("percent", "0");
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity entity = resolveTarget(ctx);
        if (entity == null || entity.isDead() || !entity.isValid()) return false;
        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) == null) return false;

        double maxHp    = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHp = entity.getHealth();
        if (currentHp >= maxHp) return false;

        double percent = ExpressionResolver.resolve(rawPercent, ctx.getActor(), 0);
        double flat    = ExpressionResolver.resolve(rawFlat,    ctx.getActor(), 0);
        double heal    = percent > 0 ? maxHp * percent / 100.0 : flat;
        if (heal <= 0) return false;

        double newHp = Math.min(currentHp + heal, maxHp);
        entity.setHealth(newHp);
        return newHp > currentHp;
    }
}