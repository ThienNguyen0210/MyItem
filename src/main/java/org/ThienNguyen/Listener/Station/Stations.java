
package org.ThienNguyen.Listener.Station;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.ThienNguyen.Main;
import org.ThienNguyen.Command.Stats;
import org.ThienNguyen.Effect.BuffData;
import org.ThienNguyen.Element.ElementCore;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Stations implements Listener {

    private final Main plugin;
    private final NamespacedKey stationCodeKey;
    private final NamespacedKey abilityKey;
    private final NamespacedKey stationVersionKey;
    private final Stats statsHandler;
    private final StationDatabase stationDb;
    private final Gson gson = new Gson();

    // Cooldown per player (ms)
    private final Map<UUID, Long> playerCooldown = new HashMap<>();
    private static final long COOLDOWN_MS = 5000; // 5 giây

    public Stations(Main plugin, StationDatabase stationDb) {
        this.plugin = plugin;
        this.stationDb = stationDb;
        this.stationCodeKey = new NamespacedKey(plugin, "item_station_code");
        this.abilityKey = new NamespacedKey(plugin, "item_abilities");
        this.stationVersionKey = new NamespacedKey(plugin, "station_version");
        this.statsHandler = new Stats();
    }

    // ====================== TRIGGER: ĐỔI SLOT HOTBAR ======================
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        long currentTime = System.currentTimeMillis();
        long lastCheck = playerCooldown.getOrDefault(uuid, 0L);

        if (currentTime - lastCheck < COOLDOWN_MS) return;

        playerCooldown.put(uuid, currentTime);

        checkAllItems(player);
    }




    // ====================== QUÉT TOÀN BỘ INV + ARMOR + OFFHAND ======================
    private void checkAllItems(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            checkAndSyncItem(player, item);
        }

        for (ItemStack item : player.getInventory().getArmorContents()) {
            checkAndSyncItem(player, item);
        }

        checkAndSyncItem(player, player.getInventory().getItemInOffHand());
    }

    // ====================== CHECK & SYNC ITEM ======================
    private void checkAndSyncItem(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        String code = meta.getPersistentDataContainer().get(stationCodeKey, PersistentDataType.STRING);
        if (code == null) return;

        int itemVersion = meta.getPersistentDataContainer()
                .getOrDefault(stationVersionKey, PersistentDataType.INTEGER, 0);

        int masterVersion = stationDb.getMasterVersion(code);
        if (masterVersion <= 0 || masterVersion <= itemVersion) return;

        // Check player_sync trước
        int playerSyncVersion = stationDb.getPlayerSyncVersion(player.getUniqueId(), code);
        if (playerSyncVersion >= masterVersion) return;

        StationFullData fullData = stationDb.getStationData(code);
        if (fullData == null) return;

        StationData data = gson.fromJson(fullData.getDataJson(), StationData.class);

        final ItemStack finalItem = item;
        final Player finalPlayer = player;
        final StationFullData finalFullData = fullData;
        final int finalMasterVersion = masterVersion;
        final String finalCode = code;

        Bukkit.getScheduler().runTask(plugin, () -> {
            rebuildItem(finalItem, data, finalFullData);

            ItemMeta updatedMeta = finalItem.getItemMeta();
            if (updatedMeta != null) {
                updatedMeta.getPersistentDataContainer().set(stationVersionKey, PersistentDataType.INTEGER, finalMasterVersion);
                finalItem.setItemMeta(updatedMeta);
            }

            // Lưu player_sync để skip lần sau
            stationDb.updatePlayerSync(finalPlayer.getUniqueId(), finalCode, finalMasterVersion);


        });
    }

    // ====================== REBUILD ITEM (PDC GHI ĐÈ KEY TRÙNG) ======================
    private void rebuildItem(ItemStack item, StationData data, StationFullData fullData) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        // 1. Xử lý Lore (Thay thế phần đầu, giữ lại phần custom phía dưới)
        List<String> masterLore = new ArrayList<>();
        if (fullData.getLoreJson() != null) {
            masterLore = gson.fromJson(fullData.getLoreJson(), List.class);
        }

        List<String> oldLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        int replaceCount = masterLore.size();
        List<String> customLore = new ArrayList<>();
        if (oldLore.size() > replaceCount) {
            customLore = oldLore.subList(replaceCount, oldLore.size());
        }

        List<String> finalLore = new ArrayList<>(masterLore);
        finalLore.addAll(customLore);
        meta.setLore(finalLore);

        // 2. PDC Ghi đè từ JSON (MỚI - Đồng bộ toàn bộ dữ liệu metadata lưu trữ)
        if (fullData.getPdcJson() != null && !fullData.getPdcJson().equals("{}")) {
            mergePDCFromJson(fullData.getPdcJson(), meta);
        }

        // 3. Ghi đè các thuộc tính cụ thể từ StationData (Abilities, Effects, Elements)
        if (data.getRawAbilities() != null) {
            meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, data.getRawAbilities());
        }

        if (data.getEffects() != null) {
            // Áp dụng effect (cần truyền meta vào nếu BuffData hỗ trợ, hoặc update meta sau)
            data.getEffects().forEach((name, level) -> BuffData.setEffect(item, name, level));
            // Lưu ý: Nếu BuffData.setEffect tự thực hiện item.setItemMeta,
            // hãy đảm bảo nó đồng bộ với object 'meta' hiện tại.
        }

        if (data.getElements() != null) {
            data.getElements().forEach((elemId, level) -> ElementCore.setElement(item, elemId, level));
        }

        // 4. Remake Display Name và Model ID từ DB (Ghi đè tuyệt đối)
        if (fullData.getDisplayName() != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', fullData.getDisplayName()));
        }

        if (fullData.getCustomModelData() != null) {
            meta.setCustomModelData(fullData.getCustomModelData());
        }

        item.setItemMeta(meta);
    }
    private void mergePDCFromJson(String pdcJson, ItemMeta targetMeta) {
        if (pdcJson == null || pdcJson.isEmpty()) return;

        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> map = gson.fromJson(pdcJson, type);

        PersistentDataContainer target = targetMeta.getPersistentDataContainer();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String keyStr = entry.getKey();
            String[] parts = keyStr.split(":", 2);
            if (parts.length != 2) continue;

            NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
            Object value = entry.getValue();

            if (value instanceof String) {
                target.set(key, PersistentDataType.STRING, (String) value);
            } else if (value instanceof Number num) {
                if (num instanceof Integer || num instanceof Long) {
                    target.set(key, PersistentDataType.INTEGER, num.intValue());
                } else {
                    target.set(key, PersistentDataType.DOUBLE, num.doubleValue());
                }
            }
        }
    }
}