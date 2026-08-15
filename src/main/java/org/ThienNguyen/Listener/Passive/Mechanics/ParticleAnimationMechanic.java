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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ParticleAnimationMechanic extends AbstractMechanic {

    private final Particle particle;
    private final String shape;
    private final String rawRadius;
    private final String rawPointsPerTick;
    private final String rawParticlePerPoint;
    private final String rawDurationSeconds;
    private final String rawUpdateIntervalTicks;
    private final String rawHeight;
    private final String rawForwardOffset;
    private final String rawSideOffset;
    private final boolean rotate;

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
        this.rawHeight              = cfg.getString("height",               "1.0");
        this.rawForwardOffset       = cfg.getString("forward-offset",       "0.0");
        this.rawSideOffset          = cfg.getString("side-offset",          "0.0");
        this.rotate                 = cfg.getBoolean("rotate", true);

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

        // 1. Lấy vị trí xuất phát an toàn (kể cả khi center đã chết hoặc null)
        Location initialLoc = null;
        if (center != null) {
            try {
                initialLoc = center.getLocation();
            } catch (Exception ignored) {}
        }
        if (initialLoc == null) {
            initialLoc = ctx.getActorLocation();
        }

        if (initialLoc == null || initialLoc.getWorld() == null) return false;

        World world = initialLoc.getWorld();
        Player actor = ctx.getActor();
        Player safeActor = (actor != null && actor.isValid()) ? actor : null;

        // 2. Resolve các thông số kỹ thuật an toàn
        double radius           = ExpressionResolver.resolve(rawRadius,           safeActor, 1.0);
        int    pointsPerTick    = Math.max(1, ExpressionResolver.resolveInt(rawPointsPerTick,    safeActor, 12));
        int    particlePerPoint = Math.max(1, ExpressionResolver.resolveInt(rawParticlePerPoint, safeActor, 1));
        int    durationTicks    = Math.max(1, ExpressionResolver.resolveInt(rawDurationSeconds,  safeActor, 3) * 20);
        int    updateInterval   = Math.max(1, ExpressionResolver.resolveInt(rawUpdateIntervalTicks, safeActor, 4));
        double height           = ExpressionResolver.resolve(rawHeight,        safeActor, 1.0);
        double forwardOffset    = ExpressionResolver.resolve(rawForwardOffset, safeActor, 0.0);
        double sideOffset       = ExpressionResolver.resolve(rawSideOffset,    safeActor, 0.0);

        // Tạo biến lưu vị trí tĩnh làm Fallback khi target chết/bị xóa
        final Location lastKnownOrigin = computeOrigin(initialLoc, height, forwardOffset, sideOffset);

        // 3. Chạy Task hiển thị hiệu ứng
        new BukkitRunnable() {
            int elapsedTicks = 0;
            double rotationOffset = 0.0;

            @Override
            public void run() {
                if (elapsedTicks >= durationTicks) {
                    cancel();
                    return;
                }

                Location origin;
                // Nếu mục tiêu còn tồn tại và sống -> Bám theo mục tiêu
                if (center != null && center.isValid() && !center.isDead()) {
                    origin = computeOrigin(center.getLocation(), height, forwardOffset, sideOffset);
                    lastKnownOrigin.setX(origin.getX());
                    lastKnownOrigin.setY(origin.getY());
                    lastKnownOrigin.setZ(origin.getZ());
                    lastKnownOrigin.setYaw(origin.getYaw());
                    lastKnownOrigin.setPitch(origin.getPitch());
                } else {
                    // Mục tiêu đã gục/biến mất -> Dùng tọa độ cuối cùng recorded
                    origin = lastKnownOrigin;
                }

                drawShape(world, origin, rotationOffset, radius, pointsPerTick, particlePerPoint);

                if (rotate) {
                    rotationOffset += Math.PI / 8;
                }

                elapsedTicks += updateInterval;
            }
        }.runTaskTimer(Main.getInstance(), 0L, updateInterval);

        return true;
    }

    private Location computeOrigin(Location base, double height, double forwardOffset, double sideOffset) {
        Location origin = base.clone().add(0, height, 0);

        if (forwardOffset == 0.0 && sideOffset == 0.0) return origin;

        Vector direction = base.getDirection().setY(0);
        if (direction.lengthSquared() < 1.0E-6) {
            direction = new Vector(0, 0, 1);
        } else {
            direction = direction.normalize();
        }

        Vector right = new Vector(-direction.getZ(), 0, direction.getX());

        if (forwardOffset != 0.0) origin.add(direction.clone().multiply(forwardOffset));
        if (sideOffset != 0.0)    origin.add(right.clone().multiply(sideOffset));

        return origin;
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
            if (particle == Particle.DUST) {
                Particle.DustOptions options = new Particle.DustOptions(dustColor, 1.5f);
                world.spawnParticle(particle, point, count, 0, 0, 0, 0, options);
            } else if (particle == Particle.BLOCK || particle == Particle.FALLING_DUST) {
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