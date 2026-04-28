package org.ThienNguyen.Hook;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.events.UpdateItemEvent;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.ThienNguyen.Main;

/**
 * Hook cơ bản với Shopkeepers API.
 * Hiện tại chỉ đăng ký event, không thay đổi item gì cả (dự án tạm dừng).
 * Sau này có thể mở rộng lại ở đây nếu cần.
 */
public class ShopkeeperFileUpdate implements Listener {

    private final Main plugin;

    public ShopkeeperFileUpdate() {
        this.plugin = Main.getInstance();
    }

    public void register() {
        if (ShopkeepersAPI.isEnabled()) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("[ShopkeeperHook] Đã đăng ký listener với Shopkeepers API");
        } else {
            plugin.getLogger().warning("[ShopkeeperHook] Shopkeepers không được enable, bỏ qua hook");
        }
    }

    public void updateFileDirectly() {
        if (ShopkeepersAPI.isEnabled()) {
            ShopkeepersAPI.updateItems();
            plugin.getLogger().info("[ShopkeeperHook] Đã gọi ShopkeepersAPI.updateItems()");
        }
    }

    @EventHandler
    public void onShopkeeperItemUpdate(UpdateItemEvent event) {
        ItemStack item = event.getItem().copy();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }

        // Hiện tại KHÔNG làm gì cả với item
        // (bạn có thể thêm logic cũ trở lại sau này nếu muốn)
        // Ví dụ: nếu muốn debug thì uncomment dòng dưới
        // plugin.getLogger().info("Shopkeepers đang update item: " + item.getType());

        // Không thay đổi event → Shopkeepers sẽ dùng item gốc
    }
}