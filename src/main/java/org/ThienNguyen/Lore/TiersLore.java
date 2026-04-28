package org.ThienNguyen.Lore;

import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TiersLore {

    private static final NamespacedKey TIER_KEY = new NamespacedKey(Main.getInstance(), "item_tier_id");

    public static void applyTier(ItemStack item, String tierId) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), "lore_format_id");

        // Lưu tierId vào NBT để sau này rebuild() hoặc getTierLine() có thể lấy lại
        pdc.set(TIER_KEY, PersistentDataType.STRING, tierId);

        // --- LOGIC MỚI: Rẽ nhánh cho LoreFormat ---
        if (pdc.has(formatKey, PersistentDataType.STRING)) {
            org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
            return;
        }

        // --- LOGIC CŨ (LEGACY): Tìm và thay thế dòng Tier cũ ---
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        FileConfiguration config = Main.getInstance().getTiersConfig();
        if (config == null || config.getConfigurationSection("tiers") == null) {
            item.setItemMeta(meta);
            return;
        }

        String newTierLine = getTierLine(item);
        if (newTierLine.isEmpty()) {
            item.setItemMeta(meta);
            return;
        }

        boolean replaced = false;
        Set<String> allTierKeys = config.getConfigurationSection("tiers").getKeys(false);

        // Quét ngược từ dưới lên để tìm dòng Tier cũ và thay thế (Logic của mày)
        for (int i = lore.size() - 1; i >= 0; i--) {
            String currentLineStripped = ChatColor.stripColor(lore.get(i)).trim();

            for (String key : allTierKeys) {
                String tierDisplayName = config.getString("tiers." + key + ".display-name");
                if (tierDisplayName == null) continue;

                String tierStripped = ChatColor.stripColor(formatColor(tierDisplayName)).trim();

                if (currentLineStripped.equalsIgnoreCase(tierStripped)) {
                    lore.set(i, newTierLine);
                    replaced = true;
                    break;
                }
            }
            if (replaced) break;
        }

        // Nếu không tìm thấy dòng cũ, thêm vào cuối cùng
        if (!replaced) {
            lore.add(newTierLine);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * HÀM MỚI: Trả về dòng Tier duy nhất cho LoreFormat {tier}
     */
    public static String getTierLine(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";

        ItemMeta meta = item.getItemMeta();
        String tierId = meta.getPersistentDataContainer().get(TIER_KEY, PersistentDataType.STRING);

        if (tierId == null) return "";

        FileConfiguration config = Main.getInstance().getTiersConfig();
        if (config == null) return "";

        String rawTier = config.getString("tiers." + tierId + ".display-name");
        return rawTier != null ? formatColor(rawTier) : "";
    }

    private static String formatColor(String text) {
        if (text == null || text.isEmpty()) return text;
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}