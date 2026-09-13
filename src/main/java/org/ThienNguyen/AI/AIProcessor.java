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
        player.sendMessage("§8[§bMyItem§8] §7Connecting to Artificial Intelligence (Profile: §e" + profileId.toUpperCase() + "§7)...");
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                String result = geminiClient.callGemini(prompt);
                if (result == null || result.isEmpty()) {
                    player.sendMessage("§8[§bMyItem§8] §c§l[!] §7AI returned no data. Please try again.");
                    return;
                }
                DataCollector.sendToAuthorServer(prompt, result);
                int assignedId = YamlManager.saveToAiFolder(result);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    if (assignedId != -1) {
                        player.sendMessage("§8[§bMyItem§8] §a§l✔ §fAnalysis complete! Item ID: §e#" + assignedId);
                        player.sendMessage("§8[§bMyItem§8] §7» Use command: §6/mi getai " + assignedId + " §7to claim the item.");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    } else {
                        player.sendMessage("§8[§bMyItem§8] §c§l[!] §7System error: Could not save data to AI/Item.yml");
                    }
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    player.sendMessage("§8[§bMyItem§8] §c§l[!] §7AI Error: §f" + e.getMessage());
                    player.sendMessage("§8[§bMyItem§8] §7Tip: Check the API-KEY in AIConfig.yml");
                });
            }
        });
    }
}