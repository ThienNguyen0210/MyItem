package org.ThienNguyen.Command;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.ThienNguyen.Lore.LoreGenerator;
import org.ThienNguyen.Utils.Tooltips;

public class ItemStorageManager {

    private final Main plugin;
    private final File folder;
    private final Map<String, ItemStack> itemCache = new HashMap<>();

    // Namespace mặc định dùng cho PDC (theo ví dụ của bạn)
    private static final String PDC_NAMESPACE = "myitem";

    // Các key điều khiển của hệ thống Lore/Tooltip (LoreGenerator + Tooltips)
    // Lưu riêng dưới "lore-format" / "tooltip", KHÔNG đi qua nhánh stats chung
    private static final String KEY_LORE_FORMAT_ID = "lore_format_id";
    private static final String KEY_TOOLTIP_TYPE   = "tooltip_type";
    private static final String KEY_ORIGINAL_NAME  = "original_name";
    private static final String KEY_ORIGINAL_LORE  = "original_lore";

    // Danh sách stat (lấy từ Tab + các key phổ biến)
    private static final Set<String> STAT_KEYS = new HashSet<>(Arrays.asList(
            "damage", "health", "armor", "pve_damage", "pvp_damage",
            "pve_defense", "pvp_defense", "critical_chance", "critical_damage",
            "lifesteal", "dodge_rate", "block_rate", "penetration", "level_require",
            "true_damage", "thorns", "class_require", "max_mana", "mana_regen",
            "exp_bonus", "attack_speed", "movement_speed", "health_regen",
            "armor_pen", "all_damage", "all_defense", "bow_damage",
            "knockback_resistance", "death_damage", "durability",
            "magic_damage", "magic_defense", "Accuracy",
            "critical_damage_reduction", "damage_reduction", "effect_resistance"
    ));

