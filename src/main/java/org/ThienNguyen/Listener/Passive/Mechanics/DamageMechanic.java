package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveDef;
import org.ThienNguyen.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class DamageMechanic extends AbstractMechanic {

    public static final String META_KEY_NORMAL_SOURCE = "PASSIVE_NORMAL_DAMAGE_SOURCE";

    private final String rawAmount;
    private final String damageType;

    public DamageMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawAmount  = cfg.getString("amount", "0");
        this.damageType = cfg.getString("damage-type", "TRUE").toUpperCase();
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity entity = resolveTarget(ctx);
        if (entity == null || entity.isDead() || !entity.isValid()) return false;
        String expr = PassiveDef.substitutePlaceholders(rawAmount, ctx);
        double amount = ExpressionResolver.resolve(expr, ctx.getActor(), 0);
        if (amount <= 0) return false;

        if ("NORMAL".equals(damageType)) {
            return doExecuteNormal(ctx, entity, amount);
        }
        return doExecuteTrue(entity, amount);
    }

    private boolean doExecuteTrue(LivingEntity entity, double amount) {
        double newHealth = Math.max(0.0, entity.getHealth() - amount);
        entity.setHealth(newHealth);

        
        
        
        
        
        entity.playHurtAnimation(0f);

        if (newHealth <= 0.0) entity.damage(0.0);
        return true;
    }

    private boolean doExecuteNormal(PassiveContext ctx, LivingEntity entity, double amount) {
        Player damager = ctx.getActor();

        
        
        
        
        
        
        
        
        
        entity.setMetadata("SKILL_DAMAGE_VALUE", new FixedMetadataValue(Main.getInstance(), amount));
        entity.setMetadata("SKILL_DAMAGE_PROCESSED", new FixedMetadataValue(Main.getInstance(), true));
        try {
            if (damager != null) entity.damage(amount, damager);
            else                 entity.damage(amount);
        } finally {
            
            entity.removeMetadata("SKILL_DAMAGE_PROCESSED", Main.getInstance());
        }
        return true;
    }
}