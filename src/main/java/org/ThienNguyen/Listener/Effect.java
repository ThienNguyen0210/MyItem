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

        // Quét 6 vị trí: 4 món giáp + 2 tay
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

            // Kiểm tra level yêu cầu từ MMOCore (nếu có hook)
            if (!MMOCORE.canUse(player, item)) continue;

            // Lấy Map hiệu ứng từ PDC của item thông qua BuffData
            Map<String, Integer> effects = BuffData.getEffects(item);

            for (Map.Entry<String, Integer> entry : effects.entrySet()) {
                PotionEffectType type = PotionEffectType.getByName(entry.getKey());
                if (type != null) {
                    int level = entry.getValue();
                    if (level <= 0) continue;

                    // Amplifier = level - 1 (Vì trong code 0 là Level I)
                    // Duration = 80 ticks (4 giây) để hiệu ứng không bị nhấp nháy khi lặp lại mỗi 2 giây
                    player.addPotionEffect(new PotionEffect(type, 80, level - 1, true, false, true));
                }
            }
        }
    }
}