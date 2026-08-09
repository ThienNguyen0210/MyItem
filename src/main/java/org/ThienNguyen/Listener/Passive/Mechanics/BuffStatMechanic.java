package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.ThienNguyen.Listener.Passive.TempBuff;
import org.ThienNguyen.Listener.StatsListener;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class BuffStatMechanic extends AbstractMechanic {

    
    private static final ConcurrentHashMap<String, Double>  activeAmounts = new ConcurrentHashMap<>();

    
    private static final ConcurrentHashMap<String, Integer> activeTasks   = new ConcurrentHashMap<>();

    private final String stat;
    private final String rawAmount;
    private final String rawDuration;

    public BuffStatMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.stat        = cfg.getString("stat",             "").toLowerCase().trim();
        this.rawAmount   = cfg.getString("amount",           "0");
        this.rawDuration = cfg.getString("duration-seconds", "5");

        
        
        
        
        if (!stat.isEmpty() && !PlayerCombatCache.isKnownStat(stat)) {
            Bukkit.getLogger().warning(
                    "[BuffStatMechanic] stat '" + stat + "' không khớp tên nào PlayerCombatCache " +
                            "nhận diện — buff này sẽ KHÔNG có tác dụng thật trong combat dù không báo lỗi khi chạy. " +
                            "Các tên hợp lệ: " + PlayerCombatCache.getKnownStatKeys()
            );
        }
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity targetEntity = resolveTarget(ctx);
        if (!(targetEntity instanceof Player p) || stat.isEmpty()) return false;

        double newAmount = ExpressionResolver.resolve(rawAmount,   ctx.getActor(), 0);
        int    duration  = ExpressionResolver.resolveInt(rawDuration, ctx.getActor(), 5);
        if (newAmount == 0) return false;

        String key = p.getUniqueId() + ":" + stat;
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(p.getUniqueId());

        
        Integer oldTaskId = activeTasks.remove(key);
        if (oldTaskId != null) {
            Bukkit.getScheduler().cancelTask(oldTaskId);
        }
        activeAmounts.remove(key);
        
        
        stats.tempBuffs.remove(key);

        
        stats.tempBuffs.put(key, new TempBuff(stat, newAmount, duration));
        activeAmounts.put(key, newAmount);

        
        
        
        int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            activeTasks.remove(key);
            activeAmounts.remove(key);
            if (!p.isOnline()) return;
            PlayerCombatCache.CombatStats current = PlayerCombatCache.getStats(p.getUniqueId());
            current.tempBuffs.remove(key);
            if ("health".equals(stat)) {
                StatsListener.getInstance().updatePlayerStats(p);
            }
        }, duration * 20L).getTaskId();

        activeTasks.put(key, taskId);
        

        if ("health".equals(stat)) {
            StatsListener.getInstance().updatePlayerStats(p);
        }
        

        return true;
    }

    

    
    public static void clearPlayer(UUID playerId) {
        String prefix = playerId + ":";
        activeAmounts.keySet().removeIf(k -> k.startsWith(prefix));
        activeTasks.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(prefix)) {
                Bukkit.getScheduler().cancelTask(e.getValue());
                return true;
            }
            return false;
        });

        
        
        
        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(playerId);
        stats.tempBuffs.keySet().removeIf(k -> k.startsWith(prefix));
    }
}