package org.ThienNguyen.Command.GUI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
/**
 * Mọi lớp GUI (GUIStats, GUIEffect, GUIElement, ...) implement interface này
 * và đăng ký với GUIListener.registerHandler(title, this) để được nhận sự kiện click,
 * mà không cần tự implements Listener hay tự registerEvents.
 *
 * Trong đa số trường hợp, các GUI dạng danh sách/phân trang nên extends
 * {@link AbstractPaginatedGui} thay vì implement trực tiếp interface này —
 * lớp đó đã lo sẵn phần khung (header/content/footer, phân trang, map click -> entry).
 */
public interface IGuiHandler {
    /**
     * Được GUIListener gọi khi người chơi click vào 1 ô trong đúng inventory
     * (đã được registerHandler với tiêu đề tương ứng). Sự kiện đã được setCancelled(true)
     * sẵn trước khi gọi vào đây.
     */
    void onClick(Player player, InventoryClickEvent event);

    /**
     * Được GUIListener gọi khi người chơi đóng inventory của GUI này (kể cả khi thoát server).
     * Mặc định không làm gì; ghi đè nếu GUI cần dọn dẹp trạng thái (VD: nhớ trang đang xem,
     * huỷ 1 tác vụ đang chờ, ...).
     */
    default void onClose(Player player) {
    }
}