package org.ThienNguyen.Webapi;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

import java.net.URL;
import java.util.Scanner;

public class UpdateListener implements Listener {
    private final Main plugin;
    private String latestVersionCache = null;

    public UpdateListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            String currentVersion = plugin.getDescription().getVersion();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    URL url = new URL("http://103.188.83.137/api/version.txt");
                    try (Scanner s = new Scanner(url.openStream()).useDelimiter("\\A")) {
                        if (s.hasNext()) {
                            String rawContent = s.next();
                            latestVersionCache = rawContent.split("\\s+")[0];

                            if (!currentVersion.equalsIgnoreCase(latestVersionCache)) {
                                String coloredMsg = ChatColor.translateAlternateColorCodes('&', rawContent);
                                sendFixedWrappedMessage(player, "§b§l[MyItem] §f", coloredMsg);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // FIX 1: Khóa cứng toàn bộ GUI dựa trên tiêu đề (An toàn 100%)
        if (title.equals("§0MyItem - Plugin Version") || title.equals("§0MyItem - Danh sách Update")) {
            event.setCancelled(true); // Không ai bốc được gì ra ngoài nữa

            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;

            Player player = (Player) event.getWhoClicked();
            var container = event.getCurrentItem().getItemMeta().getPersistentDataContainer();

            // Xử lý Click GUI Version (Mở danh sách)
            NamespacedKey actionOpenList = new NamespacedKey(plugin, "action_open_list");
            if (container.has(actionOpenList, PersistentDataType.BYTE)) {
                player.closeInventory();
                Update.openUpdateListGUI(player, plugin);
                return;
            }

            // Xử lý Click GUI Danh sách (Bắt đầu tải)
            NamespacedKey updateVerKey = new NamespacedKey(plugin, "update_ver");
            if (container.has(updateVerKey, PersistentDataType.STRING)) {
                String targetVersion = container.get(updateVerKey, PersistentDataType.STRING);
                player.closeInventory();
                Update.downloadAndUpdate(plugin, targetVersion, player);
                return;
            }

            // Xử lý nút Update bản mới nhất trực tiếp (nếu có)
            NamespacedKey actionKey = new NamespacedKey(plugin, "action_update");
            if (container.has(actionKey, PersistentDataType.STRING)) {
                player.closeInventory();
                String targetVersion = (latestVersionCache != null) ? latestVersionCache : "latest";
                Update.downloadAndUpdate(plugin, targetVersion, player);
            }
        }
    }

    private void sendFixedWrappedMessage(Player player, String prefix, String message) {
        int limit = 50;
        if (message.length() <= limit) {
            player.sendMessage(prefix + message);
            return;
        }

        int index = 0;
        while (index < message.length()) {
            int end = Math.min(index + limit, message.length());
            String subMessage = message.substring(index, end);
            player.sendMessage(prefix + subMessage);
            index = end;
        }
    }
}