package org.ThienNguyen.AI;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.ThienNguyen.AI.utils.GeminiClient;
import org.ThienNguyen.AI.utils.DataCollector;
import org.ThienNguyen.AI.utils.YamlManager;
import java.util.concurrent.CompletableFuture;

public class AIProcessor {
    private final GeminiClient geminiClient;

    public AIProcessor() {
        this.geminiClient = new GeminiClient();
    }

    public void handleItemCreation(Player player, String prompt, String profileId) {
        player.sendMessage("§b§l[WindyAI] §7Đang kết nối trí tuệ nhân tạo (Profile: §e" + profileId.toUpperCase() + "§7)...");

        // Sử dụng runTaskAsynchronously để không làm treo server khi đợi API phản hồi
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                // 1. Gọi GeminiClient với 2 tham số (đã sửa ở bước trước)
                // Hàm này sẽ tự động check API Key trong config hoặc dùng Hard-code
                String result = geminiClient.callGemini(prompt);

                // 2. Kiểm tra kết quả trả về
                if (result == null || result.isEmpty()) {
                    player.sendMessage("§c§l[!] §7AI không trả về dữ liệu. Hãy thử lại.");
                    return;
                }

                // 3. Chạy các tác vụ liên quan đến File và Web (nên chạy Async tiếp)
                // Gửi data về Server Web của bạn: 103.188.83.137/AI/CollectData
                DataCollector.sendToAuthorServer(prompt, result);

                // Lưu vào file AI/Item.yml và lấy ID
                int assignedId = YamlManager.saveToAiFolder(result);

                // 4. Quay về Main Thread để gửi tin nhắn và thực hiện các lệnh Bukkit an toàn
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    if (assignedId != -1) {
                        player.sendMessage("§a§l✔ §fPhân tích hoàn tất! Mã số vật phẩm: §e#" + assignedId);
                        player.sendMessage("§7» Dùng lệnh: §6/mi getai " + assignedId + " §7để nhận item.");

                        // Hiệu ứng nhỏ cho người chơi
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    } else {
                        player.sendMessage("§c§l[!] §7Lỗi hệ thống: Không thể lưu dữ liệu vào AI/Item.yml");
                    }
                });

            } catch (Exception e) {
                // Xử lý lỗi (API Key sai, mất mạng, model lỗi...)
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    player.sendMessage("§c§l[!] §7Lỗi AI: §f" + e.getMessage());
                    player.sendMessage("§7Gợi ý: Kiểm tra lại API-KEY trong AIConfig.yml");
                });
            }
        });
    }
}