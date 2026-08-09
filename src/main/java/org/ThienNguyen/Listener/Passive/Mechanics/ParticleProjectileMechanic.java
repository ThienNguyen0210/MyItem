package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ParticleProjectileMechanic extends AbstractMechanic {

    private final Particle particle;
    private final String rawParticlePerStep;
    private final String rawSpeed;
    private final String rawAmount;
    private final String damageType;
    private final String rawHitRadius;
    private final boolean hitActorSelf;

    private final String impactShape;
    private final Particle impactParticle;
    private final String rawImpactRadius;
    private final String rawImpactDurationTicks;
    private final String rawImpactPoints;
    private final String rawImpactParticlePerPoint;
    private final String rawImpactDamage;
    private final String impactDamageType;

    private final boolean immediateImpact;

    
    private final String flightShape;
    private final String rawFlightRadius;
    private final String rawFlightRotationSpeed;
    private final int flightRings;
    private final int flightPointsPerRing;
    private final boolean flightGrow;
    private final String rawFlightGrowSteps;

    
    private final boolean sweepAttack;
    private final int sweepAttackInterval;

    
    private final Color dustColor;
    private final Material blockMaterial;

    public ParticleProjectileMechanic(ConfigurationSection cfg) {
        super(cfg);

        this.particle           = parseParticle(cfg.getString("particle", "FLAME"));
        this.rawParticlePerStep = cfg.getString("particle-per-step", "1");
        this.rawSpeed           = cfg.getString("speed", "1.0");
        this.rawAmount          = cfg.getString("amount", "0");
        this.damageType         = cfg.getString("damage-type", "NORMAL").toUpperCase();
        this.rawHitRadius       = cfg.getString("hit-radius", "1.0");
        this.hitActorSelf       = cfg.getBoolean("hit-actor-self", false);

        this.impactShape        = cfg.getString("impact-shape", "NONE").toUpperCase();
        String impactParticleName = cfg.getString("impact-particle", cfg.getString("particle", "FLAME"));
        this.impactParticle = parseParticle(impactParticleName);

        this.rawImpactRadius           = cfg.getString("impact-radius", "3.0");
        this.rawImpactDurationTicks    = cfg.getString("impact-duration-ticks", "15");
        this.rawImpactPoints           = cfg.getString("impact-points", "30");
        this.rawImpactParticlePerPoint = cfg.getString("impact-particle-per-point", "2");

        this.rawImpactDamage = cfg.getString("impact-damage", this.rawAmount);
        this.impactDamageType = cfg.getString("impact-damage-type", this.damageType).toUpperCase();

        this.immediateImpact = cfg.getBoolean("immediate-impact", false);

        
        this.flightShape            = cfg.getString("flight-shape", "NONE").toUpperCase();
        this.rawFlightRadius        = cfg.getString("flight-radius", "0.45");
        this.rawFlightRotationSpeed = cfg.getString("flight-rotation-speed", "0.55");
        this.flightRings            = Math.max(1, cfg.getInt("flight-rings", 2));
        this.flightPointsPerRing    = Math.max(3, cfg.getInt("flight-points-per-ring", 6));
        this.flightGrow             = cfg.getBoolean("flight-grow", true);
        this.rawFlightGrowSteps     = cfg.getString("flight-grow-steps", "20");

        
        this.sweepAttack         = cfg.getBoolean("sweep-attack", false);
        this.sweepAttackInterval = Math.max(1, cfg.getInt("sweep-attack-interval", 4));

        
        this.dustColor = parseColor(cfg.getString("dust-color", "#FFAA00"));
        this.blockMaterial = parseMaterial(cfg.getString("block-material", "MAGMA_BLOCK"));
    }

    private Color parseColor(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (Exception e) {
            return Color.ORANGE;
        }
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Material.MAGMA_BLOCK;
        }
    }

    private void spawnParticleSafe(World world, Location loc, Particle p, int count, double ox, double oy, double oz, double extra) {
        try {
            if (p == Particle.DUST) {
                Particle.DustOptions options = new Particle.DustOptions(dustColor, 1.8f);
                world.spawnParticle(p, loc, count, ox, oy, oz, extra, options);
            } else if (p == Particle.BLOCK || p == Particle.FALLING_DUST) {
                BlockData data = blockMaterial.createBlockData();
                world.spawnParticle(p, loc, count, ox, oy, oz, extra, data);
            } else {
                world.spawnParticle(p, loc, count, ox, oy, oz, extra);
            }
        } catch (Exception e) {
            world.spawnParticle(Particle.FLAME, loc, count, ox, oy, oz, extra);
        }
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        Player actor = ctx.getActor();
        if (actor == null || !actor.isValid()) return false;

        World world = actor.getWorld();
        if (world == null) return false;

        int    particlePerStep = Math.max(1, ExpressionResolver.resolveInt(rawParticlePerStep, actor, 1));
        double speed           = ExpressionResolver.resolve(rawSpeed, actor, 1.0);
        double projectileDamage= ExpressionResolver.resolve(rawAmount, actor, 0);
        double hitRadius       = Math.max(0.3, ExpressionResolver.resolve(rawHitRadius, actor, 1.0));

        double impactRadius    = ExpressionResolver.resolve(rawImpactRadius, actor, 3.0);
        int    impactDuration  = Math.max(1, ExpressionResolver.resolveInt(rawImpactDurationTicks, actor, 15));
        int    impactPoints    = Math.max(1, ExpressionResolver.resolveInt(rawImpactPoints, actor, 30));
        int    impactPPP       = Math.max(1, ExpressionResolver.resolveInt(rawImpactParticlePerPoint, actor, 2));
        double impactDamage    = ExpressionResolver.resolve(rawImpactDamage, actor, 0);

        double flightRadius        = Math.max(0.05, ExpressionResolver.resolve(rawFlightRadius, actor, 0.45));
        double flightRotationSpeed = ExpressionResolver.resolve(rawFlightRotationSpeed, actor, 0.55);
        int    flightGrowSteps     = Math.max(1, ExpressionResolver.resolveInt(rawFlightGrowSteps, actor, 20));

        Location start = actor.getEyeLocation().clone().add(0, 0.8, 0);

        if (immediateImpact || speed <= 0.05) {
            if (!"NONE".equals(impactShape) || impactDamage > 0) {
                doImpact(world, start, impactRadius, impactDuration, impactPoints, impactPPP, impactDamage, actor);
            }
            return true;
        }

        
        LivingEntity originalVictim = ctx.getVictim();
        Vector direction;
        if (originalVictim != null && originalVictim.isValid()) {
            Location targetBody = originalVictim.getLocation().add(0, originalVictim.getHeight() / 2.0, 0);
            direction = targetBody.toVector().subtract(start.toVector()).normalize();
        } else {
            direction = actor.getEyeLocation().getDirection();
        }

        Vector stepVec = direction.multiply(Math.max(0.1, speed));
        int maxSteps = 200;

        new BukkitRunnable() {
            int step = 0;
            final Location pos = start.clone();

            @Override
            public void run() {
                if (step >= maxSteps) { cancel(); return; }

                pos.add(stepVec);
                step++;

                if (world.isChunkLoaded(pos.getBlockX() >> 4, pos.getBlockZ() >> 4)) {
                    if ("TORNADO".equals(flightShape)) {
                        drawFlightTornado(world, pos, step, flightRadius, flightRotationSpeed, flightGrowSteps);
                    } else {
                        spawnParticleSafe(world, pos, particle, particlePerStep, 0, 0, 0, 0);
                    }

                    if (sweepAttack && step % sweepAttackInterval == 0) {
                        world.spawnParticle(Particle.SWEEP_ATTACK, pos, 1, 0, 0, 0, 0);
                    }
                }

                for (Entity nearby : world.getNearbyEntities(pos, hitRadius, hitRadius, hitRadius)) {
                    if (!(nearby instanceof LivingEntity target)) continue;
                    if (target.isDead() || !target.isValid()) continue;
                    if (target.equals(actor) && !hitActorSelf) continue;

                    Location targetCenter = target.getLocation().add(0, target.getHeight() / 2.0, 0);
                    if (pos.distance(targetCenter) > hitRadius) continue;

                    if (projectileDamage > 0) {
                        applyDamage(target, actor, projectileDamage, damageType);
                    }

                    if (!"NONE".equals(impactShape) || impactDamage > 0) {
                        doImpact(world, pos.clone(), impactRadius, impactDuration, impactPoints, impactPPP, impactDamage, actor);
                    }

                    cancel();
                    return;
                }

                if (step > 80 && pos.distance(start) > 60) cancel();
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        return true;
    }

    private void doImpact(World world, Location origin, double radius, int durationTicks,
                          int points, int ppp, double impactDamage, Player damager) {

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;

                if (impactDamage > 0 && tick % 3 == 0) {
                    double currentRadius = radius * (0.5 + progress * 0.7);
                    for (Entity e : world.getNearbyEntities(origin, currentRadius, currentRadius * 0.9, currentRadius)) {
                        if (!(e instanceof LivingEntity target) || target.isDead() || !target.isValid()) continue;
                        if (target.equals(damager)) continue;
                        double dmg = impactDamage * (0.35 / (1 + tick / 5.0));
                        applyDamage(target, damager, dmg, impactDamageType);
                    }
                }

                if (!"NONE".equals(impactShape)) {
                    switch (impactShape) {
                        case "CIRCLE"  -> drawImpactCircle(world, origin, radius, progress, points, ppp);
                        case "SPHERE"  -> drawImpactSphere(world, origin, radius, progress, points, ppp);
                        case "BURST"   -> drawImpactBurst(world, origin, radius, progress, points, ppp);
                        case "TORNADO" -> drawImpactTornado(world, origin, radius, progress, points, ppp);
                    }
                }
                tick++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    

    
    private void drawFlightTornado(World world, Location pos, int step, double maxRadius,
                                   double rotationSpeed, int growSteps) {
        double growFactor = flightGrow ? Math.min(1.0, step / (double) growSteps) : 1.0;
        double radius = maxRadius * (0.4 + 0.6 * growFactor);

        for (int ring = 0; ring < flightRings; ring++) {
            double ringRatio = flightRings <= 1 ? 0 : (double) ring / (flightRings - 1);
            double yOffset = (ringRatio - 0.5) * 0.6;
            double ringRadius = radius * (1.0 - ringRatio * 0.35);
            double rotation = step * rotationSpeed + ring * 1.2;

            for (int i = 0; i < flightPointsPerRing; i++) {
                double angle = 2 * Math.PI * i / flightPointsPerRing + rotation;
                double x = ringRadius * Math.cos(angle);
                double z = ringRadius * Math.sin(angle);
                Location point = pos.clone().add(x, yOffset, z);
                spawnParticleSafe(world, point, particle, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }

    

    private void drawImpactCircle(World world, Location origin, double maxRadius, double progress, int points, int ppp) {
        double radius = maxRadius * Math.sin(progress * Math.PI / 2);
        double heightOffset = 0.3 + progress * 0.8;
        int actualPpp = Math.max(2, (int) Math.ceil(ppp * (1.4 - progress * 0.8)));

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location point = origin.clone().add(x, heightOffset, z);
            spawnParticleSafe(world, point, impactParticle, actualPpp, 0.1, 0.1, 0.1, 0.02);

            if (progress < 0.75) {
                Vector dir = new Vector(x, 0.4, z).normalize().multiply(0.15 + progress * 0.2);
                spawnParticleSafe(world, point, impactParticle, 1, dir.getX(), 0.2, dir.getZ(), 0.08);
            }
        }
    }

    private void drawImpactSphere(World world, Location origin, double maxRadius, double progress, int points, int ppp) {
        double r = maxRadius * easeOut(progress);
        int rings = Math.max(3, points / 5);
        int actualPpp = Math.max(1, (int) Math.ceil(ppp * (1.0 - progress * 0.6)));
        for (int ring = 0; ring < rings; ring++) {
            double phi = Math.PI * ring / (rings - 1);
            double ringRadius = r * Math.sin(phi);
            double yOffset = r * Math.cos(phi);
            int pointsInRing = Math.max(4, (int)(points * Math.sin(phi)) + 1);
            for (int i = 0; i < pointsInRing; i++) {
                double angle = 2 * Math.PI * i / pointsInRing;
                Location point = origin.clone().add(ringRadius * Math.cos(angle), yOffset, ringRadius * Math.sin(angle));
                spawnParticleSafe(world, point, impactParticle, actualPpp, 0, 0, 0, 0);
            }
        }
    }

    private void drawImpactBurst(World world, Location origin, double maxRadius, double progress, int points, int ppp) {
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < points; i++) {
            double y = 1.0 - (i / (double)(points - 1)) * 2.0;
            double r2d = Math.sqrt(Math.max(0, 1 - y * y));
            double theta = goldenAngle * i;
            double speedMult = 0.8 + (i % 5) * 0.1;
            double dist = maxRadius * easeOut(progress) * speedMult;
            int actualPpp = Math.max(1, (int) Math.ceil(ppp * (1.0 - progress * 0.8)));
            Location point = origin.clone().add(dist * r2d * Math.cos(theta), dist * y, dist * r2d * Math.sin(theta));
            spawnParticleSafe(world, point, impactParticle, actualPpp, 0, 0, 0, 0);
        }
    }

    private void drawImpactTornado(World world, Location origin, double maxRadius, double progress, int points, int ppp) {
        double height = maxRadius * 2.4;
        int rings = 7;
        double currentHeight = height * Math.min(1.0, progress * 1.15);
        int actualPpp = Math.max(1, (int) Math.ceil(ppp * (1.3 - progress * 0.9)));

        for (int ring = 0; ring < rings; ring++) {
            double ringProgress = (double) ring / rings;
            double y = ringProgress * currentHeight;
            double radius = maxRadius * (1.0 - ringProgress * 0.65) * (0.9 + Math.sin(progress * Math.PI * 5) * 0.15);
            double rotation = progress * Math.PI * 7.5 + ring * 1.1;

            int pointsInRing = Math.max(10, points / rings + 6);

            for (int i = 0; i < pointsInRing; i++) {
                double angle = 2 * Math.PI * i / pointsInRing + rotation;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location point = origin.clone().add(x, y + Math.random() * 0.3, z);
                spawnParticleSafe(world, point, impactParticle, actualPpp, 0.08, 0.15, 0.08, 0.03);
            }
        }
    }

    private static double easeOut(double t) {
        return 1.0 - (1.0 - t) * (1.0 - t);
    }

    private static Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Particle.FLAME;
        }
    }

    private static void applyDamage(LivingEntity target, Player damager, double amount, String damageType) {
        if ("NORMAL".equals(damageType)) {
            target.setMetadata("SKILL_DAMAGE_PROCESSED", new FixedMetadataValue(Main.getInstance(), true));
            target.setMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE, new FixedMetadataValue(Main.getInstance(), true));
            try {
                if (damager != null) target.damage(amount, damager);
                else target.damage(amount);
            } finally {
                target.removeMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE, Main.getInstance());
                target.removeMetadata("SKILL_DAMAGE_PROCESSED", Main.getInstance());
            }
        } else {
            double newHealth = Math.max(0.0, target.getHealth() - amount);
            target.setHealth(newHealth);
            if (newHealth <= 0) target.damage(0);
        }
    }
}