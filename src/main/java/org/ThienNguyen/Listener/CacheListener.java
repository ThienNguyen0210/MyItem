package org.ThienNguyen.Listener;

import org.ThienNguyen.API.PlayerRefreshStatsEvent;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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
    // THÊM vào đầu class CacheListener, sau các field hiện có:
    private static final NamespacedKey KEY_PCT_DAMAGE          = new NamespacedKey(Main.getInstance(), "pct_damage");
    private static final NamespacedKey KEY_PCT_CRIT_DMG_RED    = new NamespacedKey(Main.getInstance(), "pct_critical_damage_reduction");
    private static final NamespacedKey KEY_PCT_ARMOR           = new NamespacedKey(Main.getInstance(), "pct_armor");
    private static final NamespacedKey KEY_PCT_TRUE_DMG        = new NamespacedKey(Main.getInstance(), "pct_true_damage");
    private static final NamespacedKey KEY_PCT_MAGIC_DMG       = new NamespacedKey(Main.getInstance(), "pct_magic_damage");
    private static final NamespacedKey KEY_PCT_MAGIC_DEF       = new NamespacedKey(Main.getInstance(), "pct_magic_defense");
    private static final NamespacedKey KEY_PCT_PVE             = new NamespacedKey(Main.getInstance(), "pct_pve_damage");
    private static final NamespacedKey KEY_PCT_PVP             = new NamespacedKey(Main.getInstance(), "pct_pvp_damage");
    private static final NamespacedKey KEY_PCT_ALL_DMG         = new NamespacedKey(Main.getInstance(), "pct_all_damage");
    private static final NamespacedKey KEY_PCT_BOW_DMG         = new NamespacedKey(Main.getInstance(), "pct_bow_damage");
    private static final NamespacedKey KEY_PCT_DEATH_DMG       = new NamespacedKey(Main.getInstance(), "pct_death_damage");
    private static final NamespacedKey KEY_IS_PLACEHOLDER      = new NamespacedKey(Main.getInstance(), "is_placeholder");
    // Thiện nguyễn dev
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
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        p.removeMetadata("DEEP_WOUND_REDUCTION", Main.getInstance());
        p.removeMetadata("DEEP_WOUND_TASK", Main.getInstance());
        p.removeMetadata("CURSED_REDUCTION", Main.getInstance());
        delayRefresh(e.getPlayer());
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
    private static final long MESSAGE_COOLDOWN_MS = 3000L; 
    /**
     * Hàm tính toán và nhân các chỉ số dạng phần trăm (%) độc lập hoàn toàn với chỉ số cố định
     */
    private static void applyPercentStats(Player player, PlayerCombatCache.CombatStats stats, FileConfiguration slotConfig) {
        double pctBonusDmg = 0.0;
        double pctArmor = 0.0;
        double pctTrueDmg = 0.0;
        double pctCritDmgReduction = 0.0;
        double pctMagicDmg = 0.0;
        double pctMagicDef = 0.0;
        double pctPveBonus = 0.0;
        double pctPvpBonus = 0.0;
        double pctAllDamage = 0.0;
        double pctBowDamage = 0.0;
        double pctDeathDamage = 0.0;

        for (EquipmentSlot slot : ALL_SLOTS) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) continue;
            if (isMMOCoreAvailable() && !MMOCORE.canUse(player, item)) continue;

            var pdc = item.getItemMeta().getPersistentDataContainer();

            if (pdc.has(KEY_PCT_DAMAGE, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "damage", slot))
                pctBonusDmg += pdc.get(KEY_PCT_DAMAGE, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_CRIT_DMG_RED, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "critical_damage_reduction", slot))
                pctCritDmgReduction += pdc.get(KEY_PCT_CRIT_DMG_RED, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_ARMOR, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "armor", slot))
                pctArmor += pdc.get(KEY_PCT_ARMOR, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_TRUE_DMG, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "true_damage", slot))
                pctTrueDmg += pdc.get(KEY_PCT_TRUE_DMG, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_MAGIC_DMG, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "magic_damage", slot))
                pctMagicDmg += pdc.get(KEY_PCT_MAGIC_DMG, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_MAGIC_DEF, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "magic_defense", slot))
                pctMagicDef += pdc.get(KEY_PCT_MAGIC_DEF, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_PVE, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "pve_damage", slot))
                pctPveBonus += pdc.get(KEY_PCT_PVE, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_PVP, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "pvp_damage", slot))
                pctPvpBonus += pdc.get(KEY_PCT_PVP, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_ALL_DMG, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "all_damage", slot))
                pctAllDamage += pdc.get(KEY_PCT_ALL_DMG, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_BOW_DMG, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "bow_damage", slot))
                pctBowDamage += pdc.get(KEY_PCT_BOW_DMG, PersistentDataType.DOUBLE);

            if (pdc.has(KEY_PCT_DEATH_DMG, PersistentDataType.DOUBLE) && isAllowedPercent(item, slotConfig, "death_damage", slot))
                pctDeathDamage += pdc.get(KEY_PCT_DEATH_DMG, PersistentDataType.DOUBLE);
        }

        if (pctCritDmgReduction != 0.0) stats.totalCritDamageReduction *= (1.0 + (pctCritDmgReduction / 100.0));
        if (pctBonusDmg != 0.0) {
            stats.totalBonusDmg += 1.0;
            stats.totalBonusDmg *= (1.0 + (pctBonusDmg / 100.0)); // thiết kế đặc biệt hơn
            stats.totalBonusDmg -= 1.0;
        }
        if (pctArmor != 0.0)       stats.totalArmor        *= (1.0 + (pctArmor / 100.0));
        if (pctTrueDmg != 0.0)     stats.totalTrueDamage   *= (1.0 + (pctTrueDmg / 100.0));
        if (pctMagicDmg != 0.0)    stats.totalMagicDamage  *= (1.0 + (pctMagicDmg / 100.0));
        if (pctMagicDef != 0.0)    stats.totalMagicDefense *= (1.0 + (pctMagicDef / 100.0));
        if (pctPveBonus != 0.0)    stats.totalPveBonus     *= (1.0 + (pctPveBonus / 100.0));
        if (pctPvpBonus != 0.0)    stats.totalPvpBonus     *= (1.0 + (pctPvpBonus / 100.0));
        if (pctAllDamage != 0.0)   stats.totalAllDamage    *= (1.0 + (pctAllDamage / 100.0));
        if (pctBowDamage != 0.0)   stats.totalBowDamage    *= (1.0 + (pctBowDamage / 100.0));
        if (pctDeathDamage != 0.0) stats.totalDeathDamage *= (1.0 + (pctDeathDamage / 100.0));
    }

    /**
     * Hàm phụ kiểm tra slot quy định riêng cho chỉ số phần trăm độc lập
     */
    private static boolean isAllowedPercent(ItemStack item, FileConfiguration config, String stat, EquipmentSlot currentSlot) {
        if (item == null || !item.hasItemMeta()) return true;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), "slot_pct_" + stat.toLowerCase());
        String requiredSlot = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (requiredSlot != null) {
            if (requiredSlot.equalsIgnoreCase("any")) return true;
            String current = normalizeSlot(currentSlot);

            String[] allowedSlots = requiredSlot.split(",");
            for (String s : allowedSlots) {
                if (s.trim().equalsIgnoreCase(current)) return true;
            }
            return false;
        }

        if (config == null) return true;
        List<String> list = config.getStringList("stats-slots." + stat);
        if (list == null || list.isEmpty()) return true;

        String currentNormalized = normalizeSlot(currentSlot);
        String originalName = currentSlot.name();

        for (String allowed : list) {
            if (allowed.equalsIgnoreCase(currentNormalized) ||
                    allowed.equalsIgnoreCase(originalName)) {
                return true;
            }
        }
        return false;
    }
    public static void refreshCache(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerCombatCache.CombatStats stats = new PlayerCombatCache.CombatStats();
        stats.clear();
        FileConfiguration slotConfig   = Main.getInstance().getStatsSettingsConfig();
        FileConfiguration gemConfig    = Main.getInstance().getGemConfig();
        FileConfiguration eConfig      = Main.getInstance().getElementConfig();
        FileConfiguration comboConfig  = Main.getInstance().getComboConfig();




        String currentComboId = org.ThienNguyen.Listener.ItemCombo.ComboListener.getFullSetComboId(player);
        if (currentComboId == null) currentComboId = "";

        String comboMetadataKey = "last_notified_combo";
        String lastNotifiedId = "";
        if (player.hasMetadata(comboMetadataKey)) {
            lastNotifiedId = player.getMetadata(comboMetadataKey).get(0).asString();
        }

        if (!currentComboId.equals(lastNotifiedId)) {
            if (!currentComboId.isEmpty()) {
                player.setMetadata(comboMetadataKey, new FixedMetadataValue(Main.getInstance(), currentComboId));
            } else {
                player.removeMetadata(comboMetadataKey, Main.getInstance());
            }

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
                LAST_COMBO_MESSAGE_TIME.put(uuid, now);
            }
        }




        if (currentComboId != null) {
            org.bukkit.configuration.ConfigurationSection comboStats = comboConfig.getConfigurationSection(currentComboId + ".stats");
            if (comboStats != null) {
                for (String key : comboStats.getKeys(false)) {
                    updateStat(stats, key, comboStats.getDouble(key));
                }
            }
        }




        stats.clearWeaponElements();
        stats.bestAbilities.clear();
        stats.totalArmor = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR).getValue();




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

            if (isAllowed(item, slotConfig, "exp_bonus", slot)) {
                stats.totalExpBonus += ExpBonus.get(item);
            }
            if (isAllowed(item, slotConfig, "movement_speed", slot)) {
                stats.totalMovementSpeed += MovementSpeed.get(item);
            }

            if (isAllowed(item, slotConfig, "damage", slot))          stats.totalBonusDmg     += Damage.getDamage(item);
            if (isAllowed(item, slotConfig, "critical_damage_reduction", slot))  stats.totalCritDamageReduction += CritDamageReduction.get(item);
            if (isAllowed(item, slotConfig, "damage_reduction", slot))   stats.totalDamageReduction += DamageReduction.get(item);
            if (isAllowed(item, slotConfig, "deep_wound", slot))   stats.totalDeepWound += DeepWound.get(item);
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




        Map<Integer, ItemStack> jewelryItems = JewelryManager.getCachedJewelry(player.getUniqueId());
        for (ItemStack jItem : jewelryItems.values()) {
            applyJewelryStats(stats, jItem, eConfig, gemConfig);
        }

        FileConfiguration mainConfig = Main.getInstance().getConfig();
        if (mainConfig.getBoolean("jewelry.enable-player-inventory", false)) {
            ConfigurationSection pSlots = mainConfig.getConfigurationSection("jewelry.player-slots");
            if (pSlots != null) {
                for (String key : pSlots.getKeys(false)) {
                    try {
                        int slotIdx = Integer.parseInt(key);
                        ItemStack item = player.getInventory().getItem(slotIdx);
                        if (item != null && item.getType() != Material.AIR && !isPlaceholder(item)) {
                            applyJewelryStats(stats, item, eConfig, gemConfig);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }




        double finalKB = Math.max(0.0, Math.min(1.0, stats.totalKnockbackResist));
        org.bukkit.attribute.AttributeInstance kbAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbAttr.setBaseValue(finalKB);
        }




        try {
            if (Bukkit.getPluginManager().isPluginEnabled("MyAttribute")) {
                UUID uuid = player.getUniqueId();

                // --- FLAT BONUS ---
                stats.totalDeathDamage           += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "death_damage");
                stats.totalKnockbackResist           += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "knockback_resistance");
                stats.totalAllDamage           += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "all_damage");
                stats.totalBlock           += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "block_rate");
                stats.totalBowDamage           += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "bow_damage");
                stats.totalThorns           += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "thorns");
                stats.totalDeepWound           += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "deep_wound");
                stats.totalDamageReduction     += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "damage_reduction");
                stats.totalCritDamageReduction += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "critical_damage_reduction");
                stats.totalExpBonus            += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "exp_bonus");
                stats.totalArmorPen            += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "armor_pen");
                stats.totalBonusDmg            += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "damage");
                stats.totalTrueDamage          += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "true_damage");
                stats.totalAccuracy            += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "accuracy");
                stats.totalCritChance          += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "critical_chance");
                stats.totalCritDamage          += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "critical_damage");
                stats.totalPenetration         += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "penetration");
                stats.totalLifesteal           += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "lifesteal");
                stats.totalPveBonus            += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "pve_damage");
                stats.totalPvpBonus            += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "pvp_damage");
                stats.totalMagicDamage         += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "magic_damage");
                stats.totalDodge               += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "dodge_rate");
                stats.totalArmor               += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "armor");
                stats.totalMagicDefense        += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "magic_defense");
                stats.totalPveDef              += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "pve_defense");
                stats.totalPvpDef              += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "pvp_defense");
                stats.totalAllDefense          += org.ThienDev.Api.AttributeAPI.getBonus(uuid, "all_defense");

                // --- PERCENT BONUS ---
                double _p;
                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "damage");
                if (_p != 0.0) stats.totalBonusDmg            *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "true_damage");
                if (_p != 0.0) stats.totalTrueDamage          *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "magic_damage");
                if (_p != 0.0) stats.totalMagicDamage         *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "magic_defense");
                if (_p != 0.0) stats.totalMagicDefense        *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "pve_damage");
                if (_p != 0.0) stats.totalPveBonus            *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "pvp_damage");
                if (_p != 0.0) stats.totalPvpBonus            *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "all_damage");
                if (_p != 0.0) stats.totalAllDamage           *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "bow_damage");
                if (_p != 0.0) stats.totalBowDamage           *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "death_damage");
                if (_p != 0.0) stats.totalDeathDamage         *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "armor");
                if (_p != 0.0) stats.totalArmor               *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "critical_damage");
                if (_p != 0.0) stats.totalCritDamage          *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "critical_damage_reduction");
                if (_p != 0.0) stats.totalCritDamageReduction *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "armor_pen");
                if (_p != 0.0) stats.totalArmorPen            *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "penetration");
                if (_p != 0.0) stats.totalPenetration         *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "lifesteal");
                if (_p != 0.0) stats.totalLifesteal           *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "dodge_rate");
                if (_p != 0.0) stats.totalDodge               *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "damage_reduction");
                if (_p != 0.0) stats.totalDamageReduction     *= (1 + _p / 100.0);

                // --- BỔ SUNG CÁC STATS CÒN THIẾU ---
                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "pve_defense");
                if (_p != 0.0) stats.totalPveDef              *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "pvp_defense");
                if (_p != 0.0) stats.totalPvpDef              *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "all_defense");
                if (_p != 0.0) stats.totalAllDefense          *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "thorns");
                if (_p != 0.0) stats.totalThorns              *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "knockback_resistance");
                if (_p != 0.0) stats.totalKnockbackResist     *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "block_rate");
                if (_p != 0.0) stats.totalBlock               *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "exp_bonus");
                if (_p != 0.0) stats.totalExpBonus            *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "accuracy");
                if (_p != 0.0) stats.totalAccuracy            *= (1 + _p / 100.0);

                _p = org.ThienDev.Api.AttributeAPI.getPercentBonus(uuid, "deep_wound");
                if (_p != 0.0) stats.totalDeepWound           *= (1 + _p / 100.0);
            }
        } catch (Throwable ignored) {}




        applyPercentStats(player, stats, slotConfig);




        PlayerRefreshStatsEvent apiEvent = new PlayerRefreshStatsEvent(player, stats);
        org.bukkit.Bukkit.getPluginManager().callEvent(apiEvent);
        StatsListener.getInstance().updatePlayerStats(player);
        PlayerCombatCache.updateCache(player.getUniqueId(), stats);
    }

    
    private static void updateStat(PlayerCombatCache.CombatStats stats, String type, double val) {
        if (val == 0) return; 

        switch (type.toLowerCase()) {
            case "exp_bonus", "exp" -> stats.totalExpBonus += val;
            case "critical_damage_reduction" -> stats.totalCritDamageReduction += val;
            case "movement_speed", "speed" -> stats.totalMovementSpeed += val;
            case "damage_reduction" -> stats.totalDamageReduction += val;
            case "damage" -> stats.totalBonusDmg += val;
            case "deep_wound" -> stats.totalDeepWound += val;
            case "pve_damage" -> stats.totalPveBonus += val;
            case "pvp_damage" -> stats.totalPvpBonus += val;
            case "all_damage" -> stats.totalAllDamage += val;
            case "magic_damage" -> stats.totalMagicDamage += val;
            case "bow_damage" -> stats.totalBowDamage += val;
            case "true_damage" -> stats.totalTrueDamage += val;
            case "death_damage" -> stats.totalDeathDamage += val;

            
            case "critical_chance", "crit_chance" -> stats.totalCritChance += val;
            case "critical_damage", "crit_damage" -> stats.totalCritDamage += val;
            case "penetration" -> stats.totalPenetration += val;
            case "armor_pen" -> stats.totalArmorPen += val;
            case "lifesteal" -> stats.totalLifesteal += val;
            case "accuracy" -> stats.totalAccuracy += val; 
            
            case "armor" -> stats.totalArmor += val;
            case "pve_defense", "pve_def" -> stats.totalPveDef += val;
            case "pvp_defense", "pvp_def" -> stats.totalPvpDef += val;
            case "all_defense" -> stats.totalAllDefense += val;
            case "magic_defense", "magic_def" -> stats.totalMagicDefense += val;
            case "dodge_rate" -> stats.totalDodge += val;
            case "block_rate" -> stats.totalBlock += val;
            case "thorns" -> stats.totalThorns += val;
            case "knockback_resistance" -> stats.totalKnockbackResist += val;

            
            
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (e.getPlayer().isOnline()) {
                refreshCache(e.getPlayer());
            }
        }, 5L);
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
        stats.totalExpBonus += ExpBonus.get(jItem);
        stats.totalCritDamageReduction += CritDamageReduction.get(jItem);
        stats.totalBonusDmg     += Damage.getDamage(jItem);
        stats.totalDamageReduction += DamageReduction.get(jItem);
        stats.totalPveBonus     += PveDamage.get(jItem);
        stats.totalPvpBonus     += PvpDamage.get(jItem);
        stats.totalDeepWound += DeepWound.get(jItem);
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

        return item.getItemMeta().getPersistentDataContainer()
                .has(KEY_IS_PLACEHOLDER, PersistentDataType.BOOLEAN);
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

    /**
     * Normalize EquipmentSlot cho thống nhất
     */
    private static String normalizeSlot(EquipmentSlot slot) {
        if (slot == null) return "";
        String name = slot.name().toLowerCase().replace("_", "");
        if (name.equals("hand")) return "mainhand";
        if (name.equals("offhand")) return "offhand";
        return name;
    }

    /**
     * Hàm kiểm tra slot được phép (đã fix)
     */
    private static boolean isAllowed(ItemStack item, FileConfiguration config, String stat, EquipmentSlot currentSlot) {
        if (item == null || !item.hasItemMeta()) return true;

        // Kiểm tra PDC custom slot
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "slot_" + stat.toLowerCase());
        String requiredSlot = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (requiredSlot != null) {
            if (requiredSlot.equalsIgnoreCase("any")) return true;

            String current = normalizeSlot(currentSlot);
            String[] allowedSlots = requiredSlot.split(",");
            for (String s : allowedSlots) {
                if (s.trim().equalsIgnoreCase(current)) return true;
            }
            return false;
        }

        // Fallback config
        if (config == null) return true;
        List<String> list = config.getStringList("stats-slots." + stat);
        if (list == null || list.isEmpty()) return true;

        String currentNormalized = normalizeSlot(currentSlot);
        String originalName = currentSlot.name();

        for (String allowed : list) {
            if (allowed.equalsIgnoreCase(currentNormalized) ||
                    allowed.equalsIgnoreCase(originalName)) {
                return true;
            }
        }
        return false;
    }
    private static boolean isMMOCoreAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("MMOCore");
    }
}