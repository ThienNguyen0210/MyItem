package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Ném một potion bay đi và khi chạm đất, tạo ra một vùng hiệu ứng
 * (AreaEffectCloud) tồn tại trong khoảng thời gian cấu hình. Vì vị trí đích
 * được chốt (snapshot) tại thời điểm cast qua {@link PassiveContext#getVictimLocation()}
 * / {@link PassiveContext#getActorLocation()}, mechanic vẫn hoạt động bình
 * thường kể cả khi victim đã chết trước khi potion rơi xuống đất.
 *
 * Vùng hiệu ứng thật sự được tạo bởi
 * {@link org.ThienNguyen.Listener.Passive.PotionZoneImpactListener} khi potion
 * chạm đất (phải được đăng ký 1 lần trong Main#onEnable).
 *
 * Config mẫu:
 * <pre>
 * type: POTION_ZONE
 * target: VICTIM              # VICTIM (mặc định) hoặc ACTOR — điểm potion sẽ rơi xuống
 * speed: "0.6"                 # tốc độ ném
 * radius: "3"                  # bán kính vùng hiệu ứng
 * duration-seconds: "8"        # thời gian vùng hiệu ứng tồn tại
 * reapplication-delay-ticks: 20 # khoảng cách (tick) giữa 2 lần áp effect cho cùng 1 entity
 * color: "255,0,0"             # màu particle của cloud (tùy chọn)
 * particle: ENTITY_EFFECT      # loại particle của cloud (tùy chọn)
 * effects:
 *   - type: POISON
 *     amplifier: "1"
 *     duration-seconds: "4"
 *   - type: SLOW
 *     amplifier: "0"
 *     duration-seconds: "4"
 * </pre>
 */
public class PotionZoneMechanic implements PassiveMechanic {




    private static final Map<UUID, ZoneSpec> pendingZones = new ConcurrentHashMap<>();


    public static ZoneSpec consumePendingZone(UUID projectileId) {
        return pendingZones.remove(projectileId);
    }



    private enum TargetMode { VICTIM, ACTOR }

    private final TargetMode targetMode;
    private final String rawSpeed;
    private final String rawRadius;
    private final String rawDurationSeconds;
    private final int reapplicationDelayTicks;
    private final Color color;
    private final Particle particle;
    private final List<EffectSpec> effectSpecs;



    public PotionZoneMechanic(ConfigurationSection cfg) {
        String targetStr = cfg.getString("target", "VICTIM").trim().toUpperCase();
        TargetMode mode;
        try {
            mode = TargetMode.valueOf(targetStr);
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger()
                    .warning("[Passive] POTION_ZONE: target không hợp lệ: '" + targetStr + "'. Trở về VICTIM.");
            mode = TargetMode.VICTIM;
        }
        this.targetMode = mode;

        this.rawSpeed              = cfg.getString("speed", "0.6");
        this.rawRadius             = cfg.getString("radius", "3");
        this.rawDurationSeconds    = cfg.getString("duration-seconds", "8");
        this.reapplicationDelayTicks = cfg.getInt("reapplication-delay-ticks", 20);

        this.color    = parseColor(cfg.getString("color", null));
        this.particle = parseParticle(cfg.getString("particle", null));

        this.effectSpecs = parseEffects(cfg);
        if (this.effectSpecs.isEmpty()) {
            Main.getInstance().getLogger()
                    .warning("[Passive] POTION_ZONE: không có 'effects' nào hợp lệ trong config, cloud sẽ không gây hiệu ứng gì.");
        }
    }



    @Override
    public boolean execute(PassiveContext ctx) {
        Player actor = ctx.getActor();
        if (actor == null) return false;

        Location targetLoc = resolveTargetLocation(ctx);
        if (targetLoc == null) return false;

        Location launchFrom = actor.getEyeLocation();
        double speed = ExpressionResolver.resolve(rawSpeed, actor, 0.6);

        Vector dir = targetLoc.toVector().subtract(launchFrom.toVector());
        double horizontalDist = Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ());

        dir.setY(dir.getY() + (horizontalDist * 0.15) + 0.2);
        if (dir.lengthSquared() == 0) dir = new Vector(0, 1, 0);
        dir.normalize().multiply(Math.max(0.05, speed));

        final Vector velocity = dir;

        ThrownPotion potion = launchFrom.getWorld().spawn(launchFrom, ThrownPotion.class, p -> {
            p.setShooter(actor);
            p.setVelocity(velocity);
            p.setItem(new ItemStack(Material.SPLASH_POTION));
        });

        ZoneSpec spec = buildZoneSpec(actor);
        pendingZones.put(potion.getUniqueId(), spec);


        Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                () -> pendingZones.remove(potion.getUniqueId()), 20L * 30);

        return true;
    }



    private Location resolveTargetLocation(PassiveContext ctx) {
        Location loc = (targetMode == TargetMode.VICTIM) ? ctx.getVictimLocation() : ctx.getActorLocation();
        if (loc == null) loc = ctx.getActorLocation();
        return loc;
    }

    private ZoneSpec buildZoneSpec(Player actor) {
        ZoneSpec spec = new ZoneSpec();
        spec.radius       = Math.max(0.5, ExpressionResolver.resolve(rawRadius, actor, 3));
        spec.durationTicks = Math.max(1, (int) Math.round(
                ExpressionResolver.resolve(rawDurationSeconds, actor, 8) * 20));
        spec.reapplicationDelayTicks = Math.max(1, reapplicationDelayTicks);
        spec.color    = color;
        spec.particle = particle;

        spec.effects = new ArrayList<>();
        for (EffectSpec es : effectSpecs) {
            int amplifier = ExpressionResolver.resolveInt(es.rawAmplifier, actor, 0);
            int durationTicks = Math.max(1, (int) Math.round(
                    ExpressionResolver.resolve(es.rawDurationSeconds, actor, 4) * 20));
            spec.effects.add(new PotionEffect(es.type, durationTicks, amplifier, es.ambient, es.particles, es.icon));
        }
        return spec;
    }



    private static Color parseColor(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            Main.getInstance().getLogger()
                    .warning("[Passive] POTION_ZONE: 'color' phải có dạng \"R,G,B\", nhận được: '" + raw + "'.");
            return null;
        }
        try {
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            return Color.fromRGB(
                    Math.max(0, Math.min(255, r)),
                    Math.max(0, Math.min(255, g)),
                    Math.max(0, Math.min(255, b)));
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger()
                    .warning("[Passive] POTION_ZONE: không parse được 'color': '" + raw + "'.");
            return null;
        }
    }

    private static Particle parseParticle(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Particle.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger()
                    .warning("[Passive] POTION_ZONE: 'particle' không hợp lệ: '" + raw + "'.");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<EffectSpec> parseEffects(ConfigurationSection cfg) {
        List<EffectSpec> result = new ArrayList<>();
        List<?> rawList = cfg.getList("effects");
        if (rawList == null) return result;

        for (Object obj : rawList) {
            ConfigurationSection section;
            if (obj instanceof ConfigurationSection cs) {
                section = cs;
            } else if (obj instanceof Map<?, ?> map) {
                MemoryConfiguration mem = new MemoryConfiguration();
                for (Map.Entry<?, ?> e : ((Map<String, Object>) map).entrySet()) {
                    mem.set(String.valueOf(e.getKey()), e.getValue());
                }
                section = mem;
            } else {
                Main.getInstance().getLogger()
                        .warning("[Passive] POTION_ZONE: 1 entry trong 'effects' không hợp lệ, bỏ qua.");
                continue;
            }

            String typeStr = section.getString("type", "");
            PotionEffectType type = PotionEffectType.getByName(typeStr.trim().toUpperCase());
            if (type == null) {
                Main.getInstance().getLogger()
                        .warning("[Passive] POTION_ZONE: hiệu ứng không hợp lệ: '" + typeStr + "', bỏ qua.");
                continue;
            }

            EffectSpec es = new EffectSpec();
            es.type               = type;
            es.rawAmplifier       = section.getString("amplifier", "0");
            es.rawDurationSeconds = section.getString("duration-seconds", "4");
            es.ambient             = section.getBoolean("ambient", false);
            es.particles           = section.getBoolean("particles", true);
            es.icon                = section.getBoolean("icon", true);
            result.add(es);
        }
        return result;
    }




    public static final class ZoneSpec {
        public double radius;
        public int durationTicks;
        public int reapplicationDelayTicks;
        public Color color;
        public Particle particle;
        public List<PotionEffect> effects;
    }

    private static final class EffectSpec {
        PotionEffectType type;
        String rawAmplifier;
        String rawDurationSeconds;
        boolean ambient;
        boolean particles;
        boolean icon;
    }
}