package org.ThienNguyen.Consume;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ConsumeManager implements Listener {

    private static final String KEY_CONSUME = "consume_id";
    private final Random random = new Random();

    // Quản lý Cooldown
    private final Map<UUID, Map<String, Long>> cooldownMap = new HashMap<>();

    public static ItemStack getConsumeItem(String id, int amount) {
        FileConfiguration config = Main.getInstance().getConsumeConfig();
        if (!config.contains(id)) return null;

        Material mat = Material.matchMaterial(config.getString(id + ".material", "PAPER"));
        ItemStack item = new ItemStack(mat != null ? mat : Material.PAPER, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString(id + ".display-name", "Consume Item")));

            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList(id + ".lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);

            if (config.contains(id + ".model-id")) {
                meta.setCustomModelData(config.getInt(id + ".model-id"));
            }

            NamespacedKey key = new NamespacedKey(Main.getInstance(), KEY_CONSUME);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);

            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        // Kiểm tra hành động chuột phải
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), KEY_CONSUME);
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (id == null) return;

        e.setCancelled(true); // Chặn đặt block hoặc dùng vật phẩm gốc
        Player player = e.getPlayer();
        FileConfiguration config = Main.getInstance().getConsumeConfig();

        if (!config.contains(id)) return;

        // === XỬ LÝ COOLDOWN (Quan trọng: Kiểm tra trước khi trừ đồ) ===
        int cooldownSeconds = config.getInt(id + ".cooldown", 0);
        if (cooldownSeconds > 0) {
            long currentTime = System.currentTimeMillis();
            Map<String, Long> playerCooldowns = cooldownMap.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

            if (playerCooldowns.containsKey(id)) {
                long expireTime = playerCooldowns.get(id);
                if (currentTime < expireTime) {
                    double timeLeft = (expireTime - currentTime) / 1000.0;

                    // Lấy message từ LanguageManager
                    String msg = Main.getInstance().getLangManager().getMessage("consume-cooldown");
                    if (msg == null || msg.isEmpty()) msg = "&cBạn phải chờ %time%s nữa!";

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            msg.replace("%time%", String.format("%.1f", timeLeft))));
                    return; // DỪNG LẠI, không cho xài tiếp
                }
            }
            // Ghi nhận cooldown ngay lập tức để chặn spam
            playerCooldowns.put(id, currentTime + (cooldownSeconds * 1000L));
        }

        // === TRỪ VẬT PHẨM ===
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            // Fix lỗi bản 1.20.1: Đặt item chính là null hoặc không khí
            player.getInventory().setItemInMainHand(null);
        }

        // === THỰC THI LỆNH ===
        List<String> commandsToRun = new ArrayList<>();
        if (config.contains(id + ".random-commands")) {
            // Logic lấy random commands
            List<?> groups = config.getList(id + ".random-commands");
            if (groups != null && !groups.isEmpty()) {
                Object groupObj = groups.get(random.nextInt(groups.size()));
                if (groupObj instanceof ConfigurationSection section) {
                    for (String keyGroup : section.getKeys(false)) {
                        commandsToRun.addAll(section.getStringList(keyGroup));
                    }
                } else if (groupObj instanceof Map<?, ?> map) {
                    for (Object listObj : map.values()) {
                        if (listObj instanceof List<?> list) {
                            for (Object cmd : list) commandsToRun.add(String.valueOf(cmd));
                        }
                    }
                }
            }
        } else {
            commandsToRun.addAll(config.getStringList(id + ".commands"));
        }

        for (String cmd : commandsToRun) {
            executeCommand(player, cmd);
        }
    }

    private void executeCommand(Player player, String cmd) {
        String finalCmd = cmd.replace("%player%", player.getName());
        if (finalCmd.startsWith("[console]")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd.replace("[console] ", ""));
        } else if (finalCmd.startsWith("[op]")) {
            String commandOnly = finalCmd.replace("[op] ", "");
            boolean isAlreadyOp = player.isOp();
            try {
                if (!isAlreadyOp) player.setOp(true);
                player.performCommand(commandOnly);
            } finally {
                if (!isAlreadyOp) player.setOp(false);
            }
        } else if (finalCmd.startsWith("[player]")) {
            player.performCommand(finalCmd.replace("[player] ", ""));
        }
    }
}