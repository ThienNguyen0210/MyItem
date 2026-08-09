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
            player.sendMessage("§8[§bMyItem§8] §7Prompt input process cancelled.");
            return;
        }

        
        player.removeMetadata("ai_prompt_mode", Main.getInstance());


        player.sendMessage("§8[§bMyItem§8] §7Receiving request with Profile: §e" + profileId);
        player.sendMessage("§8[§bMyItem§8] §f⚡ §7Processing, please wait a moment...");

        
        Main.getInstance().getAiProcessor().handleItemCreation(player, message, profileId);
    }
}