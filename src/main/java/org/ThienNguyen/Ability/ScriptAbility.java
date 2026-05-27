package org.ThienNguyen.Ability;

import bsh.Interpreter;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class ScriptAbility implements IAbility {
    private final String name;
    private final String script;

    public ScriptAbility(String name, String script) {
        this.name = name;
        this.script = script;
    }

    @Override
    public String getName() { return name; }

    /**
     * HÀM GÂY DAMAGE CHUẨN:
     * Truyền damage vào onDamage thông qua nhãn SKILL_DAMAGE_VALUE
     */
    public void powerDamage(Player attacker, LivingEntity target, double damage) {
        if (target == null || target.isDead()) return;

        
        target.setMetadata("SKILL_DAMAGE_VALUE", new FixedMetadataValue(Main.getInstance(), damage));

        
        
        target.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));
        attacker.setMetadata("IS_ABILITY", new FixedMetadataValue(Main.getInstance(), true));

        
        target.damage(1.0, attacker);

        
        target.removeMetadata("SKILL_DAMAGE_VALUE", Main.getInstance());

        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isValid()) target.removeMetadata("IS_ABILITY", Main.getInstance());
                if (attacker.isOnline()) attacker.removeMetadata("IS_ABILITY", Main.getInstance());
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }

    
    public LivingEntity getNearest(Player attacker, double range) {
        List<Entity> entities = attacker.getNearbyEntities(range, range, range);
        LivingEntity nearest = null;
        double minDist = range + 1.0;
        for (Entity e : entities) {
            if (e instanceof LivingEntity le && e != attacker) {
                double dist = e.getLocation().distance(attacker.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = le;
                }
            }
        }
        return nearest;
    }

    
    public void delay(int ticks, Runnable r) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), r, (long) ticks);
    }

    @Override
    public void execute(Player attacker, LivingEntity target, int level, double baseDamage) {
        if (script == null || script.isEmpty()) return;

        
        if (target != null && target.hasMetadata("IS_ABILITY")) return;

        try {
            Interpreter bsh = new Interpreter();
            bsh.set("attacker", attacker);
            bsh.set("target", target);
            bsh.set("level", level);
            bsh.set("baseDamage", baseDamage);
            bsh.set("api", this);

            bsh.eval(script);
        } catch (Exception e) {
            Bukkit.getLogger().warning("§c[Windycraft] Lỗi Script (" + name + "): " + e.getMessage());
        }
    }
}