package org.ThienNguyen.Listener;

import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierSource;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.ThienNguyen.Main;
import org.ThienNguyen.Stat.*;
import org.ThienNguyen.Hook.MMOCORE;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class StatsListener implements Listener {
    private static StatsListener instance;
    public StatsListener() { instance = this; }
    public static StatsListener getInstance() { return instance; }
    private final UUID WINDY_HEALTH_UUID = UUID.fromString("73223631-6164-4231-6232-313436353335");
    private final UUID WINDY_ARMOR_UUID = UUID.fromString("84334742-7275-5342-7343-424547644446");
    private final UUID WINDY_ATTACK_SPEED_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private final UUID WINDY_MOVEMENT_SPEED_UUID = UUID.fromString("66666666-7777-8888-9999-000000000000");
    private final NamespacedKey PARTICLE_KEY = new NamespacedKey(Main.getInstance(), "item_particle");
    private final String MODIFIER_KEY = "windy_custom_stats";
    double totalHealthRegenPercent = Main.getInstance().getConfig().getDouble("regeneration.percent-per-second", 0.0);
    private final org.bukkit.inventory.EquipmentSlot[] VALID_PLAYER_SLOTS = {
            org.bukkit.inventory.EquipmentSlot.HEAD,
            org.bukkit.inventory.EquipmentSlot.CHEST,
            org.bukkit.inventory.EquipmentSlot.LEGS,
            org.bukkit.inventory.EquipmentSlot.FEET,
            org.bukkit.inventory.EquipmentSlot.HAND,
            org.bukkit.inventory.EquipmentSlot.OFF_HAND
    };

    public void updateGemBuffsOnly(Player player) {
        FileConfiguration gemConfig = Main.getInstance().getGemConfig();

        for (org.bukkit.inventory.EquipmentSlot slot : VALID_PLAYER_SLOTS) {
            try {
                ItemStack item = player.getInventory().getItem(slot);
                if (item == null || item.getType().isAir()) continue;

                if (!org.ThienNguyen.Hook.MMOCORE.canUse(player, item)) continue;

                for (String gemId : org.ThienNguyen.GemSocket.GemLogic.getGemsOnItem(item)) {
                    if (gemConfig.contains(gemId + ".apply.BUFF")) {
                        for (String buffLine : gemConfig.getStringList(gemId + ".apply.BUFF")) {
                            String[] parts = buffLine.split(":", 2);
                            if (parts.length < 2) continue;

                            PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase());
                            if (type != null) {
                                try {
                                    int level = Integer.parseInt(parts[1].trim()) - 1;
                                    player.addPotionEffect(new PotionEffect(type, 80, Math.max(0, level), true, false, false));
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!player.isOnline()) return;

            updatePlayerStats(player);

            AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hpAttr != null) {
                player.setHealth(hpAttr.getValue());
            }
        }, 40L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!player.isOnline()) return;

            updatePlayerStats(player);

            AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hpAttr != null) {
                player.setHealth(hpAttr.getValue());
            }
        }, 2L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            updatePlayerStats(player);
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> updatePlayerStats(event.getPlayer()));
    }

    public void updatePlayerStats(Player player) {
        if (player == null || !player.isOnline()) return;

        double totalHealth = 0;
        double totalMaxMana = 0;
        double totalManaRegen = 0;
        double totalAttackSpeed = 0;
        double totalMovementSpeed = 0;
        double totalHealthRegen = 0;

        double pctHealth = 0.0;
        double pctMaxMana = 0.0;
        double pctManaRegen = 0.0;
        double pctAttackSpeed = 0.0;
        double pctMovementSpeed = 0.0;
        double pctHealthRegen = 0.0;

        String foundParticleId = null;
        FileConfiguration config = Main.getInstance().getStatsSettingsConfig();
        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
        FileConfiguration mainConfig = Main.getInstance().getConfig();
        UUID uuid = player.getUniqueId();

        // --- PHẦN 1: TRANG BỊ CHÍNH (ARMOR & HANDS) --- (Giữ nguyên)
        for (org.bukkit.inventory.EquipmentSlot slot : VALID_PLAYER_SLOTS) {
            try {
                ItemStack item = player.getInventory().getItem(slot);
                if (item == null || item.getType().isAir()) continue;
                if (!MMOCORE.canUse(player, item)) continue;

                if (foundParticleId == null && item.hasItemMeta()) {
                    String pId = item.getItemMeta().getPersistentDataContainer().get(PARTICLE_KEY, PersistentDataType.STRING);
                    if (pId != null) foundParticleId = pId;
                }

                if (isSlotAllowed(item, config, "health", slot)) totalHealth += Health.getHealth(item);
                if (isSlotAllowed(item, config, "max_mana", slot)) totalMaxMana += MaxMana.get(item);
                if (isSlotAllowed(item, config, "mana_regen", slot)) totalManaRegen += ManaRegen.get(item);
                if (isSlotAllowed(item, config, "attack_speed", slot)) totalAttackSpeed += AttackSpeed.get(item);
                if (isSlotAllowed(item, config, "movement_speed", slot)) totalMovementSpeed += MovementSpeed.get(item);
                if (isSlotAllowed(item, config, "health_regen", slot)) totalHealthRegen += HealthRegen.get(item);

                if (item.hasItemMeta()) {
                    var pdc = item.getItemMeta().getPersistentDataContainer();
                    if (isAllowedPercent(item, config, "health", slot)) pctHealth += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_health"), PersistentDataType.DOUBLE, 0.0);
                    if (isAllowedPercent(item, config, "max_mana", slot)) pctMaxMana += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_max_mana"), PersistentDataType.DOUBLE, 0.0);
                    if (isAllowedPercent(item, config, "mana_regen", slot)) pctManaRegen += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_mana_regen"), PersistentDataType.DOUBLE, 0.0);
                    if (isAllowedPercent(item, config, "attack_speed", slot)) pctAttackSpeed += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_attack_speed"), PersistentDataType.DOUBLE, 0.0);
                    if (isAllowedPercent(item, config, "movement_speed", slot)) pctMovementSpeed += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_movement_speed"), PersistentDataType.DOUBLE, 0.0);
                    if (isAllowedPercent(item, config, "health_regen", slot)) pctHealthRegen += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_health_regen"), PersistentDataType.DOUBLE, 0.0);
                }

                for (String gemId : org.ThienNguyen.GemSocket.GemLogic.getGemsOnItem(item)) {
                    if (gemConfig.contains(gemId + ".apply.stats")) {
                        for (String line : gemConfig.getStringList(gemId + ".apply.stats")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length < 2) continue;
                            String type = parts[0].trim().toLowerCase();
                            try {
                                double value = Double.parseDouble(parts[1].trim());
                                if (isSlotAllowed(item, config, type, slot)) {
                                    switch (type) {
                                        case "health" -> totalHealth += value;
                                        case "max_mana" -> totalMaxMana += value;
                                        case "mana_regen" -> totalManaRegen += value;
                                        case "attack_speed" -> totalAttackSpeed += value;
                                        case "movement_speed" -> totalMovementSpeed += value;
                                        case "health_regen" -> totalHealthRegen += value;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // --- PHẦN 2: TRANG SỨC (GUI + TÚI ĐỒ) - ĐÃ FIX CHO CONFIG LIST ---
        Map<Integer, ItemStack> guiJewelryMap = org.ThienNguyen.JewelryManager.getCachedJewelry(uuid);

        // Lấy tất cả slot trang sức từ config
        Set<Integer> jewelrySlotIndices = getJewelrySlots();

        for (int slotIdx : jewelrySlotIndices) {
            try {
                ItemStack itemInSlot = player.getInventory().getItem(slotIdx);
                ItemStack effectiveItem = null;

                // Ưu tiên item thật trong inventory
                if (itemInSlot != null && !itemInSlot.getType().isAir() && !isPlaceholder(itemInSlot)) {
                    effectiveItem = itemInSlot;
                }
                // Nếu không có thì lấy từ cache
                else if (guiJewelryMap != null && guiJewelryMap.containsKey(slotIdx)) {
                    effectiveItem = guiJewelryMap.get(slotIdx);
                }

                if (effectiveItem != null && !effectiveItem.getType().isAir() && isJewelryMatch(effectiveItem, slotIdx)) {
                    if (!MMOCORE.canUse(player, effectiveItem)) continue;

                    // === CỘNG STATS ===
                    totalHealth += Health.getHealth(effectiveItem);
                    totalMaxMana += MaxMana.get(effectiveItem);
                    totalManaRegen += ManaRegen.get(effectiveItem);
                    totalAttackSpeed += AttackSpeed.get(effectiveItem);
                    totalMovementSpeed += MovementSpeed.get(effectiveItem);
                    totalHealthRegen += HealthRegen.get(effectiveItem);

                    // Percent stats
                    if (effectiveItem.hasItemMeta()) {
                        var pdc = effectiveItem.getItemMeta().getPersistentDataContainer();
                        pctHealth += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_health"), PersistentDataType.DOUBLE, 0.0);
                        pctMaxMana += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_max_mana"), PersistentDataType.DOUBLE, 0.0);
                        pctManaRegen += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_mana_regen"), PersistentDataType.DOUBLE, 0.0);
                        pctAttackSpeed += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_attack_speed"), PersistentDataType.DOUBLE, 0.0);
                        pctMovementSpeed += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_movement_speed"), PersistentDataType.DOUBLE, 0.0);
                        pctHealthRegen += pdc.getOrDefault(new NamespacedKey(Main.getInstance(), "pct_health_regen"), PersistentDataType.DOUBLE, 0.0);
                    }

                    // Gems
                    for (String gemId : org.ThienNguyen.GemSocket.GemLogic.getGemsOnItem(effectiveItem)) {
                        if (gemConfig.contains(gemId + ".apply.stats")) {
                            for (String line : gemConfig.getStringList(gemId + ".apply.stats")) {
                                String[] parts = line.split(":", 2);
                                if (parts.length < 2) continue;
                                String type = parts[0].trim().toLowerCase();
                                try {
                                    double val = Double.parseDouble(parts[1].trim());
                                    switch (type) {
                                        case "health" -> totalHealth += val;
                                        case "max_mana" -> totalMaxMana += val;
                                        case "mana_regen" -> totalManaRegen += val;
                                        case "attack_speed" -> totalAttackSpeed += val;
                                        case "movement_speed" -> totalMovementSpeed += val;
                                        case "health_regen" -> totalHealthRegen += val;
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // --- PHẦN 3 & 4 giữ nguyên ---
        String comboId = org.ThienNguyen.Listener.ItemCombo.ComboListener.getFullSetComboId(player);
        if (comboId != null) {
            ConfigurationSection comboStats = Main.getInstance().getComboConfig().getConfigurationSection(comboId + ".stats");
            if (comboStats != null) {
                for (String key : comboStats.getKeys(false)) {
                    double val = comboStats.getDouble(key);
                    String k = key.toLowerCase();
                    if (k.equals("health")) totalHealth += val;
                    else if (k.equals("max_mana")) totalMaxMana += val;
                    else if (k.equals("mana_regen")) totalManaRegen += val;
                    else if (k.equals("attack_speed")) totalAttackSpeed += val;
                    else if (k.equals("movement_speed")) totalMovementSpeed += val;
                    else if (k.equals("health_regen")) totalHealthRegen += val;
                }
            }
        }

        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MyAttribute")) {
            totalHealth += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "health");
            totalAttackSpeed += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "attack_speed");
            totalMovementSpeed += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "movement_speed");
            totalMaxMana += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "max_mana");
            totalManaRegen += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "mana_regen");
            totalHealthRegen += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "health_regen");
        }

        // --- PHẦN 4: APPLY ---
        if (pctHealth != 0.0) totalHealth *= (1.0 + (pctHealth / 100.0));
        if (pctMaxMana != 0.0) totalMaxMana *= (1.0 + (pctMaxMana / 100.0));
        if (pctManaRegen != 0.0) totalManaRegen *= (1.0 + (pctManaRegen / 100.0));
        if (pctAttackSpeed != 0.0) totalAttackSpeed *= (1.0 + (pctAttackSpeed / 100.0));
        if (pctMovementSpeed != 0.0) totalMovementSpeed *= (1.0 + (pctMovementSpeed / 100.0));
        if (pctHealthRegen != 0.0) totalHealthRegen *= (1.0 + (pctHealthRegen / 100.0));

        if (foundParticleId != null) org.ThienNguyen.Listener.Particle.ParticleManager.setEffect(player, foundParticleId);
        else org.ThienNguyen.Listener.Particle.ParticleManager.removeEffect(player);

        applyVanillaAttribute(player, Attribute.GENERIC_MAX_HEALTH, WINDY_HEALTH_UUID, totalHealth);
        applyVanillaAttribute(player, Attribute.GENERIC_ATTACK_SPEED, WINDY_ATTACK_SPEED_UUID, (4.0 * totalAttackSpeed) / 100.0);
        applyVanillaAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, WINDY_MOVEMENT_SPEED_UUID, (0.1 * totalMovementSpeed) / 100.0);

        player.setMetadata("windy_health_regen", new FixedMetadataValue(Main.getInstance(), totalHealthRegen));
        player.setMetadata("windy_health_regen_percent", new FixedMetadataValue(Main.getInstance(), pctHealthRegen));

        ManaManager.applyMana(player, totalMaxMana, totalManaRegen);
    }

    // NHỚ THÊM HÀM NÀY VÀO CUỐI CLASS STATSLISTENER
    private boolean isPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(Main.getInstance(), "is_placeholder"), PersistentDataType.BOOLEAN);
    }

    private boolean isSlotAllowed(ItemStack item, FileConfiguration config, String stat, org.bukkit.inventory.EquipmentSlot currentSlot) {
        if (item == null || !item.hasItemMeta()) return true;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), "slot_" + stat.toLowerCase());
        String requiredSlot = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (requiredSlot != null) {
            String current = currentSlot.name().toLowerCase().replace("_", "");
            if (current.equals("hand")) current = "mainhand";

            if (requiredSlot.equalsIgnoreCase("any")) return true;

            String[] allowedParts = requiredSlot.split(",");
            for (String part : allowedParts) {
                if (part.trim().equalsIgnoreCase(current)) {
                    return true;
                }
            }
            return false;
        }

        if (config == null) return true;
        List<String> allowedSlots = config.getStringList("stats-slots." + stat);

        if (allowedSlots == null || allowedSlots.isEmpty()) return true;
        return allowedSlots.contains(currentSlot.name());
    }


    private boolean isAllowedPercent(ItemStack item, FileConfiguration config, String stat, org.bukkit.inventory.EquipmentSlot currentSlot) {
        if (item == null || !item.hasItemMeta()) return true;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), "slot_pct_" + stat.toLowerCase());
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

    private void applyVanillaAttribute(Player player, Attribute attr, UUID uuid, double value) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null) return;

        instance.getModifiers().stream()
                .filter(mod -> mod.getUniqueId().equals(uuid))
                .forEach(instance::removeModifier);

        if (value != 0) {
            AttributeModifier mod = new AttributeModifier(uuid, MODIFIER_KEY, value, AttributeModifier.Operation.ADD_NUMBER);
            instance.addModifier(mod);
        }

        if (attr == Attribute.GENERIC_MAX_HEALTH && player.getHealth() > instance.getValue()) {
            player.setHealth(instance.getValue());
        }
    }




    private boolean isJewelryMatch(ItemStack item, int slotIdx) {
        if (item == null || item.getType().isAir()) return false;

        FileConfiguration config = Main.getInstance().getConfig();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        // Lấy type từ item (ví dụ: "nhan")
        String itemType = meta.getPersistentDataContainer().get(
                new NamespacedKey(Main.getInstance(), "jewelry_type"), PersistentDataType.STRING);
        if (itemType == null) return false;

        // 1. Kiểm tra trong player-slots (nếu dùng)
        String pSlotType = config.getString("jewelry.player-slots." + slotIdx + ".type");
        if (pSlotType != null && pSlotType.equalsIgnoreCase(itemType)) return true;

        // 2. Kiểm tra trong jewelry.slots (hỗ trợ cả List và Object đơn)
        ConfigurationSection groups = config.getConfigurationSection("jewelry.slots");
        if (groups != null && groups.contains(itemType)) {

            if (groups.isList(itemType)) {
                // === FIX CHÍNH Ở ĐÂY ===
                List<?> slotList = groups.getList(itemType);
                for (Object obj : slotList) {
                    if (obj instanceof Map<?, ?> map) {
                        Object slotObj = map.get("slot");
                        if (slotObj instanceof Number num && num.intValue() == slotIdx) {
                            return true;
                        }
                    }
                }
            } else {
                // Cấu hình cũ (object đơn)
                if (groups.getInt(itemType + ".slot") == slotIdx) return true;
            }
        }

        return false;
    }

    private java.util.Set<Integer> getJewelrySlots() {
        java.util.Set<Integer> slots = new java.util.HashSet<>();
        FileConfiguration config = Main.getInstance().getConfig();

        // Từ player-slots
        ConfigurationSection pSlots = config.getConfigurationSection("jewelry.player-slots");
        if (pSlots != null) {
            for (String key : pSlots.getKeys(false)) {
                try {
                    slots.add(Integer.parseInt(key));
                } catch (Exception ignored) {}
            }
        }

        // Từ jewelry.slots (hỗ trợ List)
        ConfigurationSection groups = config.getConfigurationSection("jewelry.slots");
        if (groups != null) {
            for (String type : groups.getKeys(false)) {
                if (groups.isList(type)) {
                    List<?> list = groups.getList(type);
                    if (list != null) {
                        for (Object obj : list) {
                            if (obj instanceof Map<?, ?> map) {
                                Object s = map.get("slot");
                                if (s instanceof Number num) {
                                    slots.add(num.intValue());
                                }
                            }
                        }
                    }
                } else {
                    // Cấu hình cũ
                    int s = groups.getInt(type + ".slot");
                    if (s > 0) slots.add(s);
                }
            }
        }
        return slots;
    }
}