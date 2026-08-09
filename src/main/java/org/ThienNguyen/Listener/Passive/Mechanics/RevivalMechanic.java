package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.MechanicRegistry;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Listener.Passive.PlayerAware;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class RevivalMechanic extends AbstractMechanic implements PlayerAware {

    
    public static final String META_REVIVE_ARMED = "REVIVE_ARMED";
    public static final String META_REVIVE_REF   = "REVIVE_MECHANIC_REF";

    private final String rawDurationSeconds;
    private final String rawReviveHealthPercent;
    private final List<PassiveMechanic> children;

    
    private final Map<UUID, Integer> armTasks = new ConcurrentHashMap<>();

    public RevivalMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawDurationSeconds     = cfg.getString("duration-seconds",     "10");
        this.rawReviveHealthPercent = cfg.getString("revive-health-percent", "50");

        List<PassiveMechanic> parsed;
        try {
            parsed = parseChildren(cfg, "actions");
        } catch (Exception e) {
            Main.getInstance().getLogger()
                    .warning("[Passive] REVIVE: 'actions' build children lỗi: "
                            + e.getClass().getSimpleName() + " - " + e.getMessage());
            parsed = new ArrayList<>();
        }
        this.children = parsed;

        if (this.children.isEmpty()) {
            Main.getInstance().getLogger()
                    .info("[Passive] REVIVE: không có 'actions' nào — vẫn hồi sinh bình thường "
                            + "nhưng sẽ không chạy hiệu ứng phụ nào thêm lúc hồi sinh.");
        }
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity resolved = resolveTarget(ctx);

        if (!(resolved instanceof Player player)) {
            Main.getInstance().getLogger()
                    .warning("[Passive] REVIVE: target resolve ra không phải Player (mob?) — "
                            + "REVIVE chỉ áp dụng cho Player, bỏ qua.");
            return false;
        }
        if (!player.isOnline() || player.isDead()) return false;

        int durationTicks = Math.max(1,
                ExpressionResolver.resolveInt(rawDurationSeconds, player, 10) * 20);

        
        
        cancelArmTask(player.getUniqueId());

        player.setMetadata(META_REVIVE_ARMED, new FixedMetadataValue(Main.getInstance(), true));
        player.setMetadata(META_REVIVE_REF,   new FixedMetadataValue(Main.getInstance(), this));

        UUID id = player.getUniqueId();
        int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                () -> disarm(player), durationTicks).getTaskId();
        armTasks.put(id, taskId);

        return true;
    }

    
    public void onRevive(Player player) {
        disarm(player);

        double percent = ExpressionResolver.resolve(rawReviveHealthPercent, player, 50.0);
        percent = Math.max(0.0, Math.min(100.0, percent));

        AttributeInstance maxHpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHp = (maxHpAttr != null) ? maxHpAttr.getValue() : 20.0;

        
        
        double healTo = Math.max(1.0, maxHp * percent / 100.0);
        player.setHealth(Math.min(maxHp, healTo));

        if (!children.isEmpty()) {
            PassiveContext reviveCtx = new PassiveContext(player, null, 0, null);
            for (PassiveMechanic m : children) {
                m.execute(reviveCtx);
            }
        }
    }

    
    private void disarm(Player player) {
        player.removeMetadata(META_REVIVE_ARMED, Main.getInstance());
        player.removeMetadata(META_REVIVE_REF,   Main.getInstance());
        cancelArmTask(player.getUniqueId());
    }

    private void cancelArmTask(UUID id) {
        Integer taskId = armTasks.remove(id);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
    }

    

    @Override
    public void onPlayerQuit(UUID playerId) {
        
        
        
        cancelArmTask(playerId);
    }

    

    @SuppressWarnings("unchecked")
    private static List<PassiveMechanic> parseChildren(ConfigurationSection cfg, String key) {
        List<PassiveMechanic> result = new ArrayList<>();
        List<?> rawList = cfg.getList(key);
        if (rawList == null) return result;

        for (Object obj : rawList) {
            ConfigurationSection childCfg = null;

            if (obj instanceof ConfigurationSection section) {
                childCfg = section;
            } else if (obj instanceof Map<?, ?> map) {
                MemoryConfiguration mem = new MemoryConfiguration();
                for (Map.Entry<?, ?> e : ((Map<String, Object>) map).entrySet()) {
                    mem.set(String.valueOf(e.getKey()), e.getValue());
                }
                childCfg = mem;
            }

            if (childCfg == null) {
                Main.getInstance().getLogger()
                        .warning("[Passive] REVIVE '" + key
                                + "': 1 entry không phải ConfigurationSection lẫn Map (obj class = "
                                + (obj == null ? "null" : obj.getClass().getName()) + ") → bỏ qua.");
                continue;
            }

            String childType = childCfg.getString("type", "?");
            PassiveMechanic m = MechanicRegistry.create(childCfg);
            if (m == null) {
                Main.getInstance().getLogger()
                        .warning("[Passive] REVIVE '" + key
                                + "': MechanicRegistry.create() trả về NULL cho type '" + childType
                                + "' → type này có thể chưa được đăng ký, hoặc config thiếu field bắt buộc.");
                continue;
            }
            result.add(m);
        }
        return result;
    }
}