    public ItemStorageManager(Main plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "ManagerItem");
        if (!this.folder.exists()) {
            this.folder.mkdirs();
        }
        loadAllItems();
    }

    public void loadAllItems() {
        itemCache.clear();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String id : config.getKeys(false)) {
                ItemStack item = buildItemFromConfig(config, id);
                if (item != null) {
                    itemCache.put(id.toLowerCase(), item);
                }
            }
        }

        // Dời việc render lore-format/tooltip sang tick kế tiếp (xem lý do
        // chi tiết trong buildItemFromConfig). Lúc này toàn bộ config của
        // plugin chắc chắn đã load xong nên gọi TiersLore/StatsLore/... an toàn.
        Bukkit.getScheduler().runTask(plugin, this::regenerateFormattedItems);
    }

    /**
     * Render lại lore/tooltip cho các item có gắn "lore-format" và/hoặc
     * "tooltip" trong cache, dựa trên config Lore/Tooltip HIỆN TẠI.
     * Được gọi 1 tick sau loadAllItems() để tránh đệ quy khi khởi động plugin.
     */
    private void regenerateFormattedItems() {
        NamespacedKey formatKey = new NamespacedKey(Main.getInstance(), KEY_LORE_FORMAT_ID);
        NamespacedKey tooltipKey = new NamespacedKey(Main.getInstance(), KEY_TOOLTIP_TYPE);

        for (ItemStack item : itemCache.values()) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            PersistentDataContainer itemPdc = meta.getPersistentDataContainer();
            String tooltipType = itemPdc.get(tooltipKey, PersistentDataType.STRING);
            String loreFormatId = itemPdc.get(formatKey, PersistentDataType.STRING);

            if (tooltipType != null && !tooltipType.isEmpty()) {
                // reapplyTooltipSilent tự xử lý cả 2 trường hợp: có/không có lore-format đi kèm
                Tooltips.reapplyTooltipSilent(item, tooltipType);
            } else if (loreFormatId != null && !loreFormatId.isEmpty()) {
                LoreGenerator.rebuild(item);
            }
        }
    }

    public ItemStack getItem(String id) {
        ItemStack item = itemCache.get(id.toLowerCase());
        return (item != null) ? item.clone() : null;
    }

    public boolean createTypeFile(String type) {
        File file = new File(folder, type + ".yml");
        if (file.exists()) return false;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().warning("Khong the tao file: " + type + ".yml");
            return false;
        }
    }

    /**
     * Lưu ItemStack vào file yml + cập nhật cache
     * Lore: § → &
     * PDC được tách thành: stats / element / effect / ability
     */
    public boolean saveItemToType(String type, String id, ItemStack item) {
        File file = new File(folder, type + ".yml");
        if (!file.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String path = id + ".";

        // ── Cơ bản ────────────────────────────────────────────────
        config.set(path + "material", item.getType().name());

        // ── Lore-format / Tooltip: lưu ID để có thể chỉnh sửa & áp lại sau này ──
        PersistentDataContainer formatPdc = meta.getPersistentDataContainer();
        String loreFormatId = formatPdc.get(new NamespacedKey(Main.getInstance(), KEY_LORE_FORMAT_ID), PersistentDataType.STRING);
        String tooltipType  = formatPdc.get(new NamespacedKey(Main.getInstance(), KEY_TOOLTIP_TYPE), PersistentDataType.STRING);
        boolean isGenerated = (loreFormatId != null && !loreFormatId.isEmpty())
                || (tooltipType != null && !tooltipType.isEmpty());

        config.set(path + "lore-format", (loreFormatId != null && !loreFormatId.isEmpty()) ? loreFormatId : null);
        config.set(path + "tooltip", (tooltipType != null && !tooltipType.isEmpty()) ? tooltipType : null);

        // Tên: nếu item đang được bọc format/tooltip, tên hiển thị đã bị chèn
        // ký tự trang trí (icon/fill) → ưu tiên lưu tên GỐC (original_name) cho sạch.
        String nameToSave = null;
        if (isGenerated) {
            String rawOriginalName = formatPdc.get(new NamespacedKey(Main.getInstance(), KEY_ORIGINAL_NAME), PersistentDataType.STRING);
            if (rawOriginalName != null && !rawOriginalName.isEmpty()) nameToSave = rawOriginalName;
        }
        if (nameToSave == null && meta.hasDisplayName()) {
            nameToSave = meta.getDisplayName();
        }
        config.set(path + "name", nameToSave);

        if (meta.hasCustomModelData()) {
            config.set(path + "model-id", meta.getCustomModelData());
        }

        // Lore: § → &
        // Nếu item đang được bọc format/tooltip, dòng lore hiện tại (meta.getLore())
        // đã bị chèn ký tự trang trí (fill-character, icon...) — không lưu bản đó.
        // Thay vào đó lấy lore GỐC (original_lore, được Tooltips lưu lại trước khi bọc)
        // để file yml giữ nội dung sạch, dễ chỉnh sửa, và áp lại được format khác sau này.
        List<String> loreToSave = null;
        if (isGenerated) {
            String rawOriginalLore = formatPdc.get(new NamespacedKey(Main.getInstance(), KEY_ORIGINAL_LORE), PersistentDataType.STRING);
            if (rawOriginalLore != null) {
                loreToSave = rawOriginalLore.isEmpty()
                        ? new ArrayList<>()
                        : new ArrayList<>(Arrays.asList(rawOriginalLore.split(Pattern.quote(Tooltips.SEPARATOR))));
            }
        }
        if (loreToSave == null && meta.hasLore()) {
            loreToSave = meta.getLore();
        }

        if (loreToSave != null && !loreToSave.isEmpty()) {
            List<String> lore = loreToSave.stream()
                    .map(line -> line.replace("§", "&"))
                    .collect(Collectors.toList());
            config.set(path + "lore", lore);
        } else {
            config.set(path + "lore", null);
        }

        // Enchantments
        if (!item.getEnchantments().isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                config.set(path + "enchants." + entry.getKey().getKey().getKey(), entry.getValue());
            }
        }

        // Attributes
        if (meta.hasAttributeModifiers()) {
            for (Attribute attr : meta.getAttributeModifiers().keySet()) {
                int index = 0;
                for (AttributeModifier mod : meta.getAttributeModifiers(attr)) {
                    String attrPath = path + "attributes." + attr.name() + "." + index++;
                    config.set(attrPath + ".name", mod.getName());
                    config.set(attrPath + ".amount", mod.getAmount());
                    config.set(attrPath + ".operation", mod.getOperation().name());

                    // Chỉ lưu slot khi không phải null / ALL / ANY
                    EquipmentSlot slot = mod.getSlot();
                    if (slot != null) {
                        String slotName = slot.name();
                        if (!slotName.equalsIgnoreCase("ALL") && !slotName.equalsIgnoreCase("ANY")) {
                            config.set(attrPath + ".slot", slotName);
                        }
                    }
                    // nếu slot == null hoặc ALL/ANY → không ghi key .slot
                }
            }
        }

        // Flags
        if (!meta.getItemFlags().isEmpty()) {
            List<String> flags = new ArrayList<>();
            for (ItemFlag flag : meta.getItemFlags()) flags.add(flag.name());
            config.set(path + "flags", flags);
        } else {
            config.set(path + "flags", null);
        }

        // Unbreakable ("indestructible") — tách riêng với ItemFlag, phải lưu thủ công
        if (meta.isUnbreakable()) {
            config.set(path + "unbreakable", true);
        } else {
            config.set(path + "unbreakable", null);
        }

        // ── PDC → stats / element / effect / ability ──────────────
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.getKeys().isEmpty()) {

            // Xóa section cũ nếu có (để tránh trùng)
            config.set(path + "stats", null);
            config.set(path + "element", null);
            config.set(path + "effect", null);
            config.set(path + "ability", null);
            config.set(path + "pdc", null);

            for (NamespacedKey key : pdc.getKeys()) {
                String fullKey = key.getKey();

                // Các key điều khiển Lore-format/Tooltip đã được lưu riêng ở trên (lore-format/tooltip)
                // → bỏ qua để không bị quét nhầm vào "stats"
                if (fullKey.equalsIgnoreCase(KEY_LORE_FORMAT_ID)
                        || fullKey.equalsIgnoreCase(KEY_TOOLTIP_TYPE)
                        || fullKey.equalsIgnoreCase(KEY_ORIGINAL_NAME)
                        || fullKey.equalsIgnoreCase(KEY_ORIGINAL_LORE)) {
                    continue;
                }

                Object value = null;
                if (pdc.has(key, PersistentDataType.STRING)) {
                    value = pdc.get(key, PersistentDataType.STRING);
                } else if (pdc.has(key, PersistentDataType.INTEGER)) {
                    value = pdc.get(key, PersistentDataType.INTEGER);
                } else if (pdc.has(key, PersistentDataType.DOUBLE)) {
                    value = pdc.get(key, PersistentDataType.DOUBLE);
                } else if (pdc.has(key, PersistentDataType.BYTE)) {
                    value = pdc.get(key, PersistentDataType.BYTE);
                } else if (pdc.has(key, PersistentDataType.FLOAT)) {
                    value = pdc.get(key, PersistentDataType.FLOAT);
                }

                if (value == null) continue;

                // ★ Nếu value là "any" → bỏ qua, không lưu vào yml
                if (value instanceof String && ((String) value).equalsIgnoreCase("any")) {
                    continue;
                }

                // 1. Ability
                if (fullKey.equalsIgnoreCase("item_abilities")) {
                    String raw = value.toString();
                    if (!raw.isEmpty()) {
                        for (String part : raw.split(",")) {
                            String[] bits = part.trim().split(":", 2);
                            if (bits.length >= 2) {
                                config.set(path + "ability." + bits[0].trim(), bits[1].trim());
                            }
                        }
                    }
                }
                // 2. Effect
                else if (fullKey.equalsIgnoreCase("item_effects_map")) {
                    String raw = value.toString();
                    if (!raw.isEmpty()) {
                        for (String part : raw.split(";")) {
                            if (part.trim().isEmpty()) continue;
                            String[] bits = part.trim().split(":", 2);
                            if (bits.length >= 2) {
                                try {
                                    config.set(path + "effect." + bits[0].trim(), Integer.parseInt(bits[1].trim()));
                                } catch (NumberFormatException e) {
                                    config.set(path + "effect." + bits[0].trim(), bits[1].trim());
                                }
                            }
                        }
                    }
                }
                // 3. Element (elem_xxx)
                else if (fullKey.toLowerCase().startsWith("elem_")) {
                    String elemName = fullKey.substring(5);
                    config.set(path + "element." + elemName, value);
                }
                // 4. Stats (theo danh sách)
                else if (STAT_KEYS.contains(fullKey.toLowerCase()) || STAT_KEYS.contains(fullKey)) {
                    config.set(path + "stats." + fullKey, value);
                }
                // 5. Các key khác → đưa vào stats
                else {
                    config.set(path + "stats." + fullKey, value);
                }
            }
        }

        try {
            config.save(file);
            itemCache.put(id.toLowerCase(), item.clone());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Dựng ItemStack từ config (hỗ trợ cả format cũ pdc: lẫn format mới stats/element/effect/ability)
     */
    private ItemStack buildItemFromConfig(FileConfiguration config, String id) {
        String path = id + ".";
        String materialName = config.getString(path + "material");
        if (materialName == null) return null;

        Material material = Material.getMaterial(materialName);
        if (material == null) return null;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Cơ bản
        if (config.contains(path + "name")) {
            meta.setDisplayName(config.getString(path + "name").replace("&", "§"));
        }
        if (config.contains(path + "model-id")) {
            meta.setCustomModelData(config.getInt(path + "model-id"));
        }
        if (config.contains(path + "lore")) {
            List<String> lore = config.getStringList(path + "lore");
            lore.replaceAll(line -> line.replace("&", "§"));
            meta.setLore(lore);
        }

        // Enchantments
        if (config.contains(path + "enchants")) {
            ConfigurationSection enchantSection = config.getConfigurationSection(path + "enchants");
            if (enchantSection != null) {
                for (String key : enchantSection.getKeys(false)) {
                    Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase()));
                    if (enchant != null) {
                        meta.addEnchant(enchant, enchantSection.getInt(key), true);
                    }
                }
            }
        }

        // Attributes
        if (config.contains(path + "attributes")) {
            ConfigurationSection attrSection = config.getConfigurationSection(path + "attributes");
            if (attrSection != null) {
                for (String attrKey : attrSection.getKeys(false)) {
                    try {
                        Attribute attribute = Attribute.valueOf(attrKey);
                        ConfigurationSection modSection = attrSection.getConfigurationSection(attrKey);
                        if (modSection != null) {
                            for (String modKey : modSection.getKeys(false)) {
                                String name = modSection.getString(modKey + ".name");
                                double amount = modSection.getDouble(modKey + ".amount");
                                AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(
                                        modSection.getString(modKey + ".operation"));
                                String slotStr = modSection.getString(modKey + ".slot");
                                EquipmentSlot slot = (slotStr == null || slotStr.equalsIgnoreCase("ALL"))
                                        ? null : EquipmentSlot.valueOf(slotStr);
                                AttributeModifier modifier = new AttributeModifier(
                                        UUID.randomUUID(),
                                        name != null ? name : "attr",
                                        amount, op, slot);
                                meta.addAttributeModifier(attribute, modifier);
                            }
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        // Flags
        if (config.contains(path + "flags")) {
            for (String flagName : config.getStringList(path + "flags")) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flagName));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Unbreakable ("indestructible")
        if (config.contains(path + "unbreakable")) {
            meta.setUnbreakable(config.getBoolean(path + "unbreakable"));
        }

        // ── PDC (hỗ trợ cả format mới + format cũ) ────────────────
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 1. Format mới: stats
        if (config.contains(path + "stats")) {
            ConfigurationSection sec = config.getConfigurationSection(path + "stats");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    setPdcValue(pdc, key, sec.get(key));
                }
            }
        }

        // 2. Format mới: element → lưu thành elem_<name>
        if (config.contains(path + "element")) {
            ConfigurationSection sec = config.getConfigurationSection(path + "element");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    setPdcValue(pdc, "elem_" + key, sec.get(key));
                }
            }
        }

        // 3. Format mới: effect → ghép lại thành item_effects_map
        if (config.contains(path + "effect")) {
            ConfigurationSection sec = config.getConfigurationSection(path + "effect");
            if (sec != null) {
                StringBuilder sb = new StringBuilder();
                for (String key : sec.getKeys(false)) {
                    sb.append(key).append(":").append(sec.get(key)).append(";");
                }
                if (sb.length() > 0) {
                    setPdcValue(pdc, "item_effects_map", sb.toString());
                }
            }
        }

        // 4. Format mới: ability → ghép lại thành item_abilities
        if (config.contains(path + "ability")) {
            ConfigurationSection sec = config.getConfigurationSection(path + "ability");
            if (sec != null) {
                StringBuilder sb = new StringBuilder();
                for (String key : sec.getKeys(false)) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(key).append(":").append(sec.get(key));
                }
                if (sb.length() > 0) {
                    setPdcValue(pdc, "item_abilities", sb.toString());
                }
            }
        }

        // 5. Format cũ (pdc:) – vẫn hỗ trợ để tương thích ngược
        if (config.contains(path + "pdc")) {
            ConfigurationSection pdcSection = config.getConfigurationSection(path + "pdc");
            if (pdcSection != null) {
                for (String keyStr : pdcSection.getKeys(false)) {
                    String[] parts = keyStr.split(":");
                    NamespacedKey key = (parts.length > 1)
                            ? new NamespacedKey(parts[0], parts[1])
                            : new NamespacedKey(PDC_NAMESPACE, parts[0]);
                    Object value = pdcSection.get(keyStr);
                    if (value instanceof String) pdc.set(key, PersistentDataType.STRING, (String) value);
                    else if (value instanceof Integer) pdc.set(key, PersistentDataType.INTEGER, (Integer) value);
                    else if (value instanceof Double) pdc.set(key, PersistentDataType.DOUBLE, (Double) value);
                    else if (value instanceof Byte) pdc.set(key, PersistentDataType.BYTE, (Byte) value);
                    else if (value instanceof Float) pdc.set(key, PersistentDataType.FLOAT, (Float) value);
                }
            }
        }

        // ── Lore-format / Tooltip: ghi ID điều khiển vào PDC ─────────────────
        // CHÚ Ý: KHÔNG render/rebuild lore ngay tại đây. buildItemFromConfig()
        // chạy bên trong loadAllItems(), tức là bên trong constructor của
        // ItemStorageManager - lúc này các config khác của plugin (vd: Tiers)
        // có thể chưa sẵn sàng. Nếu resolvePlaceholder() cần đến 1 config chưa
        // load, nó có thể vô tình kích hoạt Main#reloadPluginConfigs(), mà hàm
        // đó lại tạo mới ItemStorageManager -> gọi lại loadAllItems() -> đệ quy
        // vô hạn (StackOverflowError). Vì vậy ở đây chỉ gắn PDC, còn việc
        // render thật sự được dời sang tick kế tiếp trong loadAllItems().
        String loreFormatId = config.getString(path + "lore-format");
        String tooltipType  = config.getString(path + "tooltip");

        if (loreFormatId != null && !loreFormatId.isEmpty()) {
            pdc.set(new NamespacedKey(Main.getInstance(), KEY_LORE_FORMAT_ID), PersistentDataType.STRING, loreFormatId);
        }
        if (tooltipType != null && !tooltipType.isEmpty()) {
            pdc.set(new NamespacedKey(Main.getInstance(), KEY_TOOLTIP_TYPE), PersistentDataType.STRING, tooltipType);
        }

        item.setItemMeta(meta);
        return item;
    }

    /** Helper: set giá trị vào PDC với namespace myitem */
    private void setPdcValue(PersistentDataContainer pdc, String key, Object value) {
        NamespacedKey nsKey = new NamespacedKey(PDC_NAMESPACE, key);
        if (value instanceof String) {
            pdc.set(nsKey, PersistentDataType.STRING, (String) value);
        } else if (value instanceof Integer) {
            pdc.set(nsKey, PersistentDataType.INTEGER, (Integer) value);
        } else if (value instanceof Double) {
            pdc.set(nsKey, PersistentDataType.DOUBLE, (Double) value);
        } else if (value instanceof Float) {
            pdc.set(nsKey, PersistentDataType.FLOAT, (Float) value);
        } else if (value instanceof Byte) {
            pdc.set(nsKey, PersistentDataType.BYTE, (Byte) value);
        } else if (value instanceof Number) {
            // fallback
            pdc.set(nsKey, PersistentDataType.DOUBLE, ((Number) value).doubleValue());
        }
    }

    public List<String> getTypeNames() {
        List<String> types = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return types;
        for (File f : files) {
            types.add(f.getName().replace(".yml", ""));
        }
        return types;
    }

    public List<String> getIdsByType(String type) {
        List<String> ids = new ArrayList<>();
        File file = new File(folder, type + ".yml");
        if (!file.exists()) return ids;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ids.addAll(config.getKeys(false));
        return ids;
    }

    public List<String> getAllIds() {
        return new ArrayList<>(itemCache.keySet());
    }
}