package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Summon TNT tại vị trí được resolve từ target.
 *
 * <p><b>ON_DEATH note:</b> Khi dùng với trigger {@code ON_DEATH},
 * hãy set {@code target: ACTOR} (hoặc {@code SELF}) nếu muốn TNT xuất hiện
 * tại vị trí người chết. Mặc định {@code target: VICTIM} sẽ lấy vị trí của
 * killer (hoặc null).
 *
 * <p>Class này đã được chỉnh để vẫn hoạt động bình thường ngay cả khi
 * target (ACTOR/VICTIM) đã chết.
 */
public class SummonTNTMechanic extends AbstractMechanic {

    private enum SpawnMode { FALL, SHOOT, FIXED }

    private static final Random RANDOM = new Random();

    private final String rawAmount;
    private final String rawPower;
    private final String rawFuseTicks;
    private final boolean destroyBlocks;
    private final String targetKeyRaw;
    private final SpawnMode spawnMode;

    private final String rawFallHeight;

    private final String rawShootSpeed;
    private final String rawShootSpreadAngle;

    private final String rawFixedRadius;

    private final String rawDamage;
    private final String rawDamageRadius;
    private final String damageType;
    private final boolean includeSelf;
    private final double particleScale;

    public SummonTNTMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawAmount     = cfg.getString("amount", "1");
        this.rawPower      = cfg.getString("power", "4.0");
        this.rawFuseTicks  = cfg.getString("fuse-ticks", "80");
        this.destroyBlocks = cfg.getBoolean("destroy-blocks", false);
        this.targetKeyRaw  = cfg.getString("target", "VICTIM").toUpperCase();

        String modeRaw = cfg.getString("mode", "FIXED").toUpperCase();
        SpawnMode mode;
        try {
            mode = SpawnMode.valueOf(modeRaw);
        } catch (IllegalArgumentException ex) {
            mode = SpawnMode.FIXED;
        }
        this.spawnMode = mode;

        this.rawFallHeight = cfg.getString("fall-height", "10.0");

        this.rawShootSpeed       = cfg.getString("shoot-speed", "1.0");
        this.rawShootSpreadAngle = cfg.getString("shoot-spread-angle", "45.0");

        this.rawFixedRadius = cfg.getString("radius", "1.0");

        this.rawDamage       = cfg.getString("damage", "6.0");
        this.rawDamageRadius = cfg.getString("damage-radius", "4.0");
        this.damageType       = cfg.getString("damage-type", "TRUE").toUpperCase();
        this.includeSelf      = cfg.getBoolean("include-self", false);
        this.particleScale    = cfg.getDouble("particle-scale", 1.5);
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        Location origin = resolveOrigin(ctx);
        if (origin == null) return false;

        World world = origin.getWorld();
        if (world == null) return false;

        Player actor = ctx.getActor();

        int amount = (int) Math.round(ExpressionResolver.resolve(rawAmount, actor, 1));
        if (amount <= 0) return false;

        float power = (float) ExpressionResolver.resolve(rawPower, actor, 4.0);
        int fuseTicks = (int) Math.round(ExpressionResolver.resolve(rawFuseTicks, actor, 80));
        if (fuseTicks < 0) fuseTicks = 0;

        final double damageAmount = ExpressionResolver.resolve(rawDamage, actor, 6.0);
        final double damageRadius = ExpressionResolver.resolve(rawDamageRadius, actor, 4.0);
        final int fuseTicksFinal = fuseTicks;

        boolean anySpawned = false;
        for (int i = 0; i < amount; i++) {
            Location spawnLoc = computeSpawnLocation(origin, actor);
            TNTPrimed tnt = world.spawn(spawnLoc, TNTPrimed.class);

            tnt.setFuseTicks(fuseTicks);

            tnt.setYield(destroyBlocks ? power : 0f);
            tnt.setIsIncendiary(false);
            if (actor != null) tnt.setSource(actor);

            applyMomentum(tnt, actor);

            scheduleDetonationDamage(tnt, actor, damageAmount, damageRadius, fuseTicksFinal);

            anySpawned = true;
        }

