package org.ThienNguyen.Listener;

import org.ThienNguyen.Main;
import org.ThienNguyen.Hook.MythicMobHook;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MobDrop implements Listener {
    private final Random random = new Random();

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        FileConfiguration config = Main.getInstance().getMobDropConfig();
        if (config == null) return;

        // Hàm này bây giờ đã an toàn, không có plugin sẽ trả về null
        String mythicName = MythicMobHook.getMythicName(entity);

        if (mythicName != null) {
            handleDrop(event, config, "MythicMobs." + mythicName);
        } else {
            // Tên Entity thường phải viết hoa (VD: IRON_GOLEM)
            handleDrop(event, config, "Vanilla." + entity.getType().name());
        }
    }

    private void handleDrop(EntityDeathEvent event, FileConfiguration config, String path) {
        if (!config.contains(path)) return;

        // Lấy danh sách item từ config
        List<Map<?, ?>> drops = config.getMapList(path);
        for (Map<?, ?> drop : drops) {
            String itemId = (String) drop.get("id");
            Object chanceObj = drop.get("chance");
            if (itemId == null || chanceObj == null) continue;

            double chance = ((Number) chanceObj).doubleValue();

            if (random.nextDouble() * 100 <= chance) {
                // Sử dụng ItemDatabase đã được khởi tạo trong Main
                ItemStack item = Main.getInstance().getItemDatabase().loadItem(itemId);
                if (item != null) {
                    event.getDrops().add(item);
                }
            }
        }
    }
}