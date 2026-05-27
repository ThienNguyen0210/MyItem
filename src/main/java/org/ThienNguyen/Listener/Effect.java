package org.ThienNguyen.Listener;

import org.ThienNguyen.Effect.BuffData;
import org.ThienNguyen.Hook.MMOCORE;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class Effect {

    /**
     * Quét toàn bộ trang bị trên người Player và áp dụng Potion Effects.
     * Nên được gọi mỗi 2-3 giây một lần.
     */
    public void updatePlayerEffects(Player player) {
        if (player == null || !player.isOnline()) return;

        
        ItemStack[] items = {
                player.getInventory().getHelmet(),
                player.getInventory().getChestplate(),
                player.getInventory().getLeggings(),
                player.getInventory().getBoots(),
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand()
        };

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;

            
            if (!MMOCORE.canUse(player, item)) continue;

            
            Map<String, Integer> effects = BuffData.getEffects(item);

            for (Map.Entry<String, Integer> entry : effects.entrySet()) {
                PotionEffectType type = PotionEffectType.getByName(entry.getKey());
                if (type != null) {
                    int level = entry.getValue();
                    if (level <= 0) continue;

                    
                    
                    player.addPotionEffect(new PotionEffect(type, 80, level - 1, true, false, true));
                }
            }
        }
    }
}