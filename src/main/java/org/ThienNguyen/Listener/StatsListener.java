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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

        // Delay 15 tick để chắc chắn MyAttribute đã load xong dữ liệu từ SQLite
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!player.isOnline()) return;

            // Cập nhật toàn bộ stats (đã bao gồm hook API MyAttribute bên trong)
            updatePlayerStats(player);

            // Ép máu hiện tại bằng máu tối đa mới sau khi đã cộng dồn
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

            // Hồi đầy máu sau khi hồi sinh với chỉ số mới
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

        // --- KHỞI TẠO BIẾN ---
        double totalHealth = 0;
        double totalArmor = 0;
        double totalMaxMana = 0;
        double totalManaRegen = 0;
        double totalAttackSpeed = 0;
        double totalMovementSpeed = 0;
        double totalArmorPen = 0;
        double totalHealthRegen = 0;
        double totalAccuracy = 0; // Thêm biến Chính xác

        String foundParticleId = null;
        FileConfiguration config = Main.getInstance().getStatsSettingsConfig();
        FileConfiguration gemConfig = Main.getInstance().getGemConfig();

        // 1. Quét TRANG BỊ MẶC ĐỊNH (6 ô giáp và tay)
        for (org.bukkit.inventory.EquipmentSlot slot : VALID_PLAYER_SLOTS) {
            try {
                ItemStack item = player.getInventory().getItem(slot);
                if (item == null || item.getType().isAir()) continue;
                if (!MMOCORE.canUse(player, item)) continue;

                if (foundParticleId == null && item.hasItemMeta()) {
                    String pId = item.getItemMeta().getPersistentDataContainer().get(PARTICLE_KEY, PersistentDataType.STRING);
                    if (pId != null) foundParticleId = pId;
                }

                // Cộng stats gốc
                if (isSlotAllowed(item, config, "health", slot)) totalHealth += Health.getHealth(item);
                if (isSlotAllowed(item, config, "armor", slot)) totalArmor += Armor.getArmor(item);
                if (isSlotAllowed(item, config, "max_mana", slot)) totalMaxMana += MaxMana.get(item);
                if (isSlotAllowed(item, config, "mana_regen", slot)) totalManaRegen += ManaRegen.get(item);
                if (isSlotAllowed(item, config, "attack_speed", slot)) totalAttackSpeed += AttackSpeed.get(item);
                if (isSlotAllowed(item, config, "movement_speed", slot)) totalMovementSpeed += MovementSpeed.get(item);
                if (isSlotAllowed(item, config, "armor_pen", slot)) totalArmorPen += ArmorPen.get(item);
                if (isSlotAllowed(item, config, "health_regen", slot)) totalHealthRegen += HealthRegen.get(item);
                // totalAccuracy += org.ThienNguyen.Stat.Accuracy.get(item); // Nếu bạn đã tạo class Stat cho Accuracy

                // Quét Ngọc trên item
                for (String gemId : org.ThienNguyen.GemSocket.GemLogic.getGemsOnItem(item)) {
                    if (gemConfig.contains(gemId + ".apply.stats")) {
                        for (String line : gemConfig.getStringList(gemId + ".apply.stats")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length < 2) continue;
                            String type = parts[0].trim().toLowerCase();
                            double value = Double.parseDouble(parts[1].trim());
                            if (isSlotAllowed(item, config, type, slot)) {
                                switch (type) {
                                    case "health" -> totalHealth += value;
                                    case "armor" -> totalArmor += value;
                                    case "max_mana" -> totalMaxMana += value;
                                    case "mana_regen" -> totalManaRegen += value;
                                    case "attack_speed" -> totalAttackSpeed += value;
                                    case "movement_speed" -> totalMovementSpeed += value;
                                    case "armor_pen" -> totalArmorPen += value;
                                    case "health_regen" -> totalHealthRegen += value;
                                    case "accuracy" -> totalAccuracy += value;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Quét TRANG SỨC (Các ô đặc biệt 9, 10, 11...)
        for (int slotIdx : getJewelrySlots()) {
            try {
                ItemStack item = player.getInventory().getItem(slotIdx);
                if (isJewelryMatch(item, slotIdx)) {
                    if (!MMOCORE.canUse(player, item)) continue;

                    totalHealth += Health.getHealth(item);
                    totalArmor += Armor.getArmor(item);
                    totalMaxMana += MaxMana.get(item);
                    totalManaRegen += ManaRegen.get(item);
                    totalAttackSpeed += AttackSpeed.get(item);
                    totalMovementSpeed += MovementSpeed.get(item);
                    totalArmorPen += ArmorPen.get(item);
                    totalHealthRegen += HealthRegen.get(item);
                    // totalAccuracy += org.ThienNguyen.Stat.Accuracy.get(item);

                    for (String gemId : org.ThienNguyen.GemSocket.GemLogic.getGemsOnItem(item)) {
                        if (gemConfig.contains(gemId + ".apply.stats")) {
                            for (String line : gemConfig.getStringList(gemId + ".apply.stats")) {
                                String[] parts = line.split(":", 2);
                                if (parts.length < 2) continue;
                                String type = parts[0].trim().toLowerCase();
                                double value = Double.parseDouble(parts[1].trim());
                                switch (type) {
                                    case "health" -> totalHealth += value;
                                    case "armor" -> totalArmor += value;
                                    case "max_mana" -> totalMaxMana += value;
                                    case "mana_regen" -> totalManaRegen += value;
                                    case "attack_speed" -> totalAttackSpeed += value;
                                    case "movement_speed" -> totalMovementSpeed += value;
                                    case "armor_pen" -> totalArmorPen += value;
                                    case "health_regen" -> totalHealthRegen += value;
                                    case "accuracy" -> totalAccuracy += value;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Xử lý COMBO
        String comboId = org.ThienNguyen.Listener.ItemCombo.ComboListener.getFullSetComboId(player);
        if (comboId != null) {
            FileConfiguration comboConfig = Main.getInstance().getComboConfig();
            ConfigurationSection comboStats = comboConfig.getConfigurationSection(comboId + ".stats");
            if (comboStats != null) {
                for (String key : comboStats.getKeys(false)) {
                    double value = comboStats.getDouble(key);
                    switch (key.toLowerCase()) {
                        case "health" -> totalHealth += value;
                        case "armor" -> totalArmor += value;
                        case "max_mana" -> totalMaxMana += value;
                        case "mana_regen" -> totalManaRegen += value;
                        case "attack_speed" -> totalAttackSpeed += value;
                        case "movement_speed" -> totalMovementSpeed += value;
                        case "armor_pen" -> totalArmorPen += value;
                        case "health_regen" -> totalHealthRegen += value;
                        case "accuracy" -> totalAccuracy += value;
                    }
                }
            }
            String lastCombo = player.hasMetadata("last_combo_msg") ? player.getMetadata("last_combo_msg").get(0).asString() : "";
            if (!lastCombo.equals(comboId)) {
                String msg = comboConfig.getString(comboId + ".message");
                if (msg != null) player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
                player.setMetadata("last_combo_msg", new FixedMetadataValue(Main.getInstance(), comboId));
            }
        } else {
            player.removeMetadata("last_combo_msg", Main.getInstance());
        }


        // ── 4. HOOK MYATTRIBUTE (CỘNG DỒN TỪ SQLITE/TIỀM NĂNG) ──
        try {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MyAttribute")) {
                java.util.UUID uuid = player.getUniqueId();

                // Nhóm Attributes chính (Sử dụng API MyAttribute của bạn)
                totalHealth         += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "health");
                totalArmor          += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "armor");
                totalAttackSpeed    += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "attack_speed");
                totalMovementSpeed  += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "movement_speed");

                // Nhóm Mana & Regen
                totalMaxMana        += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "max_mana");
                totalManaRegen      += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "mana_regen");
                totalHealthRegen    += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "health_regen");

                // Nhóm Combat
                totalArmorPen       += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "penetration");
                totalAccuracy       += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "accuracy");
            }
        } catch (Throwable ignored) {}

        // 5. Áp dụng Particle
        if (foundParticleId != null) {
            org.ThienNguyen.Listener.Particle.ParticleManager.setEffect(player, foundParticleId);
        } else {
            org.ThienNguyen.Listener.Particle.ParticleManager.removeEffect(player);
        }

        // 6. Áp dụng Minecraft Attributes
        // Lưu ý: Đảm bảo bạn đã cập nhật hàm applyVanillaAttribute để có cơ chế so sánh giá trị cũ!
        applyVanillaAttribute(player, Attribute.GENERIC_MAX_HEALTH, WINDY_HEALTH_UUID, totalHealth);
        applyVanillaAttribute(player, Attribute.GENERIC_ARMOR, WINDY_ARMOR_UUID, totalArmor);
        applyVanillaAttribute(player, Attribute.GENERIC_ATTACK_SPEED, WINDY_ATTACK_SPEED_UUID, (4.0 * totalAttackSpeed) / 100.0);
        applyVanillaAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, WINDY_MOVEMENT_SPEED_UUID, (0.1 * totalMovementSpeed) / 100.0);

        // 7. Lưu Metadata để sử dụng trong Damage Listener & Placeholder
        player.setMetadata("windy_armor_pen", new FixedMetadataValue(Main.getInstance(), totalArmorPen));
        player.setMetadata("windy_health_regen", new FixedMetadataValue(Main.getInstance(), totalHealthRegen));
        player.setMetadata("windy_accuracy", new FixedMetadataValue(Main.getInstance(), totalAccuracy));

        applyManaStats(player, totalMaxMana, totalManaRegen);
    }
    private boolean isSlotAllowed(ItemStack item, FileConfiguration config, String stat, org.bukkit.inventory.EquipmentSlot currentSlot) {
        if (item == null || !item.hasItemMeta()) return true;

        // 1. Kiểm tra PDC
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "slot_" + stat.toLowerCase());
        String requiredSlot = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (requiredSlot != null) {
            // Chuẩn hóa slot hiện tại đang cầm
            String current = currentSlot.name().toLowerCase().replace("_", "");
            if (current.equals("hand")) current = "mainhand";

            // Nếu yêu cầu là 'any', cho phép luôn
            if (requiredSlot.equalsIgnoreCase("any")) return true;

            // --- ĐOẠN SỬA MỚI: Hỗ trợ dấu phẩy ---
            // Tách chuỗi yêu cầu bằng dấu phẩy (ví dụ: "offhand,mainhand" -> ["offhand", "mainhand"])
            String[] allowedParts = requiredSlot.split(",");
            for (String part : allowedParts) {
                if (part.trim().equalsIgnoreCase(current)) {
                    return true; // Nếu khớp với bất kỳ slot nào trong danh sách thì cho phép
                }
            }
            return false; // Không khớp cái nào thì từ chối
            // ------------------------------------
        }

        // 2. Logic config cũ (giữ nguyên)
        if (config == null) return true;
        List<String> allowedSlots = config.getStringList("stats-slots." + stat);
        if (allowedSlots == null || allowedSlots.isEmpty()) return true;
        return allowedSlots.contains(currentSlot.name());
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

    private static boolean mmocoreIssueLogged = false;

    private void applyManaStats(Player player, double maxMana, double manaRegen) {
        // 1. Kiểm tra và ưu tiên MMOCore
        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            try {
                net.Indyuce.mmocore.api.player.PlayerData data = net.Indyuce.mmocore.api.player.PlayerData.get(player);
                if (data != null && data.getStats() != null) {
                    var statsMap = data.getStats().getMap();

                    var maxManaInstance = statsMap.getInstance("MAX_MANA");
                    if (maxManaInstance != null) {
                        maxManaInstance.removeIf(key -> key.equals(MODIFIER_KEY));
                        if (maxMana != 0) {
                            maxManaInstance.registerModifier(new io.lumine.mythic.lib.api.stat.modifier.StatModifier(MODIFIER_KEY, "MAX_MANA", maxMana, io.lumine.mythic.lib.player.modifier.ModifierType.FLAT, io.lumine.mythic.lib.api.player.EquipmentSlot.OTHER, io.lumine.mythic.lib.player.modifier.ModifierSource.OTHER));
                        }
                    }

                    var regenInstance = statsMap.getInstance("MANA_REGENERATION");
                    if (regenInstance != null) {
                        regenInstance.removeIf(key -> key.equals(MODIFIER_KEY));
                        if (manaRegen != 0) {
                            regenInstance.registerModifier(new io.lumine.mythic.lib.api.stat.modifier.StatModifier(MODIFIER_KEY, "MANA_REGENERATION", manaRegen, io.lumine.mythic.lib.player.modifier.ModifierType.FLAT, io.lumine.mythic.lib.api.player.EquipmentSlot.OTHER, io.lumine.mythic.lib.player.modifier.ModifierSource.OTHER));
                        }
                    }

                    data.getStats().updateStats();
                    return;
                }
            } catch (NoClassDefFoundError | Exception ignored) {}
        }

        // 2. Logic cho Fabled (Studio MageMonkey)
        if (Bukkit.getPluginManager().isPluginEnabled("Fabled")) {
            try {
                studio.magemonkey.fabled.api.player.PlayerData data = studio.magemonkey.fabled.Fabled.getData(player);
                if (data != null) {

                    // --- QUAN TRỌNG: XÓA MODIFIER CŨ ĐỂ TRÁNH CỘNG DỒN ---
                    // Duyệt qua toàn bộ Map các modifier để tìm và xóa MODIFIER_KEY của plugin mình
                    data.getStatModifiers().forEach((attrKey, modifierList) -> {
                        modifierList.removeIf(mod -> mod.getName().equals(MODIFIER_KEY));
                    });

                    // --- ÁP DỤNG MAX MANA ---
                    if (maxMana != 0) {
                        studio.magemonkey.fabled.api.player.PlayerStatModifier maxManaMod =
                                new studio.magemonkey.fabled.api.player.PlayerStatModifier(
                                        MODIFIER_KEY,
                                        maxMana,
                                        studio.magemonkey.fabled.api.enums.Operation.ADD_NUMBER,
                                        false
                                );
                        // Key mặc định của Max Mana trong Fabled là "mana"
                        data.addStatModifier("mana", maxManaMod, true);
                    }

                    // --- ÁP DỤNG MANA REGEN (CẬP NHẬT MỚI) ---
                    if (manaRegen != 0) {
                        studio.magemonkey.fabled.api.player.PlayerStatModifier regenMod =
                                new studio.magemonkey.fabled.api.player.PlayerStatModifier(
                                        MODIFIER_KEY,
                                        manaRegen,
                                        studio.magemonkey.fabled.api.enums.Operation.ADD_NUMBER,
                                        false
                                );
                        // Key mặc định của Mana Regen trong Fabled thường là "mana-regen"
                        // (Bạn nên kiểm tra lại file attributes.yml của Fabled để chắc chắn key này)
                        data.addStatModifier("mana-regen", regenMod, true);
                    }

                    // Cập nhật lại thuộc tính cho Player
                    data.updatePlayerStat(player);
                }
            } catch (Exception ignored) {
                // Tránh crash nếu class không tồn tại
            }
        }
    }
    // Hàm kiểm tra xem món đồ có khớp với loại trang sức quy định của slot không
    private boolean isJewelryMatch(ItemStack item, int slotIdx) {
        if (item == null || item.getType().isAir()) return false;

        FileConfiguration config = Main.getInstance().getConfig();
        String requiredType = config.getString("jewelry.player-slots." + slotIdx + ".type");
        if (requiredType == null) return false;

        // Lấy loại trang sức thực tế của item từ NBT (PDC)
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        // JEWELRY_TYPE_KEY cần được định nghĩa hoặc lấy từ JewelryManager
        String itemType = meta.getPersistentDataContainer().get(
                new NamespacedKey(Main.getInstance(), "jewelry_type"), PersistentDataType.STRING);

        return requiredType.equalsIgnoreCase(itemType);
    }

    // Hàm lấy danh sách các slot trang sức từ config
    private java.util.Set<Integer> getJewelrySlots() {
        java.util.Set<Integer> slots = new java.util.HashSet<>();
        FileConfiguration config = Main.getInstance().getConfig();
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("jewelry.player-slots");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try { slots.add(Integer.parseInt(key)); } catch (Exception ignored) {}
            }
        }
        return slots;
    }
    private void logMMOCoreIssueOnce(String message) {
        if (!mmocoreIssueLogged) {
            Bukkit.getLogger().warning(message);
            mmocoreIssueLogged = true;
        }
    }

}