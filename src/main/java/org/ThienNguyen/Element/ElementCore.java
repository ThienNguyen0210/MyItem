package org.ThienNguyen.Element;

import org.ThienNguyen.Main;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class ElementCore {
    
    private static final String ATK_PREFIX = "elem_";
    private static final String DEF_PREFIX = "elem_def_";

    
    private static final NamespacedKey ELEMENT_KEY = new NamespacedKey(Main.getInstance(), "item_element");
    private static final NamespacedKey LEVEL_KEY = new NamespacedKey(Main.getInstance(), "item_element_level");

    /**
     * HÀM QUAN TRỌNG: Lấy level an toàn tránh lỗi ép kiểu Double/Integer từ PersistentData
     */
    public static int getSafeInt(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            return pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
        }
        if (pdc.has(key, PersistentDataType.DOUBLE)) {
            Double val = pdc.get(key, PersistentDataType.DOUBLE);
            return val != null ? val.intValue() : 0;
        }
        return 0;
    }

    /**
     * Lấy toàn bộ nguyên tố TẤN CÔNG (Sửa lỗi cannot find symbol getAllElements)
     */
    public static Map<String, Integer> getAllElements(ItemStack item) {
        Map<String, Integer> elements = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return elements;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            String k = key.getKey();
            
            if (k.startsWith(ATK_PREFIX) && !k.startsWith(DEF_PREFIX)) {
                String elementId = k.replace(ATK_PREFIX, "").toUpperCase();
                int level = getSafeInt(pdc, key);
                if (level > 0) elements.put(elementId, level);
            }
        }

        
        String oldId = pdc.get(ELEMENT_KEY, PersistentDataType.STRING);
        int oldLv = getSafeInt(pdc, LEVEL_KEY);
        if (oldId != null && oldLv > 0) {
            elements.merge(oldId.toUpperCase(), oldLv, Integer::max);
        }

        return elements;
    }

    /**
     * Lấy toàn bộ chỉ số PHÒNG THỦ nguyên tố
     */
    public static Map<String, Integer> getAllDefenses(ItemStack item) {
        Map<String, Integer> defenses = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return defenses;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            String k = key.getKey();
            if (k.startsWith(DEF_PREFIX)) {
                String elementId = k.replace(DEF_PREFIX, "").toUpperCase();
                int level = getSafeInt(pdc, key);
                if (level > 0) defenses.put(elementId, level);
            }
        }
        return defenses;
    }

    /**
     * Thêm/Cập nhật nguyên tố TẤN CÔNG
     */
    public static void addElement(ItemStack item, String elementId, int level) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), ATK_PREFIX + elementId.toLowerCase());
        int currentLevel = getSafeInt(meta.getPersistentDataContainer(), key);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, currentLevel + level);
        item.setItemMeta(meta);
    }

    /**
     * Thêm/Cập nhật nguyên tố PHÒNG THỦ
     */
    public static void addDefenseElement(ItemStack item, String elementId, int level) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), DEF_PREFIX + elementId.toLowerCase());
        int currentLevel = getSafeInt(meta.getPersistentDataContainer(), key);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, currentLevel + level);
        item.setItemMeta(meta);
    }

    /**
     * Hiệu ứng Particle khi gây sát thương nguyên tố
     */
    public static void playEffect(Entity target, String elementId) {
        FileConfiguration config = Main.getInstance().getElementConfig();
        if (config == null || !config.contains(elementId)) return;

        String typeStr = config.getString(elementId + ".particle", "CRIT");
        int density = config.getInt(elementId + ".density", 5);
        Location loc = target.getLocation().add(0, 1, 0);

        try {
            Particle particle = Particle.valueOf(typeStr.toUpperCase());
            World world = loc.getWorld();
            if (world == null) return;

            for (int i = 0; i < density; i++) {
                double offsetX = (Math.random() - 0.5) * 0.6;
                double offsetY = (Math.random() - 0.5) * 0.6;
                double offsetZ = (Math.random() - 0.5) * 0.6;
                Location spawnLoc = loc.clone().add(offsetX, offsetY, offsetZ);

                if (particle == Particle.REDSTONE) {
                    String colorStr = config.getString(elementId + ".color", "255,0,0");
                    String[] rgb = colorStr.split(",");
                    int r = Integer.parseInt(rgb[0].trim());
                    int g = Integer.parseInt(rgb[1].trim());
                    int b = Integer.parseInt(rgb[2].trim());
                    Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(r, g, b), 1.0F);
                    world.spawnParticle(Particle.REDSTONE, spawnLoc, 1, dust);
                } else if (particle == Particle.BLOCK_DUST || particle == Particle.BLOCK_CRACK || particle == Particle.BLOCK_MARKER) {
                    String matStr = config.getString(elementId + ".material", "STONE");
                    Material mat = Material.matchMaterial(matStr);
                    if (mat == null) mat = Material.STONE;
                    world.spawnParticle(particle, spawnLoc, 1, mat.createBlockData());
                } else {
                    world.spawnParticle(particle, spawnLoc, 1, 0, 0, 0, 0.05);
                }
            }
        } catch (Exception ignored) {}
    }

    

    public static void setElement(ItemStack item, String elementId, int level) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(ELEMENT_KEY, PersistentDataType.STRING, elementId.toUpperCase());
        meta.getPersistentDataContainer().set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
    }

    public static String getElement(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ELEMENT_KEY, PersistentDataType.STRING);
    }

    public static int getLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return getSafeInt(item.getItemMeta().getPersistentDataContainer(), LEVEL_KEY);
    }

    public static Map<String, Integer> getItemDefenses(ItemStack item) {
        return getAllDefenses(item); 
    }
}