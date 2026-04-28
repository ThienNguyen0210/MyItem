package org.ThienNguyen.Listener;

import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.JewelryManager;
import org.ThienNguyen.Main;
import org.ThienNguyen.GemSocket.GemLogic;
import org.ThienNguyen.Stat.*;
import org.ThienNguyen.Hook.MMOCORE;
import org.ThienNguyen.Element.ElementCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CacheListener implements Listener {
    private static final Map<UUID, Long> LAST_COMBO_NOTIFY = new ConcurrentHashMap<>();
    private static final long NOTIFY_COOLDOWN_MS = 500L; // 500ms,
    private static final EquipmentSlot[] ALL_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND
    };

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        JewelryManager.loadDataToCache(p);
        refreshCache(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        PlayerCombatCache.invalidate(e.getPlayer().getUniqueId());
        JewelryManager.invalidateJewelryCache(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            delayRefresh(p);
        }
        if (e.getInventory().getHolder() instanceof JewelryManager jm) {
            jm.onClick(e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            delayRefresh(p);
        }
        if (e.getInventory().getHolder() instanceof JewelryManager jm) {
            jm.onClose(e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHold(PlayerItemHeldEvent e) {
        delayRefresh(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent e) {
        // Gọi delayRefresh để quét lại túi đồ và tay sau khi item đã rời khỏi người
        delayRefresh(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
        delayRefresh(e.getPlayer());
    }

    private void delayRefresh(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    refreshCache(player);
                }
            }
        }.runTaskLater(Main.getInstance(), 2L);
    }


    private static final ConcurrentHashMap<UUID, Long> LAST_COMBO_MESSAGE_TIME = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000L; // đúng 1 giây như bạn muốn :D

    public static void refreshCache(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerCombatCache.CombatStats stats = new PlayerCombatCache.CombatStats();

        FileConfiguration slotConfig   = Main.getInstance().getStatsSettingsConfig();
        FileConfiguration gemConfig    = Main.getInstance().getGemConfig();
        FileConfiguration eConfig      = Main.getInstance().getElementConfig();
        FileConfiguration comboConfig  = Main.getInstance().getComboConfig();

        // ────────────────────────────────────────────────
        // 1. Xử lý thông báo combo - CHỈ GỬI ĐÚNG 1 LẦN TRONG 1 GIÂY
        // ────────────────────────────────────────────────
// 1. Lấy ID bộ combo hiện tại (Hàm này phải trả về ID duy nhất nếu mặc đủ bộ)
        String currentComboId = org.ThienNguyen.Listener.ItemCombo.ComboListener.getFullSetComboId(player);
        if (currentComboId == null) currentComboId = ""; // Chuyển null thành chuỗi rỗng để dễ so sánh

// 2. Lấy ID đã lưu từ Metadata (ID cuối cùng mà người chơi đã được thông báo)
        String comboMetadataKey = "last_notified_combo";
        String lastNotifiedId = "";
        if (player.hasMetadata(comboMetadataKey)) {
            lastNotifiedId = player.getMetadata(comboMetadataKey).get(0).asString();
        }

// 3. CHỈ XỬ LÝ NẾU TRẠNG THÁI THAY ĐỔI (Tránh việc đổi tay qua lại mà ID vẫn thế)
        if (!currentComboId.equals(lastNotifiedId)) {

            // Cập nhật Metadata NGAY LẬP TỨC để chặn các Event chạy song song phía sau
            if (!currentComboId.isEmpty()) {
                player.setMetadata(comboMetadataKey, new FixedMetadataValue(Main.getInstance(), currentComboId));
            } else {
                player.removeMetadata(comboMetadataKey, Main.getInstance());
            }

            // Kiểm tra Cooldown thời gian để tránh spam khi mặc/tháo đồ quá nhanh
            long now = System.currentTimeMillis();
            UUID uuid = player.getUniqueId();
            long lastSent = LAST_COMBO_MESSAGE_TIME.getOrDefault(uuid, 0L);

            if (now - lastSent >= MESSAGE_COOLDOWN_MS) {
                if (!currentComboId.isEmpty()) {
                    String msg = comboConfig.getString(currentComboId + ".message");
                    if (msg != null && !msg.trim().isEmpty()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    }
                }
                // Cập nhật mốc thời gian gửi tin nhắn thành công
                LAST_COMBO_MESSAGE_TIME.put(uuid, now);
            }
        }

        // ────────────────────────────────────────────────
        // 2. Cộng stats combo (luôn cập nhật, không cooldown)
        // ────────────────────────────────────────────────
        if (currentComboId != null) {
            org.bukkit.configuration.ConfigurationSection comboStats = comboConfig.getConfigurationSection(currentComboId + ".stats");
            if (comboStats != null) {
                for (String key : comboStats.getKeys(false)) {
                    updateStat(stats, key, comboStats.getDouble(key));
                }
            }
        }

        // ────────────────────────────────────────────────
        // 3. Reset cache cũ
        // ────────────────────────────────────────────────
        stats.clearWeaponElements();
        stats.bestAbilities.clear();
        stats.totalArmor = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR).getValue();

        // ────────────────────────────────────────────────
        // 4. Trang bị (giữ nguyên)
        // ────────────────────────────────────────────────
        for (EquipmentSlot slot : ALL_SLOTS) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            Map<String, Integer> itemDefs = ElementCore.getItemDefenses(item);
            itemDefs.forEach((elem, lv) -> {
                stats.elementDefenses.merge(elem, lv, Integer::sum);
            });
            if (isMMOCoreAvailable() && !MMOCORE.canUse(player, item)) continue;
            if (isAllowed(item, slotConfig, "accuracy", slot)) {
                stats.totalAccuracy += Accuracy.get(item);
            }
            if (isAllowed(item, slotConfig, "damage", slot))          stats.totalBonusDmg     += Damage.getDamage(item);
            if (isAllowed(item, slotConfig, "pve_damage", slot))      stats.totalPveBonus     += PveDamage.get(item);
            if (isAllowed(item, slotConfig, "pvp_damage", slot))      stats.totalPvpBonus     += PvpDamage.get(item);
            if (isAllowed(item, slotConfig, "all_damage", slot))      stats.totalAllDamage    += AllDamage.get(item);
            if (isAllowed(item, slotConfig, "all_defense", slot))     stats.totalAllDefense   += AllDefense.get(item);
            if (isAllowed(item, slotConfig, "bow_damage", slot))      stats.totalBowDamage    += BowDamage.get(item);
            if (isAllowed(item, slotConfig, "critical_chance", slot)) stats.totalCritChance   += CriticalChance.get(item);
            if (isAllowed(item, slotConfig, "critical_damage", slot)) stats.totalCritDamage   += CriticalDamage.get(item);
            if (isAllowed(item, slotConfig, "lifesteal", slot))       stats.totalLifesteal    += Lifesteal.get(item);
            if (isAllowed(item, slotConfig, "true_damage", slot))     stats.totalTrueDamage   += TrueDamage.get(item);
            if (isAllowed(item, slotConfig, "penetration", slot))     stats.totalPenetration  += Penetration.get(item);
            if (isAllowed(item, slotConfig, "armor_pen", slot))       stats.totalArmorPen     += ArmorPen.get(item);
            if (isAllowed(item, slotConfig, "armor", slot))           stats.totalArmor        += Armor.getArmor(item);
            if (isAllowed(item, slotConfig, "pve_defense", slot))     stats.totalPveDef       += PveDefense.get(item);
            if (isAllowed(item, slotConfig, "pvp_defense", slot))     stats.totalPvpDef       += PvpDefense.get(item);
            if (isAllowed(item, slotConfig, "dodge_rate", slot))      stats.totalDodge        += DodgeRate.get(item);
            if (isAllowed(item, slotConfig, "block_rate", slot))      stats.totalBlock        += BlockRate.get(item);
            if (isAllowed(item, slotConfig, "thorns", slot))          stats.totalThorns       += Thorns.get(item);
            if (isAllowed(item, slotConfig, "knockback_resistance", slot)) stats.totalKnockbackResist += KnockbackResistance.get(item);
            if (isAllowed(item, slotConfig, "death_damage", slot))    stats.totalDeathDamage  += DeathDamage.get(item);
            if (isAllowed(item, slotConfig, "magic_damage", slot))    stats.totalMagicDamage  += MagicDamage.get(item);
            if (isAllowed(item, slotConfig, "magic_defense", slot))   stats.totalMagicDefense += MagicDefense.get(item);
            // Nguyên tố, ngọc, ability legacy... (giữ nguyên như cũ)
            Map<String, Integer> itemElements = ElementCore.getAllElements(item);
            for (Map.Entry<String, Integer> entry : itemElements.entrySet()) {
                String eId = entry.getKey().toUpperCase();
                if (eConfig.contains(eId)) {
                    double base = eConfig.getDouble(eId + ".base-damage", 0.0);
                    double per  = eConfig.getDouble(eId + ".damage-per", 0.0);
                    double totalDmg = base + (entry.getValue() * per);
                    stats.weaponElementDamage.merge(eId, totalDmg, Double::sum);
                    stats.weaponElementLevels.merge(eId, entry.getValue(), Integer::sum);
                }
            }

            List<String> gemIds = GemLogic.getGemsOnItem(item);
            for (String gemId : gemIds) {
                // stats, element, ability từ gem (giữ nguyên)
                if (gemConfig.contains(gemId + ".apply.stats")) {
                    for (String line : gemConfig.getStringList(gemId + ".apply.stats")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length < 2) continue;
                        try {
                            String type = parts[0].trim().toLowerCase();
                            double value = Double.parseDouble(parts[1].trim());
                            if (isAllowed(item, slotConfig, type, slot)) {
                                updateStat(stats, type, value);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (gemConfig.contains(gemId + ".apply.element")) {
                    for (String line : gemConfig.getStringList(gemId + ".apply.element")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            try {
                                String eId = parts[0].trim().toUpperCase();
                                double val = Double.parseDouble(parts[1].trim());
                                stats.weaponElementDamage.merge(eId, val, Double::sum);
                            } catch (Exception ignored) {}
                        }
                    }
                }
                if (gemConfig.contains(gemId + ".apply.ability")) {
                    for (String line : gemConfig.getStringList(gemId + ".apply.ability")) {
                        processAbilityLine(line, stats.bestAbilities);
                    }
                }
            }

            for (String data : AbilityData.getAbilityList(item)) {
                processAbilityLineLegacy(data, stats.bestAbilities);
            }
        }

        // ────────────────────────────────────────────────
        // 5. Trang sức (Jewelry) - giữ nguyên
        // ────────────────────────────────────────────────
        // ────────────────────────────────────────────────
        // 5. Trang sức (Jewelry)
        // ────────────────────────────────────────────────

        // NGUỒN 1: Lấy từ GUI ảo (Dữ liệu cũ trong Database/Cache)
        Map<Integer, ItemStack> jewelryItems = JewelryManager.getCachedJewelry(player.getUniqueId());
        for (ItemStack jItem : jewelryItems.values()) {
            applyJewelryStats(stats, jItem, eConfig, gemConfig);
        }

        // NGUỒN 2: Lấy trực tiếp từ Inventory người chơi (Nếu bật trong config)
        FileConfiguration mainConfig = Main.getInstance().getConfig();
        if (mainConfig.getBoolean("jewelry.enable-player-inventory", false)) {
            ConfigurationSection pSlots = mainConfig.getConfigurationSection("jewelry.player-slots");
            if (pSlots != null) {
                for (String key : pSlots.getKeys(false)) {
                    try {
                        int slotIdx = Integer.parseInt(key);
                        ItemStack item = player.getInventory().getItem(slotIdx);

                        // CHỈ CỘNG STATS NẾU: Không phải không khí & Không phải Placeholder
                        if (item != null && item.getType() != Material.AIR && !isPlaceholder(item)) {
                            applyJewelryStats(stats, item, eConfig, gemConfig);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // ────────────────────────────────────────────────
        // 6. Knockback Resistance
        // ────────────────────────────────────────────────
        double finalKB = Math.max(0.0, Math.min(1.0, stats.totalKnockbackResist));
        org.bukkit.attribute.AttributeInstance kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbAttr.setBaseValue(finalKB);
        }
        // ────────────────────────────────────────────────
        // 8. HOOK MYATTRIBUTE - CỘNG DỒN TỪ SQLITE
        // ────────────────────────────────────────────────
        try {
            // Chỉ chạy nếu plugin MyAttribute đang được bật trên server
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MyAttribute")) {
                UUID uuid = player.getUniqueId();

                // Nhóm Tấn Công
                stats.totalAccuracy += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "accuracy");
                stats.totalBonusDmg     += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "damage");
                stats.totalMagicDamage  += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "magic_damage");
                stats.totalCritChance   += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "crit_chance");
                stats.totalCritDamage   += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "crit_damage");
                stats.totalPenetration  += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "penetration");
                stats.totalLifesteal    += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "lifesteal");
                stats.totalTrueDamage   += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "true_damage");
                stats.totalPveBonus     += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "pve_damage");
                stats.totalPvpBonus     += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "pvp_damage");

                // Nhóm Phòng Thủ
                stats.totalArmor        += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "armor");
                stats.totalMagicDefense += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "magic_defense");
                stats.totalPveDef       += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "pve_defense");
                stats.totalPvpDef       += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "pvp_defense");
            }
        } catch (Throwable t) {
            // Tránh crash nếu class API không tồn tại lúc runtime
        }
        // ────────────────────────────────────────────────
        // 7. Update cache
        // ────────────────────────────────────────────────
        PlayerCombatCache.updateCache(player.getUniqueId(), stats);
    }

    // Hàm updateStat bạn đã cung cấp (đặt ở cùng class)
    private static void updateStat(PlayerCombatCache.CombatStats stats, String type, double val) {
        if (val == 0) return; // Tối ưu: không xử lý nếu giá trị bằng 0

        switch (type.toLowerCase()) {
            // TẤN CÔNG
            case "damage" -> stats.totalBonusDmg += val;
            case "pve_damage" -> stats.totalPveBonus += val;
            case "pvp_damage" -> stats.totalPvpBonus += val;
            case "all_damage" -> stats.totalAllDamage += val;
            case "magic_damage" -> stats.totalMagicDamage += val;
            case "bow_damage" -> stats.totalBowDamage += val;
            case "true_damage" -> stats.totalTrueDamage += val;
            case "death_damage" -> stats.totalDeathDamage += val;

            // CHỈ SỐ PHỤ TẤN CÔNG
            case "critical_chance", "crit_chance" -> stats.totalCritChance += val;
            case "critical_damage", "crit_damage" -> stats.totalCritDamage += val;
            case "penetration" -> stats.totalPenetration += val;
            case "armor_pen" -> stats.totalArmorPen += val;
            case "lifesteal" -> stats.totalLifesteal += val;
            case "accuracy" -> stats.totalAccuracy += val; // Thêm dòng này
            // PHÒNG THỦ
            case "armor" -> stats.totalArmor += val;
            case "pve_defense", "pve_def" -> stats.totalPveDef += val;
            case "pvp_defense", "pvp_def" -> stats.totalPvpDef += val;
            case "all_defense" -> stats.totalAllDefense += val;
            case "magic_defense", "magic_def" -> stats.totalMagicDefense += val;
            case "dodge_rate" -> stats.totalDodge += val;
            case "block_rate" -> stats.totalBlock += val;
            case "thorns" -> stats.totalThorns += val;
            case "knockback_resistance" -> stats.totalKnockbackResist += val;

            // LƯU Ý: Health thường được xử lý qua Attribute gốc của Minecraft
            // nên bạn không cộng vào 'stats' mà cộng trực tiếp vào Player trong refreshCache
        }
    }

    private static void processAbilityLine(String line, Map<String, double[]> best) {
        String[] p = line.split(":");
        if (p.length < 2) return;
        String name = p[0].trim().toUpperCase();
        String[] vals = p[1].trim().split("\\s+");
        if (vals.length < 2) return;
        try {
            double lv = Double.parseDouble(vals[0]);
            double ch = Double.parseDouble(vals[1]);
            if (!best.containsKey(name) || lv > best.get(name)[0]) {
                best.put(name, new double[]{lv, ch});
            }
        } catch (Exception ignored) {}
    }
    private static void applyJewelryStats(PlayerCombatCache.CombatStats stats, ItemStack jItem, FileConfiguration eConfig, FileConfiguration gemConfig) {
        if (jItem == null || jItem.getType() == Material.AIR) return;
        stats.totalAccuracy += Accuracy.get(jItem);
        // Cộng các chỉ số cơ bản
        stats.totalBonusDmg     += Damage.getDamage(jItem);
        stats.totalPveBonus     += PveDamage.get(jItem);
        stats.totalPvpBonus     += PvpDamage.get(jItem);
        stats.totalCritChance   += CriticalChance.get(jItem);
        stats.totalCritDamage   += CriticalDamage.get(jItem);
        stats.totalLifesteal    += Lifesteal.get(jItem);
        stats.totalTrueDamage   += TrueDamage.get(jItem);
        stats.totalPenetration  += Penetration.get(jItem);
        stats.totalArmorPen     += ArmorPen.get(jItem);
        stats.totalArmor        += Armor.getArmor(jItem);
        stats.totalPveDef       += PveDefense.get(jItem);
        stats.totalPvpDef       += PvpDefense.get(jItem);
        stats.totalDodge        += DodgeRate.get(jItem);
        stats.totalBlock        += BlockRate.get(jItem);
        stats.totalThorns       += Thorns.get(jItem);
        stats.totalAllDamage    += AllDamage.get(jItem);
        stats.totalAllDefense   += AllDefense.get(jItem);
        stats.totalBowDamage    += BowDamage.get(jItem);
        stats.totalDeathDamage  += DeathDamage.get(jItem);
        stats.totalMagicDamage  += MagicDamage.get(jItem);
        stats.totalMagicDefense += MagicDefense.get(jItem);

        // Xử lý nguyên tố (Elements)
        Map<String, Integer> jElements = ElementCore.getAllElements(jItem);
        for (Map.Entry<String, Integer> entry : jElements.entrySet()) {
            String eId = entry.getKey().toUpperCase();
            if (eConfig.contains(eId)) {
                double base = eConfig.getDouble(eId + ".base-damage", 0.0);
                double per  = eConfig.getDouble(eId + ".damage-per", 0.0);
                double totalDmg = base + (entry.getValue() * per);
                stats.weaponElementDamage.merge(eId, totalDmg, Double::sum);
                stats.weaponElementLevels.merge(eId, entry.getValue(), Integer::sum);
            }
        }

        // Xử lý ngọc (Gems) đính trên trang sức
        List<String> jGemIds = GemLogic.getGemsOnItem(jItem);
        for (String gemId : jGemIds) {
            if (gemConfig.contains(gemId + ".apply.stats")) {
                for (String line : gemConfig.getStringList(gemId + ".apply.stats")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length < 2) continue;
                    try {
                        updateStat(stats, parts[0].trim().toLowerCase(), Double.parseDouble(parts[1].trim()));
                    } catch (Exception ignored) {}
                }
            }
            if (gemConfig.contains(gemId + ".apply.ability")) {
                for (String line : gemConfig.getStringList(gemId + ".apply.ability")) {
                    processAbilityLine(line, stats.bestAbilities);
                }
            }
        }
    }
    private static boolean isPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        // Kiểm tra xem item có chứa NBT 'is_placeholder' mà chúng ta đã đặt không
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(Main.getInstance(), "is_placeholder"), org.bukkit.persistence.PersistentDataType.BOOLEAN);
    }
    private static void processAbilityLineLegacy(String data, Map<String, double[]> best) {
        String[] parts = data.split(":");
        if (parts.length < 3) return;
        try {
            String name = parts[0].toUpperCase();
            double lv = Double.parseDouble(parts[1]);
            double ch = Double.parseDouble(parts[2]);
            if (!best.containsKey(name) || lv > best.get(name)[0]) {
                best.put(name, new double[]{lv, ch});
            }
        } catch (Exception ignored) {}
    }

    private static boolean isAllowed(ItemStack item, FileConfiguration config, String stat, EquipmentSlot currentSlot) {
        if (item == null || !item.hasItemMeta()) return true;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), "slot_" + stat.toLowerCase());
        String requiredSlot = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (requiredSlot != null) {
            if (requiredSlot.equalsIgnoreCase("any")) return true;
            String current = currentSlot.name().toLowerCase().replace("_", "");
            if (current.equals("hand")) current = "mainhand";

            String[] allowedSlots = requiredSlot.split(",");
            for (String s : allowedSlots) {
                if (s.trim().equalsIgnoreCase(current)) return true;
            }
            return false;
        }

        if (config == null) return true;
        List<String> list = config.getStringList("stats-slots." + stat);
        return list == null || list.isEmpty() || list.contains(currentSlot.name());
    }

    private static boolean isMMOCoreAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("MMOCore");
    }
}