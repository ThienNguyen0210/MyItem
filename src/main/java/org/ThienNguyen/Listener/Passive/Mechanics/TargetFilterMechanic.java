package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.MechanicRegistry;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TargetFilterMechanic extends AbstractMechanic {

    private static final String KEYWORD_PLAYER  = "PLAYER";
    private static final String KEYWORD_ALL_MOB = "ALL";
    private static final String KEYWORD_ALL_MM  = "ALL_MM";
    private static final String MM_PREFIX       = "MM_";

    private final boolean matchPlayer;
    private final boolean matchAllMobs;
    private final boolean matchAllMythic;
    private final Set<String> vanillaTypes; 
    private final Set<String> mythicIds;    

    private final List<PassiveMechanic> children;
    private final boolean mythicMobsEnabled;

    public TargetFilterMechanic(ConfigurationSection cfg) {
        super(cfg);

        boolean player = false;
        boolean allMobs = false;
        boolean allMythic = false;
        Set<String> vTypes = new HashSet<>();
        Set<String> mIds = new HashSet<>();

        List<String> rawTypes = cfg.getStringList("types");
        if (rawTypes.isEmpty()) {
            Main.getInstance().getLogger()
                    .warning("[Passive] TARGET_FILTER: không có 'types' nào được khai báo — mechanic này sẽ KHÔNG BAO GIỜ khớp.");
        }

        for (String raw : rawTypes) {
            if (raw == null || raw.isBlank()) continue;
            String trimmed = raw.trim();
            String upper = trimmed.toUpperCase();

            if (upper.equals(KEYWORD_PLAYER)) {
                player = true;
            } else if (upper.equals(KEYWORD_ALL_MOB)) {
                allMobs = true;
            } else if (upper.equals(KEYWORD_ALL_MM)) {
                allMythic = true;
            } else if (upper.startsWith(MM_PREFIX)) {
                
                String id = trimmed.length() > 3 ? trimmed.substring(3) : "";
                if (!id.isEmpty()) mIds.add(id.toUpperCase());
                else Main.getInstance().getLogger()
                        .warning("[Passive] TARGET_FILTER: entry 'mm_' thiếu id phía sau, bỏ qua: '" + raw + "'");
            } else {
                
                vTypes.add(upper);
            }
        }

        this.matchPlayer    = player;
        this.matchAllMobs   = allMobs;
        this.matchAllMythic = allMythic;
        this.vanillaTypes   = vTypes;
        this.mythicIds      = mIds;

        this.mythicMobsEnabled = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
        if ((allMythic || !mIds.isEmpty()) && !mythicMobsEnabled) {
            Main.getInstance().getLogger()
                    .warning("[Passive] TARGET_FILTER: config có entry MythicMobs (mm_*/all_mm) nhưng server KHÔNG cài MythicMobs — các entry này sẽ không bao giờ khớp.");
        }

        this.children = parseChildren(cfg, "actions");
        if (this.children.isEmpty()) {
            Main.getInstance().getLogger()
                    .warning("[Passive] TARGET_FILTER: không có 'actions' nào build thành công — mechanic này sẽ không làm gì kể cả khi khớp type.");
        }
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity target = resolveTarget(ctx);
        if (target == null || !target.isValid() || target.isDead()) return false;
        if (children.isEmpty()) return false;
        if (!matches(target)) return false;

        boolean anySuccess = false;
        for (PassiveMechanic m : children) {
            if (m.execute(ctx)) anySuccess = true;
        }
        return anySuccess;
    }

    

    private boolean matches(LivingEntity target) {
        if (target instanceof Player) {
            
            
            return matchPlayer;
        }

        
        
        if (mythicMobsEnabled) {
            String mythicId = getMythicInternalName(target);
            if (mythicId != null) {
                if (matchAllMythic) return true;
                return mythicIds.contains(mythicId.toUpperCase());
            }
        }

        
        
        if (matchAllMobs) return true;
        return vanillaTypes.contains(target.getType().name());
    }

    
    private String getMythicInternalName(LivingEntity entity) {
        try {
            var activeMobOpt = io.lumine.mythic.bukkit.MythicBukkit.inst()
                    .getMobManager()
                    .getActiveMob(entity.getUniqueId());
            if (activeMobOpt.isPresent()) {
                return activeMobOpt.get().getMobType();
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
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
                        .warning("[Passive] TARGET_FILTER '" + key
                                + "': 1 entry không phải ConfigurationSection lẫn Map (obj class = "
                                + (obj == null ? "null" : obj.getClass().getName()) + ") → bỏ qua.");
                continue;
            }

            String childType = childCfg.getString("type", "?");
            PassiveMechanic m = MechanicRegistry.create(childCfg);
            if (m == null) {
                Main.getInstance().getLogger()
                        .warning("[Passive] TARGET_FILTER '" + key
                                + "': MechanicRegistry.create() trả về NULL cho type '" + childType
                                + "' → type này có thể chưa được đăng ký, hoặc config thiếu field bắt buộc.");
                continue;
            }
            result.add(m);
        }
        return result;
    }
}