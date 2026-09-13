package org.ThienNguyen.Command.GUI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
/**
 * Mọi lớp GUI (GUIStats, GUIEffect, GUIElement, ...) implement interface này
 * và đăng ký với GUIListener.registerHandler(title, this) để được nhận sự kiện click,
 * mà không cần tự implements Listener hay tự registerEvents.
 */
public interface IGuiHandler {
    /**
     * Được GUIListener gọi khi người chơi click vào 1 ô trong đúng inventory
     * (đã được registerHandler với tiêu đề tương ứng). Sự kiện đã được setCancelled(true)
     * sẵn trước khi gọi vào đây.
     */
    void onClick(Player player, InventoryClickEvent event);
}
