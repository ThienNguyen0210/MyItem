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

        
        List<String> excludedKeys = new ArrayList<>();
        Pattern manualStatPattern = Pattern.compile("\\{stats:([a-zA-Z0-9_]+)\\}");

        for (String line : formatLines) {
            if (line.contains("{stats:")) {
                Matcher m = manualStatPattern.matcher(line);
                while (m.find()) {
                    excludedKeys.add(m.group(1).toLowerCase());
                }
            }
        }

        List<String> newLore = new ArrayList<>();

        
        for (String line : formatLines) {

            
            if (line.contains("{stats}")) {
                newLore.addAll(processListLore(org.ThienNguyen.Lore.StatsLore.getStatsList(item, excludedKeys)));
                continue;
            }

            
            if (line.contains("{stats:")) {
                Matcher matcher = manualStatPattern.matcher(line);
                String processedLine = line;
                boolean hasAtLeastOneValue = false;

                while (matcher.find()) {
                    String statKey = matcher.group(1);
                    
                    String value = org.ThienNguyen.Lore.StatsLore.getSingleStat(item, statKey);

                    if (value != null && !value.isEmpty()) {
                        processedLine = processedLine.replace(matcher.group(0), value);
                        hasAtLeastOneValue = true;
                    }
                }

                
                if (hasAtLeastOneValue) {
                    newLore.add(colorize(processedLine));
                }
                continue;
            }

            
            if (line.contains("{ability}")) {
                newLore.addAll(processListLore(org.ThienNguyen.Lore.AbilityLore.getAbilityList(item)));
                continue;
            }
            if (line.contains("{effect}")) {
                newLore.addAll(processListLore(org.ThienNguyen.Lore.EffectLore.getEffectList(item)));
                continue;
            }
            if (line.contains("{skill}")) {
                newLore.addAll(processListLore(org.ThienNguyen.Lore.SkillLore.getSkillList(item)));
                continue;
            }
            if (line.contains("{element}")) {
                newLore.addAll(processListLore(org.ThienNguyen.Lore.ElementLore.getElementList(item)));
                continue;
            }
            if (line.contains("{sockets}")) {
                newLore.addAll(processListLore(getSocketLore(item)));
                continue;
            }

            
            String finalLine = line;
            if (finalLine.contains("{tier}")) {
                finalLine = finalLine.replace("{tier}", org.ThienNguyen.Lore.TiersLore.getTierLine(item));
            }

            newLore.add(colorize(finalLine));
        }

        meta.setLore(newLore);
        item.setItemMeta(meta);
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
    private static List<String> processListLore(List<String> list) {
        List<String> coloredList = new ArrayList<>();
        if (list == null) return coloredList;

        for (String s : list) {
            coloredList.add(colorize(s));
        }
        return coloredList;
    }
}