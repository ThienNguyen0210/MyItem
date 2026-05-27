package org.ThienNguyen.Listener.Station;

import com.google.gson.Gson;
import org.ThienNguyen.Main;
import org.ThienNguyen.Effect.BuffData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationCMD implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final StationDatabase stationDb;
    private final NamespacedKey stationCodeKey;
    private final NamespacedKey abilityKey;
    private final NamespacedKey stationVersionKey;
    private final Gson gson = new Gson();

    public StationCMD(Main plugin, StationDatabase stationDb) {
        this.plugin = plugin;
        this.stationDb = stationDb;
        this.stationCodeKey = new NamespacedKey(plugin, "item_station_code");
        this.abilityKey = new NamespacedKey(plugin, "item_abilities");
        this.stationVersionKey = new NamespacedKey(plugin, "station_version");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh chỉ dùng trong game!");
            return true;
        }

        if (!player.hasPermission("myitem.admin")) {
            player.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eSử dụng: /" + label + " station <addcode | update | clear | check>");
            return true;
        }

        if (!args[0].equalsIgnoreCase("sync")) return false;

        if (args.length < 2) {
            player.sendMessage("§eSub-command: §faddcode §7| §fupdate §7| §fclear §7| §fcheck");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "addcode" -> handleAddCode(player);
            case "update" -> handleStationUpdate(player);
            case "clear" -> handleClearStation(player);
            case "check" -> handleCheck(player);
            default -> player.sendMessage("§cSub-command không hợp lệ!");
        }
        return true;
    }

    
    private void handleAddCode(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {  
            player.sendMessage("§cBạn phải cầm item trên tay (không phải tay trống)!");
            return;
        }

        
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
        if (meta == null) {
            player.sendMessage("§cKhông thể xử lý item này!");
            return;
        }

        String code = generateRandomCode(64);
        meta.getPersistentDataContainer().set(stationCodeKey, PersistentDataType.STRING, code);
        item.setItemMeta(meta);

        
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        String displayName = meta.hasDisplayName() ? meta.getDisplayName() : null;
        Integer modelId = meta.hasCustomModelData() ? meta.getCustomModelData() : null;

        StationData data = new StationData();
        String dataJson = gson.toJson(data);
        String loreJson = gson.toJson(lore);


        String pdcJson = serializePDC(meta);

        stationDb.updateMasterData(code, dataJson, loreJson, displayName, modelId, pdcJson);
        player.sendMessage("§a[Station] §fĐã tạo và lưu mã mới: §e" + code);
        plugin.getLogger().info("[StationCMD] Đã tạo & lưu mã mới vào DB: " + code);
    }

    private String generateRandomCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    
    private void handleStationUpdate(Player admin) {
        ItemStack masterItem = admin.getInventory().getItemInMainHand();
        if (masterItem == null || masterItem.getType().isAir() || !masterItem.hasItemMeta()) {
            admin.sendMessage("§cBạn phải cầm item mẫu trên tay!");
            return;
        }

        String itemCode = masterItem.getItemMeta().getPersistentDataContainer()
                .get(stationCodeKey, PersistentDataType.STRING);

        if (itemCode == null || itemCode.isEmpty()) {
            admin.sendMessage("§cItem cầm tay chưa có mã Station!");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemMeta meta = masterItem.getItemMeta();

            
            String pdcJson = serializePDC(meta);

            
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            String displayName = meta.hasDisplayName() ? meta.getDisplayName() : null;
            Integer modelId = meta.hasCustomModelData() ? meta.getCustomModelData() : null;

            String loreJson = gson.toJson(lore);
            String dataJson = gson.toJson(new StationData()); 

            
            stationDb.updateMasterData(itemCode, dataJson, loreJson, displayName, modelId, pdcJson);

            int newVersion = stationDb.getMasterVersion(itemCode);

            Bukkit.getScheduler().runTask(plugin, () -> {
                int count = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    count += syncItems(p, itemCode, new StationData(), masterItem, newVersion, admin); 
                }
                admin.sendMessage("§b[Station] §fĐã cập nhật đầy đủ PDC + lore + name cho mã: §e" + itemCode);
                admin.sendMessage("§b[Station] §fĐã patch cho §a" + count + " §fitem online.");
            });
        });
    }
    private String serializePDC(ItemMeta meta) {
        if (meta == null) return "{}";

        Map<String, Object> pdcMap = new HashMap<>();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (NamespacedKey key : pdc.getKeys()) {
            
            if (key.equals(stationCodeKey) || key.equals(stationVersionKey)) continue;

            String keyStr = key.toString(); 

            
            if (pdc.has(key, PersistentDataType.STRING)) {
                pdcMap.put(keyStr, pdc.get(key, PersistentDataType.STRING));
            } else if (pdc.has(key, PersistentDataType.INTEGER)) {
                pdcMap.put(keyStr, pdc.get(key, PersistentDataType.INTEGER));
            } else if (pdc.has(key, PersistentDataType.DOUBLE)) {
                pdcMap.put(keyStr, pdc.get(key, PersistentDataType.DOUBLE));
            } else if (pdc.has(key, PersistentDataType.LONG)) {
                pdcMap.put(keyStr, pdc.get(key, PersistentDataType.LONG));
            } else if (pdc.has(key, PersistentDataType.FLOAT)) {
                pdcMap.put(keyStr, pdc.get(key, PersistentDataType.FLOAT));
            } else if (pdc.has(key, PersistentDataType.BYTE)) {
                pdcMap.put(keyStr, pdc.get(key, PersistentDataType.BYTE));
            } else if (pdc.has(key, PersistentDataType.SHORT)) {
                pdcMap.put(keyStr, pdc.get(key, PersistentDataType.SHORT));
            }
        }

        return gson.toJson(pdcMap);
    }
    private int syncItems(Player target, String code, StationData data, ItemStack masterItem, int version, Player admin) {
        int count = 0;
        ItemStack[] contents = target.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            String itemCode = meta.getPersistentDataContainer().get(stationCodeKey, PersistentDataType.STRING);
            if (!code.equals(itemCode)) continue;

            rebuildItem(item, data, masterItem, version);
            target.getInventory().setItem(i, item);
            count++;
        }
        return count;
    }

    private void rebuildItem(ItemStack item, StationData data, ItemStack masterItem, int newVersion) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        int currentVersion = meta.getPersistentDataContainer()
                .getOrDefault(stationVersionKey, PersistentDataType.INTEGER, 0);

        if (currentVersion >= newVersion) return; 

        
        List<String> masterLore = masterItem.getItemMeta().hasLore()
                ? new ArrayList<>(masterItem.getItemMeta().getLore())
                : new ArrayList<>();

        List<String> oldLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        int replaceCount = masterLore.size();
        List<String> customLore = new ArrayList<>();
        if (oldLore.size() > replaceCount) {
            customLore = oldLore.subList(replaceCount, oldLore.size());
        }

        List<String> finalLore = new ArrayList<>(masterLore);
        finalLore.addAll(customLore);

        meta.setLore(finalLore);

        
        ItemMeta masterMeta = masterItem.getItemMeta();
        mergePDC(masterMeta, meta);

        if (data.getRawAbilities() != null) {
            meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, data.getRawAbilities());
        }

        if (data.getEffects() != null) {
            data.getEffects().forEach((name, level) ->
                    BuffData.setEffect(item, name, level));
        }

        
        if (masterMeta.hasDisplayName()) {
            meta.setDisplayName(masterMeta.getDisplayName());
        }

        if (masterMeta.hasCustomModelData()) {
            meta.setCustomModelData(masterMeta.getCustomModelData());
        }

        
        meta.getPersistentDataContainer().set(stationVersionKey, PersistentDataType.INTEGER, newVersion);

        item.setItemMeta(meta);
    }

    private void mergePDC(ItemMeta sourceMeta, ItemMeta targetMeta) {
        if (sourceMeta == null || targetMeta == null) return;

        PersistentDataContainer source = sourceMeta.getPersistentDataContainer();
        PersistentDataContainer target = targetMeta.getPersistentDataContainer();

        for (NamespacedKey key : source.getKeys()) {
            
            

            if (source.has(key, PersistentDataType.STRING)) {
                target.set(key, PersistentDataType.STRING, source.get(key, PersistentDataType.STRING));
            }
            else if (source.has(key, PersistentDataType.INTEGER)) {
                target.set(key, PersistentDataType.INTEGER, source.get(key, PersistentDataType.INTEGER));
            }
            else if (source.has(key, PersistentDataType.DOUBLE)) {
                target.set(key, PersistentDataType.DOUBLE, source.get(key, PersistentDataType.DOUBLE));
            }
            else if (source.has(key, PersistentDataType.LONG)) {
                target.set(key, PersistentDataType.LONG, source.get(key, PersistentDataType.LONG));
            }
            else if (source.has(key, PersistentDataType.FLOAT)) {
                target.set(key, PersistentDataType.FLOAT, source.get(key, PersistentDataType.FLOAT));
            }
            else if (source.has(key, PersistentDataType.BYTE)) {
                target.set(key, PersistentDataType.BYTE, source.get(key, PersistentDataType.BYTE));
            }
            else if (source.has(key, PersistentDataType.SHORT)) {
                target.set(key, PersistentDataType.SHORT, source.get(key, PersistentDataType.SHORT));
            }
        }
    }

    private void handleClearStation(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§cBạn phải cầm item có mã Station!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String code = meta.getPersistentDataContainer().get(stationCodeKey, PersistentDataType.STRING);
        meta.getPersistentDataContainer().remove(stationCodeKey);
        item.setItemMeta(meta);

        if (code != null && !code.isEmpty()) {
            stationDb.deleteMasterData(code);
        }

        player.sendMessage("§a[Station] §fĐã xóa mã Station khỏi item và database!");
    }

    private void handleCheck(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§cBạn phải cầm item trên tay để check!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String code = pdc.get(stationCodeKey, PersistentDataType.STRING);
        if (code == null || code.isEmpty()) {
            player.sendMessage("§cItem cầm tay không có mã Station!");
            return;
        }

        int version = pdc.getOrDefault(stationVersionKey, PersistentDataType.INTEGER, 0);
        int masterVersion = stationDb.getMasterVersion(code);

        player.sendMessage("§b[Station Check]");
        player.sendMessage("§f- Mã Station: §e" + code);
        player.sendMessage("§f- Version hiện tại (trên item): §a" + version);
        player.sendMessage("§f- Version master (trên DB): §e" + masterVersion);
        player.sendMessage("§f- Trạng thái: " + (version >= masterVersion ? "§aĐã cập nhật" : "§cCần cập nhật"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("sync");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sync")) {
            completions.add("addcode");
            completions.add("update");
            completions.add("clear");
            completions.add("check");
        }

        return completions;
    }
}