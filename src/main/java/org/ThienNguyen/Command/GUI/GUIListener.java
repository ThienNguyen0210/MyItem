package org.ThienNguyen.Command.GUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
/**
 * Listener DUY NHẤT cho toàn bộ hệ thống GUI trong plugin.
 *
 * Chỉ cần đăng ký 1 LẦN DUY NHẤT trong onEnable() của plugin chính:
 *   GUIListener.init(this);
 *
 * Sau đó, mỗi lớp GUI mới (GUIStats, GUIEffect, GUIElement, ...) chỉ cần:
 *   1. implements IGuiHandler
 *   2. Gọi GUIListener.registerHandler("Tiêu Đề GUI", this); (thường đặt trong static block)
 * mà KHÔNG cần tự implements Listener hay gọi registerEvents() thêm lần nào nữa.
 *
 * Ngoài ra GUIListener còn xử lý sẵn cơ chế "đóng GUI, chờ người chơi gõ chat để nhập giá trị,
 * rồi trả kết quả về qua callback" dùng chung cho mọi GUI, thông qua requestChatInput(...).
 */
public class GUIListener implements Listener {
    private static Plugin plugin;
    private static final Map<String, IGuiHandler> handlers = new HashMap<>();
    private static final Map<UUID, ChatInputRequest> awaitingChat = new HashMap<>();
    /**
     * Gọi 1 lần duy nhất trong onEnable() của plugin chính.
     */
    public static void init(Plugin pluginInstance) {
        if (plugin != null) return; // tránh đăng ký trùng nếu lỡ gọi init() nhiều lần
        plugin = pluginInstance;
        Bukkit.getPluginManager().registerEvents(new GUIListener(), plugin);
    }
    /**
     * Mỗi lớp GUI gọi hàm này (thường trong static block) để đăng ký xử lý click
     * cho inventory có tiêu đề (title) tương ứng.
     */
    public static void registerHandler(String guiTitle, IGuiHandler handler) {
        handlers.put(guiTitle, handler);
    }
    /**
     * Yêu cầu 1 người chơi nhập giá trị qua chat. GUI của bạn nên đóng inventory
     * trước khi gọi hàm này. Khi người chơi gõ chat (không phải từ khoá huỷ),
     * onInput sẽ được gọi (đã đảm bảo chạy trên main thread). Nếu người chơi gõ
     * đúng từ khoá huỷ, onCancel sẽ được gọi thay vào đó.
     */
    public static void requestChatInput(Player player, String cancelKeyword,
                                         Consumer<String> onInput, Runnable onCancel) {
        awaitingChat.put(player.getUniqueId(), new ChatInputRequest(cancelKeyword, onInput, onCancel));
    }
    /**
     * Huỷ yêu cầu nhập chat đang chờ của 1 người chơi (nếu có), ví dụ khi họ thoát server.
     */
    public static void cancelChatInput(UUID uuid) {
        awaitingChat.remove(uuid);
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        IGuiHandler handler = handlers.get(title);
        if (handler == null) return; // không phải GUI do hệ thống này quản lý -> bỏ qua
        event.setCancelled(true); // không cho lấy item ra khỏi bất kỳ GUI nào đã đăng ký
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        handler.onClick(player, event);
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ChatInputRequest request = awaitingChat.remove(uuid);
        if (request == null) return; // người chơi không ở trạng thái chờ nhập -> để chat chạy bình thường
        event.setCancelled(true); // không cho tin nhắn nhập giá trị hiển thị lên chat chung
        String message = event.getMessage().trim();
        Runnable task = () -> {
            if (request.cancelKeyword != null && message.equalsIgnoreCase(request.cancelKeyword)) {
                if (request.onCancel != null) request.onCancel.run();
            } else if (request.onInput != null) {
                request.onInput.accept(message);
            }
        };
        if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, task);
        } else {
            task.run();
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        awaitingChat.remove(event.getPlayer().getUniqueId());
    }
    private static class ChatInputRequest {
        final String cancelKeyword;
        final Consumer<String> onInput;
        final Runnable onCancel;
        ChatInputRequest(String cancelKeyword, Consumer<String> onInput, Runnable onCancel) {
            this.cancelKeyword = cancelKeyword;
            this.onInput = onInput;
            this.onCancel = onCancel;
        }
    }
}
