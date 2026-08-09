package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Listener.Passive.PlayerAware;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public abstract class AbstractSummonMechanic implements PassiveMechanic, PlayerAware {

    

    static final Map<UUID, SummonRecord> trackedMobs = new ConcurrentHashMap<>();

    private static volatile boolean listenerRegistered = false;
    private static final Object LISTENER_LOCK = new Object();

    

    protected final Map<UUID, Set<UUID>> actorSummons = new ConcurrentHashMap<>();

    

    protected final String targetKey;
    private final String rawHealth;
    private final String rawDamage;
    private final String rawSpeed;
    protected final List<PassiveMechanic> onDeathChildren;

    

    record SummonRecord(
            UUID summonerUUID,
            String targetKey,               
            PassiveContext originalCtx,
            List<PassiveMechanic> onDeathMechanics,
            AbstractSummonMechanic owner
    ) {}

    

    protected AbstractSummonMechanic(ConfigurationSection cfg) {
        this.targetKey       = cfg.getString("target", "VICTIM").toUpperCase();
        this.rawHealth       = cfg.getString("health", "");
        this.rawDamage       = cfg.getString("damage", "");
        this.rawSpeed        = cfg.getString("speed",  "");
        this.onDeathChildren = MechanicChildrenParser.parse(cfg, "on-death");
        ensureListenerRegistered();
    }

    

    protected abstract LivingEntity spawnMob(PassiveContext ctx);

    

    @Override
    public final boolean execute(PassiveContext ctx) {
        Player actor = ctx.getActor();
        if (actor == null) return false;

        LivingEntity spawned = spawnMob(ctx);
        if (spawned == null) return false;

        applyStats(spawned, ctx);

        
        
        if ("VICTIM".equals(targetKey)) {
            LivingEntity attackTarget = ctx.getVictim();
            if (attackTarget != null
                    && !attackTarget.getUniqueId().equals(actor.getUniqueId())
                    && spawned instanceof Creature creature) {
                creature.setTarget(attackTarget);
            }
        }

        UUID mobId = spawned.getUniqueId();
        trackedMobs.put(mobId, new SummonRecord(
                actor.getUniqueId(), targetKey, ctx, onDeathChildren, this
        ));
        actorSummons
                .computeIfAbsent(actor.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(mobId);

        return true;
    }

    

    private void applyStats(LivingEntity entity, PassiveContext ctx) {
        Player actor = ctx.getActor();

        if (!rawHealth.isBlank()) {
            double hp = ExpressionResolver.resolve(rawHealth, actor, -1);
            if (hp > 0) {
                AttributeInstance attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (attr != null) { attr.setBaseValue(hp); entity.setHealth(hp); }
            }
        }

        if (!rawDamage.isBlank()) {
            double dmg = ExpressionResolver.resolve(rawDamage, actor, -1);
            if (dmg > 0) {
                AttributeInstance attr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (attr != null) attr.setBaseValue(dmg);
            }
        }

        if (!rawSpeed.isBlank()) {
            double mult = ExpressionResolver.resolve(rawSpeed, actor, -1);
            if (mult > 0) {
                AttributeInstance attr = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (attr != null) attr.setBaseValue(attr.getBaseValue() * mult);
            }
        }
    }

    

    @Override
    public void onPlayerQuit(UUID playerId) {
        Set<UUID> mobs = actorSummons.remove(playerId);
        if (mobs != null) mobs.forEach(trackedMobs::remove);
    }

    

    private static void ensureListenerRegistered() {
        if (listenerRegistered) return;
        synchronized (LISTENER_LOCK) {
            if (listenerRegistered) return;
            Bukkit.getPluginManager().registerEvents(new SummonListener(), Main.getInstance());
            listenerRegistered = true;
        }
    }

    public static class SummonListener implements Listener {

        private final Map<UUID, LivingEntity> lastDamager = new ConcurrentHashMap<>();

        
        @EventHandler(priority = EventPriority.LOWEST)
        public void onMobTarget(EntityTargetLivingEntityEvent event) {
            SummonRecord record = trackedMobs.get(event.getEntity().getUniqueId());
            if (record == null) return;
            LivingEntity newTarget = event.getTarget();
            if (newTarget != null && newTarget.getUniqueId().equals(record.summonerUUID())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (!trackedMobs.containsKey(event.getEntity().getUniqueId())) return;
            if (event.getDamager() instanceof LivingEntity le) {
                lastDamager.put(event.getEntity().getUniqueId(), le);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onMobDeath(EntityDeathEvent event) {
            UUID mobId          = event.getEntity().getUniqueId();
            SummonRecord record = trackedMobs.remove(mobId);
            LivingEntity killer = lastDamager.remove(mobId);

            if (record == null) return;

            Set<UUID> actorMobs = record.owner().actorSummons.get(record.summonerUUID());
            if (actorMobs != null) actorMobs.remove(mobId);

            if (record.onDeathMechanics().isEmpty()) return;

            Player summoner = record.originalCtx().getActor();
            if (!summoner.isOnline()) return;

            
            LivingEntity onDeathVictim;
            if ("SELF".equals(record.targetKey())) {
                onDeathVictim = summoner;               
            } else {
                if (killer == null) return;             
                onDeathVictim = killer;
            }

            PassiveContext deathCtx = new PassiveContext(summoner, onDeathVictim, 0, null);
            for (PassiveMechanic m : record.onDeathMechanics()) {
                m.execute(deathCtx);
            }
        }
    }
}