package org.ThienNguyen.Command;

import org.ThienNguyen.Main;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemStorageManager {

    private final Main plugin;
    private final File folder;
    // Cache để lưu trữ item giúp load nhanh không cần đọc file nhiều lần
    private final Map<String, ItemStack> itemCache = new HashMap<>();

    public ItemStorageManager(Main plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "ManagerItem");
        if (!this.folder.exists()) {
            this.folder.mkdirs();
        }
        loadAllItems(); // Tự động nạp item khi khởi tạo
    }

    /**
     * Quét sạch cache và nạp lại toàn bộ item từ tất cả file .yml trong ManagerItem
     */
    public void loadAllItems() {
        itemCache.clear();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        int count = 0;
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String id : config.getKeys(false)) {
                ItemStack item = buildItemFromConfig(config, id);
                if (item != null) {
                    itemCache.put(id.toLowerCase(), item);
                    count++;
                }
            }
        }
        plugin.getLogger().info("§a[MyItem] Đã nạp thành công " + count + " items từ thư mục ManagerItem!");
    }

    /**
     * Lấy item từ bộ nhớ tạm (Cache)
     */
    public ItemStack getItem(String id) {
        ItemStack item = itemCache.get(id.toLowerCase());
        return (item != null) ? item.clone() : null;
    }

    /**
     * Tạo file .yml mới
     */
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
     * Lưu ItemStack vào file yml và cập nhật lại cache
     */
    public boolean saveItemToType(String type, String id, ItemStack item) {
        File file = new File(folder, type + ".yml");
        if (!file.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String path = id + ".";

        // Lưu thông tin
        config.set(path + "material", item.getType().name());
        if (meta.hasDisplayName()) config.set(path + "name", meta.getDisplayName());
        if (meta.hasCustomModelData()) config.set(path + "model-id", meta.getCustomModelData());
        if (meta.hasLore()) config.set(path + "lore", meta.getLore());

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
                    config.set(attrPath + ".slot", mod.getSlot() != null ? mod.getSlot().name() : "ALL");
                }
            }
        }

        // Flags
        if (!meta.getItemFlags().isEmpty()) {
            List<String> flags = new ArrayList<>();
            for (ItemFlag flag : meta.getItemFlags()) flags.add(flag.name());
            config.set(path + "flags", flags);
        }

        // PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.getKeys().isEmpty()) {
            ConfigurationSection pdcSection = config.createSection(path + "pdc");
            for (NamespacedKey key : pdc.getKeys()) {
                if (pdc.has(key, PersistentDataType.STRING)) {
                    pdcSection.set(key.toString(), pdc.get(key, PersistentDataType.STRING));
                } else if (pdc.has(key, PersistentDataType.INTEGER)) {
                    pdcSection.set(key.toString(), pdc.get(key, PersistentDataType.INTEGER));
                } else if (pdc.has(key, PersistentDataType.DOUBLE)) {
                    pdcSection.set(key.toString(), pdc.get(key, PersistentDataType.DOUBLE));
                } else if (pdc.has(key, PersistentDataType.BYTE)) {
                    pdcSection.set(key.toString(), pdc.get(key, PersistentDataType.BYTE));
                }
            }
        }

        try {
            config.save(file);
            // Sau khi lưu thành công, cập nhật luôn vào cache để dùng ngay không cần reload
            itemCache.put(id.toLowerCase(), item.clone());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xử lý chuyển đổi từ config sang ItemStack (Dùng cho cả load lẻ và load all)
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
        if (config.contains(path + "name")) meta.setDisplayName(config.getString(path + "name").replace("&", "§"));
        if (config.contains(path + "model-id")) meta.setCustomModelData(config.getInt(path + "model-id"));
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
                    if (enchant != null) meta.addEnchant(enchant, enchantSection.getInt(key), true);
                }
            }
        }

        // Attributes
        if (config.contains(path + "attributes")) {
            ConfigurationSection attrSection = config.getConfigurationSection(path + "attributes");
            if (attrSection != null) {
                for (String attrKey : attrSection.getKeys(false)) {
                    Attribute attribute = Attribute.valueOf(attrKey);
                    ConfigurationSection modSection = attrSection.getConfigurationSection(attrKey);
                    if (modSection != null) {
                        for (String modKey : modSection.getKeys(false)) {
                            String name = modSection.getString(modKey + ".name");
                            double amount = modSection.getDouble(modKey + ".amount");
                            AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(modSection.getString(modKey + ".operation"));
                            String slotStr = modSection.getString(modKey + ".slot");
                            EquipmentSlot slot = (slotStr == null || slotStr.equals("ALL")) ? null : EquipmentSlot.valueOf(slotStr);
                            AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), name != null ? name : "attr", amount, op, slot);
                            meta.addAttributeModifier(attribute, modifier);
                        }
                    }
                }
            }
        }

        // Flags
        if (config.contains(path + "flags")) {
            for (String flagName : config.getStringList(path + "flags")) {
                meta.addItemFlags(ItemFlag.valueOf(flagName));
            }
        }

        // PDC
        if (config.contains(path + "pdc")) {
            ConfigurationSection pdcSection = config.getConfigurationSection(path + "pdc");
            if (pdcSection != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (String keyStr : pdcSection.getKeys(false)) {
                    String[] parts = keyStr.split(":");
                    NamespacedKey key = (parts.length > 1) ? new NamespacedKey(parts[0], parts[1]) : NamespacedKey.minecraft(parts[0]);
                    Object value = pdcSection.get(keyStr);
                    if (value instanceof String) pdc.set(key, PersistentDataType.STRING, (String) value);
                    else if (value instanceof Integer) pdc.set(key, PersistentDataType.INTEGER, (Integer) value);
                    else if (value instanceof Double) pdc.set(key, PersistentDataType.DOUBLE, (Double) value);
                    else if (value instanceof Byte) pdc.set(key, PersistentDataType.BYTE, (Byte) value);
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Trả về danh sách tên tất cả file type (không kèm .yml) trong ManagerItem
     * Dùng cho tab complete args[2] của "mi save/load"
     */
    public List<String> getTypeNames() {
        List<String> types = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return types;
        for (File f : files) {
            types.add(f.getName().replace(".yml", ""));
        }
        return types;
    }

    /**
     * Trả về danh sách ID thuộc về một type file cụ thể
     * Dùng cho tab complete args[3] của "mi save/load"
     */
    public List<String> getIdsByType(String type) {
        List<String> ids = new ArrayList<>();
        File file = new File(folder, type + ".yml");
        if (!file.exists()) return ids;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ids.addAll(config.getKeys(false));
        return ids;
    }

    /**
     * Trả về tất cả ID trong cache (dùng cho /mi load khi không cần lọc theo type)
     */
    public List<String> getAllIds() {
        return new ArrayList<>(itemCache.keySet());
    }
}