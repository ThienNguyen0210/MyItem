package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Main;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;


public class SummonVanillaMechanic extends AbstractSummonMechanic {

    private final EntityType entityType; 

    public SummonVanillaMechanic(ConfigurationSection cfg) {
        super(cfg);

        String rawType = cfg.getString("mob", "").toUpperCase().trim();
        EntityType parsed = null;
        try {
            parsed = EntityType.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger()
                    .warning("[Passive] SUMMON_VANILLA: EntityType không hợp lệ: '"
                            + rawType + "'. Vui lòng kiểm tra lại cấu hình.");
        }
        this.entityType = parsed;
    }
    @Override
    protected LivingEntity spawnMob(PassiveContext ctx) {
        if (entityType == null) return null;

        
        Location spawnLoc = resolveLocation(ctx);
        if (spawnLoc == null) return null;

        try {
            var spawned = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);

            if (spawned instanceof LivingEntity le) return le;

            
            spawned.remove();
            Main.getInstance().getLogger()
                    .warning("[Passive] SUMMON_VANILLA: '" + entityType.name()
                            + "' không phải LivingEntity, không thể summon.");
            return null;

        } catch (Exception e) {
            Main.getInstance().getLogger()
                    .warning("[Passive] SUMMON_VANILLA: lỗi khi spawn '"
                            + entityType.name() + "': " + e.getMessage());
            return null;
        }
    }

    private Location resolveLocation(PassiveContext ctx) {
        return switch (targetKey) {
            case "ACTOR", "SELF" -> ctx.getActorLocation();
            case "VICTIM"        -> ctx.getVictimLocation();
            default               -> ctx.getActorLocation();
        };
    }
}