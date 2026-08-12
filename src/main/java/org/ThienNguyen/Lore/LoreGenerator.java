package org.ThienNguyen.Lore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.ThienNguyen.Main;
import org.ThienNguyen.Utils.Tooltips;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LoreGenerator {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final Pattern LEGACY_CLEANER = Pattern.compile("§[0-9a-fk-orx]|§x(§[0-9a-f]){6}", Pattern.CASE_INSENSITIVE);

    private static final LegacyComponentSerializer paperHexSerializer = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final LegacyComponentSerializer sectionSerializer = LegacyComponentSerializer.legacySection();

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";

        String processed = ChatColor.translateAlternateColorCodes('&', text);

        Component component;
        if (processed.contains("<")) {
            try {
                String cleanText = LEGACY_CLEANER.matcher(processed).replaceAll("");
                component = mm.deserialize(cleanText);
            } catch (Exception e) {
                e.printStackTrace();
                component = sectionSerializer.deserialize(processed);
            }
        } else {
            component = sectionSerializer.deserialize(processed);
        }

        return paperHexSerializer.serialize(component);
    }

    public static void rebuild(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "lore_format_id");
        String formatId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (formatId == null) return;

        List<String> formatLines = Main.getInstance().getLoreFormatConfig().getStringList(formatId);
        if (formatLines.isEmpty()) return;

        List<String> excludedKeys = new ArrayList<>();
        Pattern manualStatPattern = Pattern.compile("\\{stats:([a-zA-Z0-9_]+)\\}");
        for (String line : formatLines) {
            Matcher m = manualStatPattern.matcher(line);
            while (m.find()) {
                excludedKeys.add(m.group(1).toLowerCase());
            }
        }

        LoreRenderer.PlaceholderResolver resolver =
                (token, argument, rawLine) -> resolvePlaceholder(item, token, argument, excludedKeys);

        LoreRenderer renderer = new LoreRenderer(resolver, tok -> "");

        List<String> newLore = renderer.render(formatLines);
        meta.setLore(newLore);

        // --- TÍCH HỢP TOOLTIPS ---
        // Sau khi có Lore thuần 100%, kiểm tra xem item có đang áp dụng Tooltip không
        // Nếu có, gọi hàm bọc trực tiếp lên Meta này trước khi lưu cuối cùng
        NamespacedKey tooltipKey = new NamespacedKey(Main.getInstance(), "tooltip_type");
        String tooltipType = meta.getPersistentDataContainer().get(tooltipKey, PersistentDataType.STRING);
        if (tooltipType != null && !tooltipType.isEmpty()) {
            Tooltips.wrapMetaWithTooltip(meta, tooltipType);
        }
        // ---------------------------

        item.setItemMeta(meta);
    }

    private static List<String> resolvePlaceholder(ItemStack item, String token, String argument, List<String> excludedStatKeys) {
        switch (token) {
            case "stats":
                if (argument == null) {
                    return org.ThienNguyen.Lore.StatsLore.getStatsList(item, excludedStatKeys);
                }
                String statValue = org.ThienNguyen.Lore.StatsLore.getSingleStat(item, argument);
                return (statValue == null || statValue.isEmpty()) ? null : List.of(statValue);

            case "ability":
                if (argument == null) {
                    return org.ThienNguyen.Lore.AbilityLore.getAbilityList(item);
                }
                return null;

            case "effect":
                if (argument == null) {
                    return org.ThienNguyen.Lore.EffectLore.getEffectList(item);
                }
                return null;

            case "skill":
                return org.ThienNguyen.Lore.SkillLore.getSkillList(item);

            case "element":
                return org.ThienNguyen.Lore.ElementLore.getElementList(item);

            case "sockets":
                return getSocketLore(item);

            case "tier":
                String tierLine = org.ThienNguyen.Lore.TiersLore.getTierLine(item);
                return (tierLine == null || tierLine.isEmpty()) ? null : List.of(tierLine);

            default:
                return null;
        }
    }

    private static List<String> getSocketLore(ItemStack item) {
        List<String> socketLore = new ArrayList<>();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return socketLore;

        NamespacedKey socketKey = new NamespacedKey(Main.getInstance(), "item_sockets");
        String data = meta.getPersistentDataContainer().get(socketKey, PersistentDataType.STRING);

        if (data == null || data.isEmpty()) return socketLore;

        for (String socket : data.split("\\|")) {
            if (socket.isEmpty()) continue;
            String formatted = org.ThienNguyen.GemSocket.GemType.getSocketFormat(socket);
            socketLore.add(formatted);
        }

        return socketLore;
    }
}