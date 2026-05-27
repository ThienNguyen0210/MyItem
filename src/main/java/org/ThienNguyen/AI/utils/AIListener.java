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

        
        if (!player.hasMetadata("ai_prompt_mode")) {
            return;
        }

        List<MetadataValue> metaList = player.getMetadata("ai_prompt_mode");
        if (metaList.isEmpty()) {
            return;
        }

        
        event.setCancelled(true);

        
        String message = LegacyComponentSerializer.legacySection()
                .serialize(event.message())
                .trim();

        
        message = ChatColor.stripColor(message).trim();

        String profileId = metaList.get(0).asString();

        
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("huy")) {
            player.removeMetadata("ai_prompt_mode", Main.getInstance());
            player.sendMessage("§c§l[WindyAI] §7Đã hủy quá trình nhập prompt.");
            return;
        }

        
        player.removeMetadata("ai_prompt_mode", Main.getInstance());

        
        player.sendMessage("§b§l[WindyAI] §7Đang tiếp nhận yêu cầu với Profile: §e" + profileId);
        player.sendMessage("§f⚡ §7Hệ thống đang xử lý, vui lòng đợi trong giây lát...");

        
        Main.getInstance().getAiProcessor().handleItemCreation(player, message, profileId);
    }
}