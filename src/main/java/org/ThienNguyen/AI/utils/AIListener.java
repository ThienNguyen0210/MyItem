package org.ThienNguyen.AI.utils;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;
import org.ThienNguyen.Main;

import java.util.List;

public class AIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Kiểm tra có đang ở chế độ AI không
        if (!player.hasMetadata("ai_prompt_mode")) {
            return;
        }

        List<MetadataValue> metaList = player.getMetadata("ai_prompt_mode");
        if (metaList.isEmpty()) {
            return;
        }

        // Hủy tin nhắn không cho hiện ra chat
        event.setCancelled(true);

        // Lấy nội dung chat thuần (cách ổn định nhất hiện tại)
        String message = LegacyComponentSerializer.legacySection()
                .serialize(event.message())
                .trim();

        // Xóa màu và khoảng trắng thừa
        message = ChatColor.stripColor(message).trim();

        String profileId = metaList.get(0).asString();

        // Lệnh hủy
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("huy")) {
            player.removeMetadata("ai_prompt_mode", Main.getInstance());
            player.sendMessage("§c§l[WindyAI] §7Đã hủy quá trình nhập prompt.");
            return;
        }

        // Xóa chế độ AI
        player.removeMetadata("ai_prompt_mode", Main.getInstance());

        // Thông báo
        player.sendMessage("§b§l[WindyAI] §7Đang tiếp nhận yêu cầu với Profile: §e" + profileId);
        player.sendMessage("§f⚡ §7Hệ thống đang xử lý, vui lòng đợi trong giây lát...");

        // Gửi prompt cho Gemini xử lý
        Main.getInstance().getAiProcessor().handleItemCreation(player, message, profileId);
    }
}