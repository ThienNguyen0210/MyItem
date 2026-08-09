package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.ThienNguyen.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;


public class DelayMechanic implements PassiveMechanic {

    private final String rawSeconds;
    private final List<PassiveMechanic> children;

    public DelayMechanic(ConfigurationSection cfg) {
        this.rawSeconds = cfg.getString("seconds", "1");
        this.children   = MechanicChildrenParser.parse(cfg, "children");
    }

    @Override
    public boolean execute(PassiveContext ctx) {
        if (children.isEmpty()) return false;

        int seconds = ExpressionResolver.resolveInt(rawSeconds, ctx.getActor(), 1);
        if (seconds < 0) seconds = 0;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (ctx.getActor() == null || !ctx.getActor().isOnline()) return;
                for (PassiveMechanic m : children) {
                    m.execute(ctx);
                }
            }
        }.runTaskLater(Main.getInstance(), seconds * 20L);

        return true;
    }
}