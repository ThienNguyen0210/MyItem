package org.ThienNguyen.Lore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.ThienNguyen.Main;
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

    /**
     * Thêm hỗ trợ & codes (rất quan trọng)
     */
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

        // Manually-specified {stats:xxx} keys are excluded from the {stats} catch-all list
        // so a stat doesn't get printed twice (once by name, once in the generic dump).
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

        // Xử lý loại bỏ {bar} và {sbar}, thay thế bằng chuỗi rỗng để giữ lại toàn bộ các ký tự trang trí khác trong dòng
        LoreRenderer renderer = new LoreRenderer(resolver, tok -> "");

        List<String> newLore = renderer.render(formatLines);

        meta.setLore(newLore);
        item.setItemMeta(meta);
    }

    /**
     * Central dispatch for every placeholder LoreRenderer encounters.
     * Returns RAW (un-colorized) text/lines - LoreRenderer calls colorize() itself.
     * Return null or an empty list to signal "nothing to show" (drops the line / group).
     */
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
                // TODO: needs a single-ability lookup (e.g. AbilityLore.getSingleAbility(item, argument))
                // to support {ability:xxx} the same way {stats:xxx} works. Not wired yet.
                return null;

            case "effect":
                if (argument == null) {
                    return org.ThienNguyen.Lore.EffectLore.getEffectList(item);
                }
                // TODO: needs a single-effect lookup (e.g. EffectLore.getSingleEffect(item, argument)).
                // Not wired yet.
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
                // #hash# style MMOItems placeholders: item-type, required-level, profession-*, etc.
                // TODO: not implemented yet - plug in whatever already computes these values
                // (item type name, level requirement, profession requirement, etc.) here.
                return null;
        }
    }
    /**
     * Lấy lore của phần Ngọc/Khảm (sockets)
     */
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