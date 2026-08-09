package org.ThienNguyen.Listener.Passive.Mechanics;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;


public class SummonMMMechanic extends AbstractSummonMechanic {

    private final String rawMobType;
    private final String rawLevel;

    public SummonMMMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawMobType = cfg.getString("mob",   "");
        this.rawLevel   = cfg.getString("level", "1");
    }

    @Override
    protected LivingEntity spawnMob(PassiveContext ctx) {
        if (rawMobType.isBlank()) return null;

        Optional<MythicMob> opt = MythicBukkit.inst()
                .getMobManager()
                .getMythicMob(rawMobType);

        if (opt.isEmpty()) {
            Main.getInstance().getLogger()
                    .warning("[Passive] SUMMON_MM: không tìm thấy mob type '"
                            + rawMobType + "' trong MythicMobs.");
            return null;
        }

        
        Location spawnLoc = resolveLocation(ctx);
        if (spawnLoc == null) return null;

        double level = ExpressionResolver.resolve(rawLevel, ctx.getActor(), 1.0);
        if (level < 1) level = 1;

        ActiveMob activeMob = opt.get().spawn(
                BukkitAdapter.adapt(spawnLoc), level);

        if (activeMob == null) return null;

        return activeMob.getEntity().getBukkitEntity() instanceof LivingEntity le ? le : null;
    }

    private Location resolveLocation(PassiveContext ctx) {
        return switch (targetKey) {
            case "ACTOR", "SELF" -> ctx.getActorLocation();
            case "VICTIM"        -> ctx.getVictimLocation();
            default               -> ctx.getActorLocation();
        };
    }
}