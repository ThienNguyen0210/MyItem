package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WitherSkull;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;


/**
 * Bắn ra một hoặc nhiều projectile (snowball, egg, arrow, wither skull, ...)
 * theo hình quạt (fan) xoay quanh hướng nhìn của actor, mỗi projectile cách
 * nhau một góc "spread-angle" độ. Damage tùy chỉnh được gắn vào projectile
 * qua metadata và được áp dụng khi trúng mục tiêu bởi
 * {@link org.ThienNguyen.Listener.Passive.ProjectileShotDamageListener}
 * (phải được đăng ký 1 lần trong Main#onEnable).
 *
 * Config mẫu:
 * <pre>
 * type: PROJECTILE_SHOT
 * projectile: SNOWBALL      # SNOWBALL, EGG, ARROW, SPECTRAL_ARROW, WITHER_SKULL,
 *                            # SMALL_FIREBALL, FIREBALL, DRAGON_FIREBALL,
 *                            # SHULKER_BULLET, LLAMA_SPIT, ENDER_PEARL, TRIDENT
 * damage: "5"                # expression, damage áp dụng khi trúng LivingEntity
 * amount: "3"                # số lượng projectile bắn ra mỗi lần cast
 * spread-angle: "15"         # độ lệch góc (ngang) giữa mỗi projectile liên tiếp
 * speed: "1.5"               # tốc độ bắn
 * gravity: true               # projectile có bị trọng lực kéo xuống không
 * </pre>
 */
public class ProjectileShotMechanic implements PassiveMechanic {



    public static final String DAMAGE_METADATA_KEY = "passive_projectile_damage";



    private final String rawProjectileType;
    private final String rawDamage;
    private final String rawAmount;
    private final String rawSpreadAngle;
    private final String rawSpeed;
    private final boolean gravity;



    public ProjectileShotMechanic(ConfigurationSection cfg) {
        this.rawProjectileType = cfg.getString("projectile", "SNOWBALL").trim().toUpperCase();
        this.rawDamage         = cfg.getString("damage", "0");
        this.rawAmount         = cfg.getString("amount", "1");
        this.rawSpreadAngle    = cfg.getString("spread-angle", "10");
        this.rawSpeed          = cfg.getString("speed", "1.5");
        this.gravity           = cfg.getBoolean("gravity", true);

        if (resolveProjectileClass() == null) {
            Main.getInstance().getLogger()
                    .warning("[Passive] PROJECTILE_SHOT: loại projectile không hợp lệ trong config: '"
                            + rawProjectileType + "'. Mechanic này sẽ không bắn được gì cả.");
        }
    }



    @Override
    public boolean execute(PassiveContext ctx) {
        Player actor = ctx.getActor();
        if (actor == null) return false;

        Class<? extends Projectile> projClass = resolveProjectileClass();
        if (projClass == null) {

            return false;
        }

        int amount = ExpressionResolver.resolveInt(rawAmount, actor, 1);
        if (amount < 1) amount = 1;

        double spreadDeg = ExpressionResolver.resolve(rawSpreadAngle, actor, 10);
        double speed      = ExpressionResolver.resolve(rawSpeed, actor, 1.5);
        double damage     = ExpressionResolver.resolve(rawDamage, actor, 0);

        Location eye = actor.getEyeLocation();
        Vector baseDir = eye.getDirection().normalize();


        double startOffset = -spreadDeg * (amount - 1) / 2.0;

        boolean anySpawned = false;

        for (int i = 0; i < amount; i++) {
            double angleDeg = startOffset + (i * spreadDeg);


            Vector dir = baseDir.clone().rotateAroundY(Math.toRadians(angleDeg));

            Projectile proj = actor.launchProjectile(projClass, dir.multiply(speed));
            if (proj == null) continue;

            proj.setGravity(gravity);
            if (damage > 0) {
                proj.setMetadata(DAMAGE_METADATA_KEY, new FixedMetadataValue(Main.getInstance(), damage));
            }

            anySpawned = true;
        }

        if (!anySpawned) {
            Main.getInstance().getLogger()
                    .fine("[Passive] PROJECTILE_SHOT: không có projectile nào được bắn ra (actor null hoặc launchProjectile thất bại).");
        }

        return anySpawned;
    }




    private Class<? extends Projectile> resolveProjectileClass() {
        return switch (rawProjectileType) {
            case "SNOWBALL"                     -> Snowball.class;
            case "EGG"                           -> Egg.class;
            case "ARROW"                          -> Arrow.class;
            case "SPECTRAL_ARROW"                 -> SpectralArrow.class;
            case "WITHER_SKULL"                   -> WitherSkull.class;
            case "SMALL_FIREBALL"                 -> SmallFireball.class;
            case "FIREBALL", "LARGE_FIREBALL"     -> LargeFireball.class;
            case "DRAGON_FIREBALL"                -> DragonFireball.class;
            case "SHULKER_BULLET"                 -> ShulkerBullet.class;
            case "LLAMA_SPIT"                     -> LlamaSpit.class;
            case "ENDER_PEARL"                    -> EnderPearl.class;
            case "TRIDENT"                        -> Trident.class;
            default -> null;
        };
    }
}