        return anySpawned;
    }

    private void scheduleDetonationDamage(TNTPrimed tnt, Player actor, double damageAmount,
                                          double damageRadius, int fuseTicks) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Location loc = tnt.getLocation();
            World w = loc.getWorld();
            if (w == null) return;

            int particleCount = (int) (60 * particleScale);
            double spread = 2.5 * particleScale;
            w.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.EXPLOSION, loc, particleCount, spread, spread, spread, 0.15);

            if (damageAmount <= 0 || damageRadius <= 0) return;

            for (Entity e : w.getNearbyEntities(loc, damageRadius, damageRadius, damageRadius)) {
                // Cho phép xử lý cả entity đã chết (quan trọng với ON_DEATH)
                if (!(e instanceof LivingEntity le) || !le.isValid()) continue;
                if (le.equals(actor) && !includeSelf) continue;
                if (loc.distance(le.getLocation()) > damageRadius) continue;
                applyExplosionDamage(le, damageAmount, actor);
            }
        }, fuseTicks);
    }

    private void applyExplosionDamage(LivingEntity target, double amount, Player damager) {
        // Không còn return sớm khi target đã chết
        if (!target.isValid()) return;

        target.playEffect(org.bukkit.EntityEffect.HURT);

        if ("NORMAL".equals(damageType)) {
            target.setMetadata("SKILL_DAMAGE_PROCESSED",
                    new FixedMetadataValue(Main.getInstance(), true));
            target.setMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE,
                    new FixedMetadataValue(Main.getInstance(), true));
            try {
                if (damager != null) target.damage(amount, damager);
                else                 target.damage(amount);
            } finally {
                target.removeMetadata(DamageMechanic.META_KEY_NORMAL_SOURCE, Main.getInstance());
                target.removeMetadata("SKILL_DAMAGE_PROCESSED", Main.getInstance());
            }
            return;
        }

        // TRUE damage: vẫn setHealth được dù entity đã chết (health = 0)
        double newHealth = Math.max(0.0, target.getHealth() - amount);
        target.setHealth(newHealth);
        if (newHealth <= 0.0) {
            target.damage(0.0);
        }
    }

    private Location resolveOrigin(PassiveContext ctx) {
        if ("BLOCK".equals(targetKeyRaw)) {
            Block block = ctx.getBrokenBlock();
            if (block == null) return null;
            return block.getLocation().add(0.5, 0.5, 0.5);
        }

        return switch (targetKeyRaw) {
            case "ACTOR", "SELF" -> ctx.getActorLocation();
            case "VICTIM"        -> ctx.getVictimLocation();
            default               -> ctx.getActorLocation();
        };
    }

    private Location computeSpawnLocation(Location origin, Player actor) {
        return switch (spawnMode) {
            case FALL -> {
                double height = ExpressionResolver.resolve(rawFallHeight, actor, 10.0);
                yield origin.clone().add(0, height, 0);
            }
            case FIXED -> {
                double radius = ExpressionResolver.resolve(rawFixedRadius, actor, 1.0);
                if (radius <= 0) yield origin.clone();

                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double dist  = RANDOM.nextDouble() * radius;
                double dx = Math.cos(angle) * dist;
                double dz = Math.sin(angle) * dist;
                yield origin.clone().add(dx, 0, dz);
            }
            case SHOOT -> origin.clone();
        };
    }

    private void applyMomentum(TNTPrimed tnt, Player actor) {
        switch (spawnMode) {
            case SHOOT -> {
                double speed     = ExpressionResolver.resolve(rawShootSpeed, actor, 1.0);
                double spreadDeg = ExpressionResolver.resolve(rawShootSpreadAngle, actor, 45.0);

                double yaw = RANDOM.nextDouble() * Math.PI * 2;
                double maxPitchRad = Math.toRadians(Math.max(0, Math.min(90, spreadDeg)));
                double pitch = RANDOM.nextDouble() * maxPitchRad;

                double horizontal = Math.cos(pitch);
                double x = Math.cos(yaw) * horizontal;
                double z = Math.sin(yaw) * horizontal;
                double y = Math.sin(pitch);

                Vector velocity = new Vector(x, y, z).normalize().multiply(speed);
                tnt.setVelocity(velocity);
            }
            case FALL, FIXED -> tnt.setVelocity(new Vector(0, 0, 0));
        }
    }
}