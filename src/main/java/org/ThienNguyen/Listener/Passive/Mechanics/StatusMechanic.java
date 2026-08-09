package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class StatusMechanic extends AbstractMechanic {

    public enum StatusType { STUN, ROOT, DISARM, INVINCIBLE }

    
    public static final String META_STUNNED    = "STUNNED_STATUS";
    public static final String META_ROOTED     = "ROOTED_STATUS";
    public static final String META_DISARMED   = "DISARMED_STATUS";
    public static final String META_INVINCIBLE = "INVINCIBLE_STATUS";

    private static final String TASK_STUN        = "STUN_TASK";
    private static final String TASK_STUN_FREEZE = "STUN_FREEZE_TASK"; 
    private static final String TASK_ROOT        = "ROOT_TASK";
    private static final String TASK_DISARM      = "DISARM_TASK";
    private static final String TASK_INVINCIBLE  = "INVINCIBLE_TASK";

    private static final float DEFAULT_WALK_SPEED = 0.2f;

    private final StatusType status;
    private final String rawDuration;

    public StatusMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawDuration = cfg.getString("duration", "3");

        String raw = cfg.getString("status", "").toUpperCase().trim();
        StatusType parsed;
        try {
            parsed = StatusType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger()
                    .warning("[Passive] STATUS: giá trị không hợp lệ: '" + raw
                            + "'. Dùng STUN | ROOT | DISARM | INVINCIBLE.");
            parsed = null;
        }
        this.status = parsed;
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (status == null) return false;

        LivingEntity target = resolveTarget(ctx);
        if (target == null || !target.isValid() || target.isDead()) return false;

        int durationTicks = ExpressionResolver.resolveInt(rawDuration, ctx.getActor(), 3) * 20;
        if (durationTicks < 1) return false;

        return switch (status) {
            case STUN       -> applyStun(target, durationTicks);
            case ROOT       -> applyRoot(target, durationTicks);
            case DISARM     -> applyDisarm(target, durationTicks);
            case INVINCIBLE -> applyInvincible(target, durationTicks);
        };
    }

    

    private boolean applyStun(LivingEntity target, int ticks) {
        
        cancelExistingTask(target, TASK_STUN);
        cancelExistingTask(target, TASK_STUN_FREEZE);

        target.setMetadata(META_STUNNED, new FixedMetadataValue(Main.getInstance(), true));

        
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, ticks, 255, false, false, false));

        
        if (target instanceof Player p) {
            p.setWalkSpeed(0f);
        }

        
        
        
        Location frozen = target.getLocation().clone();
        int freezeTaskId = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if (!target.isValid() || target.isDead()) return; 
            target.teleport(frozen);
        }, 1L, 1L).getTaskId();
        target.setMetadata(TASK_STUN_FREEZE, new FixedMetadataValue(Main.getInstance(), freezeTaskId));

        int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            target.removeMetadata(META_STUNNED, Main.getInstance());
            target.removeMetadata(TASK_STUN,    Main.getInstance());
            if (target.hasMetadata(TASK_STUN_FREEZE)) {
                Bukkit.getScheduler().cancelTask(target.getMetadata(TASK_STUN_FREEZE).get(0).asInt());
                target.removeMetadata(TASK_STUN_FREEZE, Main.getInstance());
            }
            if (target instanceof Player p && p.isOnline()) {
                p.setWalkSpeed(DEFAULT_WALK_SPEED);
            }
        }, ticks).getTaskId();

        target.setMetadata(TASK_STUN, new FixedMetadataValue(Main.getInstance(), taskId));
        return true;
    }

    

    private boolean applyRoot(LivingEntity target, int ticks) {
        
        
        
        cancelExistingTask(target, TASK_ROOT);

        target.setMetadata(META_ROOTED, new FixedMetadataValue(Main.getInstance(), true));

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, ticks, 255, false, false, false));

        if (target instanceof Player p) {
            p.setWalkSpeed(0f);
        }

        int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            target.removeMetadata(META_ROOTED, Main.getInstance());
            target.removeMetadata(TASK_ROOT,   Main.getInstance());
            if (target instanceof Player p && p.isOnline()) {
                p.setWalkSpeed(DEFAULT_WALK_SPEED);
            }
        }, ticks).getTaskId();

        target.setMetadata(TASK_ROOT, new FixedMetadataValue(Main.getInstance(), taskId));
        return true;
    }

    

    private boolean applyDisarm(LivingEntity target, int ticks) {
        
        
        cancelExistingTask(target, TASK_DISARM);

        target.setMetadata(META_DISARMED, new FixedMetadataValue(Main.getInstance(), true));

        int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            target.removeMetadata(META_DISARMED, Main.getInstance());
            target.removeMetadata(TASK_DISARM,   Main.getInstance());
        }, ticks).getTaskId();

        target.setMetadata(TASK_DISARM, new FixedMetadataValue(Main.getInstance(), taskId));
        return true;
    }

    

    private boolean applyInvincible(LivingEntity target, int ticks) {
        
        cancelExistingTask(target, TASK_INVINCIBLE);

        target.setMetadata(META_INVINCIBLE, new FixedMetadataValue(Main.getInstance(), true));

        int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            target.removeMetadata(META_INVINCIBLE, Main.getInstance());
            target.removeMetadata(TASK_INVINCIBLE, Main.getInstance());
        }, ticks).getTaskId();

        target.setMetadata(TASK_INVINCIBLE, new FixedMetadataValue(Main.getInstance(), taskId));
        return true;
    }

    

    
    private void cancelExistingTask(LivingEntity entity, String taskMetaKey) {
        if (entity.hasMetadata(taskMetaKey)) {
            Bukkit.getScheduler().cancelTask(
                    entity.getMetadata(taskMetaKey).get(0).asInt());
            entity.removeMetadata(taskMetaKey, Main.getInstance());
        }
    }
}