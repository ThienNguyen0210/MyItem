package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public class ExplodeMechanic extends AbstractMechanic {

    private final String rawPower;
    private final String rawRadius;
    private final String rawAmount;
    private final String damageType;
    private final boolean includeSelf;
    private final boolean breakBlocks;
    private final String targetKeyRaw;
    private final double particleScale;

    public ExplodeMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawPower    = cfg.getString("power",  "4.0");
        this.rawRadius   = cfg.getString("radius", "4.0");
        this.rawAmount   = cfg.getString("amount", "0");
        this.damageType  = cfg.getString("damage-type", "TRUE").toUpperCase();
        this.includeSelf = cfg.getBoolean("include-self", false);
        this.breakBlocks = cfg.getBoolean("break-blocks", false);
        this.targetKeyRaw = cfg.getString("target", "VICTIM").toUpperCase();
        this.particleScale = cfg.getDouble("particle-scale", 1.0);
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        Location origin;

        if ("BLOCK".equals(targetKeyRaw)) {
            org.bukkit.block.Block block = ctx.getBrokenBlock();
            if (block == null) return false;
            origin = block.getLocation().add(0.5, 0.5, 0.5);
        } else {
            
            origin = resolveLocation(ctx);
            if (origin == null) return false;
        }

        World world = origin.getWorld();
        if (world == null) return false;

        float  power  = (float) ExpressionResolver.resolve(rawPower,  ctx.getActor(), 4.0);
        double radius = ExpressionResolver.resolve(rawRadius, ctx.getActor(), 4.0);
        double amount = ExpressionResolver.resolve(rawAmount, ctx.getActor(), 0);

        
        world.createExplosion(origin, power, false, breakBlocks);

        
        world.spawnParticle(Particle.EXPLOSION_EMITTER, origin, 1, 0, 0, 0, 0);

        int particleCount = (int) (50 * particleScale);
        double spread = 2.0 * particleScale;
        world.spawnParticle(Particle.EXPLOSION, origin, particleCount, spread, spread, spread, 0.1);

        if (amount <= 0 || radius <= 0) return true;

        
        List<LivingEntity> affected = new ArrayList<>();
        for (Entity e : world.getNearbyEntities(origin, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le) || le.isDead() || !le.isValid()) continue;
            
            if (le.equals(ctx.getActor()) && !includeSelf) continue;
            if (origin.distance(le.getLocation()) > radius) continue;
            affected.add(le);
        }

        if (affected.isEmpty()) return true;

        boolean anySuccess = false;
        for (LivingEntity target : affected) {
            if (applyExplosionDamage(ctx, target, amount)) anySuccess = true;
        }
        return anySuccess;
    }

    
    private Location resolveLocation(PassiveContext ctx) {
        return switch (targetKeyRaw) {
            case "ACTOR", "SELF" -> ctx.getActorLocation();
            case "VICTIM"        -> ctx.getVictimLocation();
            default               -> ctx.getActorLocation();
        };
    }

    private boolean applyExplosionDamage(PassiveContext ctx, LivingEntity target, double amount) {
        if (target.isDead() || !target.isValid()) return false;

        target.playEffect(org.bukkit.EntityEffect.HURT);

        if ("NORMAL".equals(damageType)) {
            Player damager = ctx.getActor();
            target.setMetadata("SKILL_DAMAGE_PROCESSED",
                    new org.bukkit.metadata.FixedMetadataValue(org.ThienNguyen.Main.getInstance(), true));
            target.setMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE,
                    new org.bukkit.metadata.FixedMetadataValue(org.ThienNguyen.Main.getInstance(), true));
            try {
                if (damager != null) target.damage(amount, damager);
                else                 target.damage(amount);
            } finally {
                target.removeMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE, org.ThienNguyen.Main.getInstance());
                target.removeMetadata("SKILL_DAMAGE_PROCESSED", org.ThienNguyen.Main.getInstance());
            }
            return true;
        }

        double newHealth = Math.max(0.0, target.getHealth() - amount);
        target.setHealth(newHealth);

        if (newHealth <= 0.0) {
            target.damage(0.0);
        }
        return true;
    }
}