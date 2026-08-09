package org.ThienNguyen.Listener.Passive.Mechanics;

import me.clip.placeholderapi.PlaceholderAPI;
import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class CommandMechanic extends AbstractMechanic {

    private final String mode;
    private final String commandTemplate;

    public CommandMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.mode            = cfg.getString("mode",    "OP").toUpperCase();
        this.commandTemplate = cfg.getString("command", "");
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        if (commandTemplate.isEmpty()) return false;

        LivingEntity targetEntity = resolveTarget(ctx);
        if (targetEntity == null || !targetEntity.isValid()) return false;

        String playerName = (targetEntity instanceof Player p) ? p.getName() : targetEntity.getName();
        String actorName  = ctx.getActor() != null ? ctx.getActor().getName() : "";

        
        String command = commandTemplate
                .replace("{player}", playerName)
                .replace("{actor}",  actorName)
                .replace("{damage}", String.format("%.1f", ctx.getDamage()));

        
        if (ctx.getActor() != null && command.contains("%")) {
            command = PlaceholderAPI.setPlaceholders(ctx.getActor(), command);
        }

        if ("PLAYER".equals(mode)) {
            if (!(targetEntity instanceof Player p)) return false;
            return p.performCommand(command);
        }

        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}