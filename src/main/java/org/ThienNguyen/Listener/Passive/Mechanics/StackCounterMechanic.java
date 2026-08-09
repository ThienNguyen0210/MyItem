package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.DeathAware;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.MechanicRegistry;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Listener.Passive.PlayerAware;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class StackCounterMechanic implements PassiveMechanic, PlayerAware, DeathAware {

    

    
    private final Map<UUID, Integer> stackMap   = new ConcurrentHashMap<>();

    
    private final Map<UUID, Integer> decayTasks = new ConcurrentHashMap<>();

    

    private final String rawMaxStacks;
    private final boolean triggerAtMax;
    private final String rawDecaySeconds;
    private final boolean onQuit;
    private final boolean onDeath;

    
    private final TreeMap<Integer, List<PassiveMechanic>> milestones = new TreeMap<>();

    
    private final List<PassiveMechanic> onEmptyChildren;

    

    public StackCounterMechanic(ConfigurationSection cfg) {
        this.rawMaxStacks    = cfg.getString("max-stacks",    "5");
        this.triggerAtMax    = cfg.getBoolean("trigger-at-max", false);
        this.rawDecaySeconds = cfg.getString("decay-seconds", "5");
        this.onQuit          = cfg.getBoolean("on-quit",  true);
        this.onDeath         = cfg.getBoolean("on-death", true);

        
        
        
        List<PassiveMechanic> parsedEmpty;
        try {
            parsedEmpty = parseFlatChildren(cfg, "on-empty");
        } catch (Exception e) {
            Main.getInstance().getLogger()
                    .warning("[Passive] STACK_COUNTER: 'on-empty' build children lỗi: "
                            + e.getClass().getSimpleName() + " - " + e.getMessage());
            parsedEmpty = new ArrayList<>();
        }
        this.onEmptyChildren = parsedEmpty;

        ConfigurationSection ms = cfg.getConfigurationSection("milestones");
        if (ms == null) {
            Main.getInstance().getLogger()
                    .warning("[Passive] STACK_COUNTER: không tìm thấy section 'milestones' nào trong config!");
            return;
        }

        for (String key : ms.getKeys(false)) {
            int stackCount;
            try {
                stackCount = Integer.parseInt(key.trim());
            } catch (NumberFormatException e) {
                Main.getInstance().getLogger()
                        .warning("[Passive] STACK_COUNTER: milestone key không phải số: '" + key + "'");
                continue;
            }

            
            
            
            List<PassiveMechanic> children;
            try {
                children = parseMilestoneChildren(ms, key);
            } catch (Exception e) {
                Main.getInstance().getLogger()
                        .warning("[Passive] STACK_COUNTER: milestone '" + key + "' build children lỗi: "
                                + e.getClass().getSimpleName() + " - " + e.getMessage());
                continue;
            }

            if (children.isEmpty()) {
                Main.getInstance().getLogger()
                        .warning("[Passive] STACK_COUNTER: milestone '" + key
                                + "' không có child nào build thành công (MechanicRegistry.create() trả về null cho tất cả entry?) → milestone này sẽ KHÔNG BAO GIỜ chạy.");
                continue;
            }

            milestones.put(stackCount, children);
            Main.getInstance().getLogger()
                    .info("[Passive] STACK_COUNTER: đã đăng ký milestone '" + stackCount
                            + "' với " + children.size() + " child mechanic.");
        }

        if (milestones.isEmpty()) {
            Main.getInstance().getLogger()
                    .warning("[Passive] STACK_COUNTER: KHÔNG có milestone nào được đăng ký thành công! Kiểm tra log phía trên để biết lý do.");
        }
    }

    

    @Override
    public boolean execute(PassiveContext ctx) {
        Player actor = ctx.getActor();
        if (actor == null) return false;

        UUID id = actor.getUniqueId();

        int maxStacks    = ExpressionResolver.resolveInt(rawMaxStacks,    actor, 5);
        int decaySeconds = ExpressionResolver.resolveInt(rawDecaySeconds, actor, 5);
        if (maxStacks    < 1) maxStacks    = 1;
        if (decaySeconds < 1) decaySeconds = 1;

        
        rescheduleDecay(id, decaySeconds);

        
        
        
        
        
        
        final int maxStacksFinal = maxStacks;
        int[] resultStack = new int[1]; 
        stackMap.compute(id, (k, v) -> {
            int current = (v == null) ? 0 : v;

            if (current >= maxStacksFinal) {
                if (triggerAtMax) {
                    
                    
                    
                    resultStack[0] = maxStacksFinal;
                    return current;
                } else {
                    
                    
                    
                    resultStack[0] = -1;
                    return 0;
                }
            }

            int newStack = current + 1;
            resultStack[0] = newStack;
            return newStack;
        });

        
        
        if (resultStack[0] == -1) {
            
            
            fireOnEmpty(ctx);
            return false; 
        }
        return runMilestone(resultStack[0], ctx);
    }

    

    private boolean runMilestone(int stack, PassiveContext ctx) {
        List<PassiveMechanic> children = milestones.get(stack);
        if (children == null || children.isEmpty()) {
            Main.getInstance().getLogger()
                    .fine("[Passive] STACK_COUNTER: stack=" + stack + " không khớp milestone nào, bỏ qua.");
            return false;
        }
        boolean anySuccess = false;
        for (PassiveMechanic m : children) {
            if (m.execute(ctx)) anySuccess = true;
        }
        Main.getInstance().getLogger()
                .fine("[Passive] STACK_COUNTER: milestone stack=" + stack + " chạy " + children.size()
                        + " child, anySuccess=" + anySuccess);
        return anySuccess;
    }

    

    private void rescheduleDecay(UUID id, int seconds) {
        
        Integer oldTask = decayTasks.remove(id);
        if (oldTask != null) Bukkit.getScheduler().cancelTask(oldTask);

        
        int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Integer had = stackMap.remove(id);
            decayTasks.remove(id);

            
            
            if (had != null && had > 0 && !onEmptyChildren.isEmpty()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    fireOnEmpty(new PassiveContext(p, null, 0, null));
                }
            }
        }, seconds * 20L).getTaskId();

        decayTasks.put(id, taskId);
    }

    
    private void fireOnEmpty(PassiveContext ctx) {
        if (onEmptyChildren.isEmpty() || ctx == null) return;
        for (PassiveMechanic m : onEmptyChildren) {
            m.execute(ctx);
        }
    }

    

    @Override
    public void onPlayerQuit(UUID playerId) {
        
        
        
        if (!onQuit) return;

        Integer taskId = decayTasks.remove(playerId);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        stackMap.remove(playerId);
    }

    

    
    @Override
    public void onPlayerDeath(Player player) {
        if (!onDeath || player == null) return;
        UUID id = player.getUniqueId();

        Integer taskId = decayTasks.remove(id);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);

        Integer had = stackMap.remove(id);
        if (had != null && had > 0) {
            fireOnEmpty(new PassiveContext(player, null, 0, null));
        }
    }

    
    public int getStack(UUID playerId) {
        return stackMap.getOrDefault(playerId, 0);
    }

    

    
    @SuppressWarnings("unchecked")
    private static List<PassiveMechanic> parseMilestoneChildren(ConfigurationSection ms, String key) {
        List<?> rawList = ms.getList(key);
        return parseChildrenFromRawList(rawList, "milestone '" + key + "'");
    }

    
    private static List<PassiveMechanic> parseFlatChildren(ConfigurationSection cfg, String key) {
        List<?> rawList = cfg.getList(key);
        return parseChildrenFromRawList(rawList, "'" + key + "'");
    }

    @SuppressWarnings("unchecked")
    private static List<PassiveMechanic> parseChildrenFromRawList(List<?> rawList, String label) {
        List<PassiveMechanic> result = new ArrayList<>();
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
                        .warning("[Passive] STACK_COUNTER " + label
                                + ": 1 entry không phải ConfigurationSection lẫn Map (obj class = "
                                + (obj == null ? "null" : obj.getClass().getName()) + ") → bỏ qua.");
                continue;
            }

            String childType = childCfg.getString("type", "?");
            PassiveMechanic m = MechanicRegistry.create(childCfg);
            if (m == null) {
                Main.getInstance().getLogger()
                        .warning("[Passive] STACK_COUNTER " + label
                                + ": MechanicRegistry.create() trả về NULL cho type '" + childType
                                + "' → type này có thể chưa được đăng ký, hoặc config thiếu field bắt buộc.");
                continue;
            }
            result.add(m);
        }
        return result;
    }
}