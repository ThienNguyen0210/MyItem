package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class BreakAreaMechanic extends AbstractMechanic {

    private final String rawSize;
    private final String rawDepth;
    private final boolean useToolDrops;
    private final List<Material> excludedMaterials;

    public BreakAreaMechanic(ConfigurationSection cfg) {
        super(cfg);
        this.rawSize  = cfg.getString("size",  "3");
        this.rawDepth = cfg.getString("depth", "1");
        this.useToolDrops = cfg.getBoolean("use-tool-drops", true);

        this.excludedMaterials = new ArrayList<>();
        String excludedRaw = cfg.getString("excluded-materials", "BEDROCK,BARRIER,END_PORTAL_FRAME,COMMAND_BLOCK");
        for (String name : excludedRaw.split(",")) {
            Material m = Material.matchMaterial(name.trim().toUpperCase());
            if (m != null) excludedMaterials.add(m);
        }
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        Block origin = ctx.getBrokenBlock();
        Player actor = ctx.getActor();
        if (origin == null || actor == null) return false;

        int size = (int) ExpressionResolver.resolve(rawSize, actor, 3);
        int depth = (int) ExpressionResolver.resolve(rawDepth, actor, 1);
        if (size < 1) size = 1;
        if (depth < 1) depth = 1;

        int half = size / 2; 

        ItemStack tool = useToolDrops ? actor.getInventory().getItemInMainHand() : null;

        boolean anySuccess = false;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dy = 0; dy < depth; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue; 

                    Block target = origin.getRelative(dx, dy, dz);
                    if (target.getType() == Material.AIR || target.getType() == Material.CAVE_AIR) continue;
                    if (excludedMaterials.contains(target.getType())) continue;

                    
                    
                    
                    boolean broke = (tool != null) ? target.breakNaturally(tool) : target.breakNaturally();
                    if (broke) anySuccess = true;
                }
            }
        }

        return anySuccess;
    }
}