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
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Hiệu ứng particle hình CIRCLE hoặc SPHERE xoay quanh target.
 */
public class ParticleAnimationMechanic extends AbstractMechanic {

    private final Particle particle;
    private final String shape;
    private final String rawRadius;
    private final String rawPointsPerTick;
    private final String rawParticlePerPoint;
    private final String rawDurationSeconds;
    private final String rawUpdateIntervalTicks;
    private final boolean rotate;

    // Extra data cho particle đặc biệt
    private final Color dustColor;
    private final Material blockMaterial;

    public ParticleAnimationMechanic(ConfigurationSection cfg) {
        super(cfg);

        Particle parsed;
        try {
            parsed = Particle.valueOf(cfg.getString("particle", "FLAME").toUpperCase());
        } catch (IllegalArgumentException e) {
            parsed = Particle.FLAME;
        }
        this.particle = parsed;

        this.shape                  = cfg.getString("shape", "CIRCLE").toUpperCase();
        this.rawRadius              = cfg.getString("radius",               "1.0");
        this.rawPointsPerTick       = cfg.getString("points-per-tick",      "12");
        this.rawParticlePerPoint    = cfg.getString("particle-per-point",   "1");
        this.rawDurationSeconds     = cfg.getString("duration-seconds",     "3");
        this.rawUpdateIntervalTicks = cfg.getString("update-interval-ticks","4");
        this.rotate                 = cfg.getBoolean("rotate", true);

        // Extra data
        this.dustColor = parseColor(cfg.getString("dust-color", "#FF5500"));
        this.blockMaterial = parseMaterial(cfg.getString("block-material", "OAK_LEAVES"));
    }

    private Color parseColor(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (Exception e) {
            return Color.RED;
        }
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Material.OAK_LEAVES;
        }
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity center = resolveTarget(ctx);
        if (center == null || !center.isValid()) return false;

        World world = center.getWorld();
        if (world == null) return false;

        double radius           = ExpressionResolver.resolve(rawRadius,           ctx.getActor(), 1.0);
        int    pointsPerTick    = Math.max(1, ExpressionResolver.resolveInt(rawPointsPerTick,    ctx.getActor(), 12));
        int    particlePerPoint = Math.max(1, ExpressionResolver.resolveInt(rawParticlePerPoint, ctx.getActor(), 1));
        int    durationTicks    = Math.max(1, ExpressionResolver.resolveInt(rawDurationSeconds,  ctx.getActor(), 3) * 20);
        int    updateInterval   = Math.max(1, ExpressionResolver.resolveInt(rawUpdateIntervalTicks, ctx.getActor(), 4));

        new BukkitRunnable() {
            int elapsedTicks = 0;
            double rotationOffset = 0.0;

            @Override
            public void run() {
                if (elapsedTicks >= durationTicks || center.isDead() || !center.isValid()) {
                    cancel();
                    return;
                }
                Location origin = center.getLocation().add(0, 1.0, 0);
                drawShape(world, origin, rotationOffset, radius, pointsPerTick, particlePerPoint);
                if (rotate) rotationOffset += Math.PI / 8;
                elapsedTicks += updateInterval;
            }
        }.runTaskTimer(Main.getInstance(), 0L, updateInterval);

        return true;
    }

    private void drawShape(World world, Location origin, double rotOff,
                           double radius, int pointsPerTick, int particlePerPoint) {
        if ("SPHERE".equals(shape)) {
            drawSphere(world, origin, rotOff, radius, pointsPerTick, particlePerPoint);
        } else {
            drawCircle(world, origin, rotOff, radius, pointsPerTick, particlePerPoint, 0.0);
        }
    }

    private void spawnParticleSafe(World world, Location point, int count) {
        try {
            if (particle == Particle.DUST || particle == Particle.DUST) {
                Particle.DustOptions options = new Particle.DustOptions(dustColor, 1.5f);
                world.spawnParticle(particle, point, count, 0, 0, 0, 0, options);
            } else if (particle == Particle.BLOCK || particle == Particle.BLOCK || particle == Particle.FALLING_DUST) {
                BlockData data = blockMaterial.createBlockData();
                world.spawnParticle(particle, point, count, 0, 0, 0, 0, data);
            } else {
                world.spawnParticle(particle, point, count, 0, 0, 0, 0);
            }
        } catch (Exception e) {
            world.spawnParticle(Particle.FLAME, point, count, 0, 0, 0, 0);
        }
    }

    private void drawCircle(World world, Location origin, double rotOff,
                            double radius, int points, int ppp, double yOffset) {
        for (int i = 0; i < points; i++) {
            double angle = rotOff + (2 * Math.PI * i / points);
            Location point = new Location(world,
                    origin.getX() + radius * Math.cos(angle),
                    origin.getY() + yOffset,
                    origin.getZ() + radius * Math.sin(angle));
            spawnParticleSafe(world, point, ppp);
        }
    }

    private void drawSphere(World world, Location origin, double rotOff,
                            double radius, int pointsPerTick, int ppp) {
        int rings = Math.max(3, pointsPerTick / 4);
        for (int ring = 0; ring < rings; ring++) {
            double phi = Math.PI * ring / (rings - 1);
            double ringRadius = radius * Math.sin(phi);
            double yOffset = radius * Math.cos(phi);
            int pointsInRing = Math.max(4, (int)(pointsPerTick * Math.sin(phi)) + 1);
            for (int i = 0; i < pointsInRing; i++) {
                double angle = rotOff + (2 * Math.PI * i / pointsInRing);
                Location point = new Location(world,
                        origin.getX() + ringRadius * Math.cos(angle),
                        origin.getY() + yOffset,
                        origin.getZ() + ringRadius * Math.sin(angle));
                spawnParticleSafe(world, point, ppp);
            }
        }
    }
}