package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.AbstractMechanic;
import org.ThienNguyen.Listener.Passive.ExpressionResolver;
import org.ThienNguyen.Listener.Passive.PassiveContext;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;


public class DropItemMechanic extends AbstractMechanic {

    private final Material material;
    
    private final String rawAmount;

    public DropItemMechanic(ConfigurationSection cfg) {
        super(cfg);
        String mat = cfg.getString("material", "GOLD_NUGGET").toUpperCase();
        Material matched = Material.matchMaterial(mat);
        this.material  = matched != null ? matched : Material.GOLD_NUGGET;
        this.rawAmount = cfg.getString("amount", "1");
    }

    @Override
    protected boolean doExecute(PassiveContext ctx) {
        LivingEntity ref = resolveTarget(ctx);
        if (ref == null || !ref.isValid()) return false;

        World world = ref.getWorld();
        if (world == null) return false;

        int qty = resolveAmount(rawAmount, ctx);
        if (qty <= 0) return false;

        world.dropItemNaturally(ref.getLocation(), new ItemStack(material, qty));
        return true;
    }

    
    private int resolveAmount(String raw, PassiveContext ctx) {
        if (raw.contains("-")) {
            
            
            int dashIdx = findRangeDash(raw);
            if (dashIdx > 0) {
                String minStr = raw.substring(0, dashIdx).trim();
                String maxStr = raw.substring(dashIdx + 1).trim();
                int min = ExpressionResolver.resolveInt(minStr, ctx.getActor(), 1);
                int max = ExpressionResolver.resolveInt(maxStr, ctx.getActor(), 1);
                if (min > max) { int tmp = min; min = max; max = tmp; }
                return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
            }
        }
        return ExpressionResolver.resolveInt(raw, ctx.getActor(), 1);
    }

    
    private int findRangeDash(String raw) {
        for (int i = 1; i < raw.length(); i++) {
            if (raw.charAt(i) == '-') {
                char prev = raw.charAt(i - 1);
                
                if (Character.isDigit(prev) || prev == '%' || prev == ' ') {
                    return i;
                }
            }
        }
        return -1;
    }
}