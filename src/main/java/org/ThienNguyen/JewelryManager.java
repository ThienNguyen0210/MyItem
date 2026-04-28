package org.ThienNguyen;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.ThienNguyen.Stat.*;
import org.ThienNguyen.Element.ElementCore;
import org.ThienNguyen.GemSocket.GemLogic;
import org.ThienNguyen.Hook.MMOCORE;

// Thêm Listener vào đây
public class JewelryManager implements InventoryHolder, org.bukkit.event.Listener {
    private static final String DB_FILE = "jewelry.db";
    private static final Map<UUID, Map<Integer, ItemStack>> jewelryCache = new ConcurrentHashMap<>(); // Cache item per player (slot -> item)
    private static final NamespacedKey JEWELRY_TYPE_KEY = new NamespacedKey(Main.getInstance(), "jewelry_type");

    private final JavaPlugin plugin = Main.getInstance();
    private final Player player;
    private final Inventory gui;

    // Danh sách slot từ config.yml (hỗ trợ nhiều slot cùng key)
    private final List<JewelrySlot> jewelrySlots = new ArrayList<>();
    private final Map<Integer, ItemStack> currentJewelry = new HashMap<>(); // slot index -> item (tạm cho GUI)

    // Slot hiển thị armor và main hand (read-only)
    private final Map<String, Integer> displaySlots = new HashMap<>(); // e.g., "head" -> slot index

    public JewelryManager(Player player) {
        this.player = player;

        // 1. Luôn load config trước (Dùng chung cho cả GUI và Listener)
        loadConfigSlots();
        loadDisplaySlots();

        // 2. Kiểm tra nếu player null (Trường hợp đăng ký Listener ở Main.java)
        if (player == null) {
            this.gui = null;
            return; // Thoát sớm để không gây lỗi NullPointerException
        }

        // 3. Kiểm tra tính năng có bật không (Dành cho người chơi)
        if (!isJewelryEnabled()) {
            // Chỉ gửi tin nhắn nếu player thực sự đang mở GUI
            player.sendMessage("§cTính năng trang sức đang bị tắt!");
            this.gui = Bukkit.createInventory(null, 9, "§cTắt");
            return;
        }

        // 4. Khởi tạo GUI và Load dữ liệu cho người chơi cụ thể
        this.gui = Bukkit.createInventory(this, getGuiSize(), getGuiTitle());
        loadFromDatabase();
        setupGui();
    }

    private boolean isJewelryEnabled() {
        return plugin.getConfig().getBoolean("jewelry.enabled", true);
    }

    private void loadConfigSlots() {
        jewelrySlots.clear();
        ConfigurationSection jewelrySec = plugin.getConfig().getConfigurationSection("jewelry");
        if (jewelrySec == null) {
            plugin.getLogger().warning("[Jewelry] Không tìm thấy phần 'jewelry' trong config.yml!");
            return;
        }

        ConfigurationSection slotsSec = jewelrySec.getConfigurationSection("slots");
        if (slotsSec == null) return;

        for (String key : slotsSec.getKeys(false)) {
            // Trường hợp 1: Là list của map (nhiều slot cùng key)
            if (slotsSec.isList(key)) {
                List<?> list = slotsSec.getList(key);
                if (list == null) continue;

                for (Object obj : list) {
                    if (!(obj instanceof Map<?, ?> slotMap)) continue;

                    Object nameObj = slotMap.get("name");
                    Object slotObj = slotMap.get("slot");

                    String name = (nameObj instanceof String) ? (String) nameObj : key;
                    int slot = (slotObj instanceof Number) ? ((Number) slotObj).intValue() : -1;

                    if (slot < 0 || slot >= 54) continue;

                    jewelrySlots.add(new JewelrySlot(key, name.replace("&", "§"), slot));
                }
            }
            // Trường hợp 2: Là map đơn (cấu hình cũ)
            else if (slotsSec.isConfigurationSection(key)) {
                ConfigurationSection singleSlot = slotsSec.getConfigurationSection(key);
                if (singleSlot == null) continue;

                String name = singleSlot.getString("name", key);
                int slot = singleSlot.getInt("slot", -1);

                if (slot < 0 || slot >= 54) continue;

                jewelrySlots.add(new JewelrySlot(key, name.replace("&", "§"), slot));
            }
        }

        jewelrySlots.sort(Comparator.comparingInt(s -> s.slot));
    }

    private void loadDisplaySlots() {
        displaySlots.clear();
        ConfigurationSection jewelrySec = plugin.getConfig().getConfigurationSection("jewelry");
        if (jewelrySec == null) return;

        ConfigurationSection displaySec = jewelrySec.getConfigurationSection("gui.display-slots");
        if (displaySec == null) return;

        for (String key : displaySec.getKeys(false)) {
            int slot = displaySec.getInt(key, -1);
            if (slot >= 0 && slot < getGuiSize()) {
                displaySlots.put(key, slot);
            }
        }
    }

    private int getGuiSize() {
        int size = plugin.getConfig().getInt("jewelry.gui.size", 27);
        return Math.max(9, Math.min(54, (size / 9) * 9));
    }

    private String getGuiTitle() {
        String title = plugin.getConfig().getString("jewelry.gui.title", "&6&l Trang Sức");
        return title.replace("&", "§");
    }

    private Material getFillerMaterial() {
        String matStr = plugin.getConfig().getString("jewelry.gui.filler-material", "BLACK_STAINED_GLASS_PANE");
        try {
            return Material.valueOf(matStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.BLACK_STAINED_GLASS_PANE; // fallback
        }
    }
    private ItemStack createFillerItem() {
        ConfigurationSection guiSec = plugin.getConfig().getConfigurationSection("jewelry.gui");
        String matStr = (guiSec != null) ? guiSec.getString("filler-material", "BLACK_STAINED_GLASS_PANE") : "BLACK_STAINED_GLASS_PANE";

        Material mat;
        try {
            mat = Material.valueOf(matStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.BLACK_STAINED_GLASS_PANE;
        }

        ItemStack filler = new ItemStack(mat);
        ItemMeta meta = filler.getItemMeta();

        if (meta != null && guiSec != null) {
            // Đọc Name (mặc định là trống nếu không có)
            String name = guiSec.getString("filler-name", " ");
            meta.setDisplayName(name.replace("&", "§"));

            // Đọc Lore
            List<String> lore = guiSec.getStringList("filler-lore");
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream().map(line -> line.replace("&", "§")).toList());
            }

            // Đọc CustomModelData
            int modelId = guiSec.getInt("filler-model-id", 0);
            if (modelId != 0) {
                meta.setCustomModelData(modelId);
            }

            filler.setItemMeta(meta);
        }
        return filler;
    }
    private void setupGui() {
        // Tạo item filler từ cấu hình
        ItemStack filler = createFillerItem();

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        // Đặt các slot trang sức và hiển thị trang bị (giữ nguyên logic cũ)
        for (JewelrySlot js : jewelrySlots) {
            ItemStack itemInSlot = currentJewelry.get(js.slot);
            if (itemInSlot == null || itemInSlot.getType().isAir()) {
                gui.setItem(js.slot, createPlaceholderItem(js));
            } else {
                gui.setItem(js.slot, itemInSlot);
            }
        }
        updatePlayerEquipmentDisplay();
    }

    private ItemStack createPlaceholderItem(JewelrySlot js) {
        ConfigurationSection placeholderSec = plugin.getConfig().getConfigurationSection("jewelry.placeholder");
        if (placeholderSec == null) {
            return new ItemStack(Material.BARRIER); // fallback nếu không config
        }

        Material mat = Material.valueOf(placeholderSec.getString("material", "BARRIER").toUpperCase());
        ItemStack placeholder = new ItemStack(mat);

        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(placeholderSec.getString("name", "&7Slot Trống").replace("&", "§").replace("%slot_name%", js.name));
            meta.setLore(placeholderSec.getStringList("lore").stream().map(line -> line.replace("&", "§").replace("%slot_name%", js.name)).toList());
            meta.setCustomModelData(placeholderSec.getInt("model-id", 0));
            placeholder.setItemMeta(meta);
        }

        return placeholder;
    }

    private void updatePlayerEquipmentDisplay() {
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        // Ánh xạ các loại trang bị vào slot tương ứng
        updateSingleDisplaySlot("head", armor[3]);
        updateSingleDisplaySlot("chest", armor[2]);
        updateSingleDisplaySlot("legs", armor[1]);
        updateSingleDisplaySlot("feet", armor[0]);
        updateSingleDisplaySlot("mainhand", mainHand);
        updateSingleDisplaySlot("offhand", offHand);
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }

    public void onClick(InventoryClickEvent e) {
        // 1. ĐỒNG BỘ HIỂN THỊ KHI THAO TÁC TRONG TÚI ĐỒ CÁ NHÂN
        if (e.getClickedInventory() == player.getInventory()) {
            Bukkit.getScheduler().runTask(plugin, this::updatePlayerEquipmentDisplay);
            return;
        }

        if (e.getClickedInventory() != gui) return;
        int raw = e.getRawSlot();

        // 2. CHẶN LẤY ITEM TỪ CÁC ô HIỂN THỊ (Armor/Mainhand)
        if (displaySlots.values().contains(raw)) {
            e.setCancelled(true);
            return;
        }

        // 3. XỬ LÝ SLOT TRANG SỨC
        JewelrySlot clickedSlot = jewelrySlots.stream().filter(s -> s.slot == raw).findFirst().orElse(null);
        if (clickedSlot == null) {
            e.setCancelled(true);
            return;
        }

        ItemStack currentItem = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        ItemStack placeholder = createPlaceholderItem(clickedSlot);

        // Xử lý khi click vào Placeholder
        if (currentItem != null && currentItem.isSimilar(placeholder)) {
            e.setCancelled(true);
            if (cursor != null && !cursor.getType().isAir() && isValidJewelry(cursor, clickedSlot)) {
                ItemStack toPut = cursor.clone();
                gui.setItem(raw, toPut);
                e.getView().setCursor(new ItemStack(Material.AIR));

                // --- LƯU TỨC THÌ ---
                updateItemAndSave(clickedSlot.slot, toPut);
            }
            return;
        }

        // Kiểm tra tính hợp lệ khi đặt item mới vào slot đang có đồ
        if (cursor != null && !cursor.getType().isAir()) {
            if (!isValidJewelry(cursor, clickedSlot)) {
                e.setCancelled(true);
                return;
            }
        }

        // 4. CẬP NHẬT TRẠNG THÁI VÀ LƯU DỮ LIỆU NGAY LẬP TỨC
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack itemNow = gui.getItem(raw);

            if (itemNow == null || itemNow.getType().isAir() || itemNow.isSimilar(placeholder)) {
                // Nếu slot trống hoặc chỉ có placeholder -> Xóa khỏi DB/Cache
                gui.setItem(raw, placeholder);
                removeItemAndSave(clickedSlot.slot);
            } else {
                // Nếu có item thật -> Lưu vào DB/Cache
                updateItemAndSave(clickedSlot.slot, itemNow);
            }
            updatePlayerEquipmentDisplay();
        });
    }

// --- THÊM 2 HÀM HELPER NÀY VÀO CLASS JEWELRYMANAGER ---

    private void updateItemAndSave(int slot, ItemStack item) {
        currentJewelry.put(slot, item);
        updateJewelryCache(player.getUniqueId(), currentJewelry);
        saveToDatabase(); // Gọi hàm saveToDatabase có sẵn của bạn
    }

    private void removeItemAndSave(int slot) {
        currentJewelry.remove(slot);
        updateJewelryCache(player.getUniqueId(), currentJewelry);
        saveToDatabase();
    }

    /**
     * Hàm hỗ trợ kiểm tra tính hợp lệ của trang sức
     */
    private boolean isValidJewelry(ItemStack item, JewelrySlot slot) {
        if (!MMOCORE.canUse(player, item)) {
            player.sendMessage("§cBạn không đủ cấp độ hoặc class để trang bị!");
            return false;
        }

        String itemType = (item.getItemMeta() != null) ?
                item.getItemMeta().getPersistentDataContainer().get(JEWELRY_TYPE_KEY, PersistentDataType.STRING) : null;

        if (itemType == null || !itemType.equals(slot.key)) {
            player.sendMessage("§cVật phẩm này không phù hợp với slot " + slot.name + "!");
            return false;
        }
        return true;
    }


    /**
     * Cập nhật 1 slot hiển thị, nếu trống thì fill Placeholder
     */
    private void updateSingleDisplaySlot(String type, ItemStack item) {
        if (!displaySlots.containsKey(type)) return;
        int slotIdx = displaySlots.get(type);

        if (item == null || item.getType().isAir()) {
            gui.setItem(slotIdx, createEquipmentPlaceholder(type));
        } else {
            gui.setItem(slotIdx, item.clone());
        }
    }

    /**
     * Tạo icon gợi ý cho các ô trang bị khi trống
     */
    private ItemStack createEquipmentPlaceholder(String type) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("jewelry.gui.display-placeholders." + type);

        // Nếu không có config riêng cho từng ô, dùng Barrier làm mặc định
        Material mat = (sec != null) ? Material.valueOf(sec.getString("material", "BARRIER").toUpperCase()) : Material.BARRIER;
        ItemStack placeholder = new ItemStack(mat);

        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            String name = (sec != null) ? sec.getString("name", "§7Ô trang bị trống") : "§7Trống";
            meta.setDisplayName(name.replace("&", "§"));
            meta.setCustomModelData((sec != null) ? sec.getInt("model-id", 0) : 0);
            placeholder.setItemMeta(meta);
        }
        return placeholder;
    }

    /**
     * Xử lý trường hợp người chơi kéo thả item (Drag) vào nhiều slot cùng lúc
     */
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (e.getInventory() != gui) return;

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < gui.getSize()) {
                JewelrySlot js = jewelrySlots.stream().filter(s -> s.slot == rawSlot).findFirst().orElse(null);
                // Chặn kéo thả vào ô hiển thị hoặc sai type trang sức
                if (js == null || displaySlots.values().contains(rawSlot)) {
                    e.setCancelled(true);
                    return;
                }

                // Kiểm tra type PDC
                ItemStack draggedItem = e.getOldCursor();
                String itemType = (draggedItem.getItemMeta() != null) ?
                        draggedItem.getItemMeta().getPersistentDataContainer().get(JEWELRY_TYPE_KEY, PersistentDataType.STRING) : null;

                if (itemType == null || !itemType.equals(js.key)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        // Update lại trang bị sau khi drag hoàn tất
        Bukkit.getScheduler().runTask(plugin, this::updatePlayerEquipmentDisplay);
    }

    public void onClose(InventoryCloseEvent e) {
        // 1. Dọn Map cũ
        currentJewelry.clear();

        // 2. Quét thực tế từ GUI
        for (JewelrySlot js : jewelrySlots) {
            ItemStack item = gui.getItem(js.slot);
            ItemStack placeholder = createPlaceholderItem(js);

            if (item != null && !item.getType().isAir() && !item.isSimilar(placeholder)) {
                currentJewelry.put(js.slot, item.clone());
            }
        }

        // 3. Cập nhật Cache (RAM)
        updateJewelryCache(player.getUniqueId(), currentJewelry);

        // 4. Lưu xuống Database (Ổ cứng)
        saveToDatabase();

        // 5. Cập nhật chỉ số
        updatePlayerEquipmentDisplay();
    }

    // ────────────────────────────────────────────────
    //                  DATABASE
    // ────────────────────────────────────────────────

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/" + DB_FILE);
    }

    private void ensureTable() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS jewelry (
                    uuid TEXT NOT NULL,
                    slot_key TEXT NOT NULL,
                    item_data TEXT NOT NULL,
                    PRIMARY KEY (uuid, slot_key)
                )
                """);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadFromDatabase() {
        currentJewelry.clear();
        ensureTable();
        UUID uuid = player.getUniqueId();
        if (jewelryCache.containsKey(uuid)) {
            currentJewelry.putAll(jewelryCache.get(uuid)); // Load từ cache nếu có
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT slot_key, item_data FROM jewelry WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            Map<Integer, ItemStack> loaded = new HashMap<>();
            while (rs.next()) {
                String key = rs.getString("slot_key");
                String data = rs.getString("item_data");
                ItemStack item = deserializeItem(data);
                // Lưu theo slot, hỗ trợ nhiều slot cùng key
                jewelrySlots.stream().filter(s -> s.key.equals(key)).forEach(js -> {
                    if (loaded.containsKey(js.slot)) return;  // Tránh overwrite nếu nhiều
                    loaded.put(js.slot, item);
                });
            }
            currentJewelry.putAll(loaded);
            jewelryCache.put(uuid, new HashMap<>(loaded)); // Save to cache
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void saveToDatabase() {
        ensureTable();
        UUID uuid = player.getUniqueId();

        // Sử dụng Transaction để đảm bảo an toàn dữ liệu
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            // BƯỚC 1: Xóa sạch dữ liệu cũ của player này trong DB
            try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM jewelry WHERE uuid = ?")) {
                deletePs.setString(1, uuid.toString());
                deletePs.executeUpdate();
            }

            // BƯỚC 2: Lưu lại những item đang thực sự có trong currentJewelry
            try (PreparedStatement insertPs = conn.prepareStatement("INSERT INTO jewelry (uuid, slot_key, item_data) VALUES (?, ?, ?)")) {
                Map<String, ItemStack> savedByKey = new HashMap<>();
                for (JewelrySlot js : jewelrySlots) {
                    ItemStack item = currentJewelry.get(js.slot);
                    // Chỉ lưu những item không phải không khí và không phải placeholder
                    if (item != null && !item.getType().isAir() && !item.isSimilar(createPlaceholderItem(js))) {
                        savedByKey.put(js.key, item);
                    }
                }

                for (Map.Entry<String, ItemStack> entry : savedByKey.entrySet()) {
                    insertPs.setString(1, uuid.toString());
                    insertPs.setString(2, entry.getKey());
                    insertPs.setString(3, serializeItem(entry.getValue()));
                    insertPs.addBatch();
                }
                insertPs.executeBatch();
            }

            conn.commit(); // Hoàn tất lưu
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ────────────────────────────────────────────────
    //          SERIALIZE / DESERIALIZE ITEM
    // ────────────────────────────────────────────────

    private static String serializeItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(item);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Thêm từ khóa static để có thể gọi từ các hàm static khác như loadDataToCache
    private static ItemStack deserializeItem(String base64) {
        if (base64 == null || base64.isEmpty()) return new ItemStack(Material.AIR);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack(Material.AIR);
        }
    }

    // ────────────────────────────────────────────────
    //                  STATIC CACHE METHODS
    // ────────────────────────────────────────────────

    public static Map<Integer, ItemStack> getCachedJewelry(UUID uuid) {
        return jewelryCache.getOrDefault(uuid, new HashMap<>());
    }

    public static void updateJewelryCache(UUID uuid, Map<Integer, ItemStack> items) {
        jewelryCache.put(uuid, new HashMap<>(items));
    }

    public static void invalidateJewelryCache(UUID uuid) {
        jewelryCache.remove(uuid);
    }

    // ────────────────────────────────────────────────
    //                  TÍNH STATS JEWELRY (gọi từ CacheListener)
    // ────────────────────────────────────────────────

    public static void addJewelryStatsToCache(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Integer, ItemStack> cachedItems = getCachedJewelry(uuid);
        if (cachedItems.isEmpty()) return;

        PlayerCombatCache.CombatStats stats = PlayerCombatCache.getStats(uuid);
        PlayerCombatCache.CombatStats jewelryOnly = new PlayerCombatCache.CombatStats();

        var slotConfig = Main.getInstance().getStatsSettingsConfig();
        var gemConfig = Main.getInstance().getGemConfig();
        var eConfig = Main.getInstance().getElementConfig();

        for (ItemStack item : cachedItems.values()) {
            if (item == null || item.getType().isAir()) continue;

            // Stats cơ bản
            jewelryOnly.totalBonusDmg += Damage.getDamage(item);
            jewelryOnly.totalPveBonus += PveDamage.get(item);
            jewelryOnly.totalPvpBonus += PvpDamage.get(item);
            jewelryOnly.totalCritChance += CriticalChance.get(item);
            jewelryOnly.totalCritDamage += CriticalDamage.get(item);
            jewelryOnly.totalLifesteal += Lifesteal.get(item);
            jewelryOnly.totalTrueDamage += TrueDamage.get(item);
            jewelryOnly.totalPenetration += Penetration.get(item);
            jewelryOnly.totalArmorPen += ArmorPen.get(item);
            jewelryOnly.totalArmor += Armor.getArmor(item);
            jewelryOnly.totalPveDef += PveDefense.get(item);
            jewelryOnly.totalPvpDef += PvpDefense.get(item);
            jewelryOnly.totalDodge += DodgeRate.get(item);
            jewelryOnly.totalBlock += BlockRate.get(item);
            jewelryOnly.totalThorns += Thorns.get(item);

            // Element
            Map<String, Integer> elements = ElementCore.getAllElements(item);
            for (Map.Entry<String, Integer> entry : elements.entrySet()) {
                String eid = entry.getKey().toUpperCase();
                int lv = entry.getValue();
                double base = eConfig.getDouble(eid + ".base-damage", 0.0);
                double per = eConfig.getDouble(eid + ".damage-per", 0.0);
                jewelryOnly.totalElementDamage += base + (lv * per);
            }

            // Gem
            List<String> gemIds = GemLogic.getGemsOnItem(item);
            for (String gemId : gemIds) {
                if (gemConfig.contains(gemId + ".apply.stats")) {
                    for (String line : gemConfig.getStringList(gemId + ".apply.stats")) {
                        String[] p = line.split(":");
                        if (p.length < 2) continue;
                        String type = p[0].trim().toLowerCase();
                        double val = Double.parseDouble(p[1].trim());
                        updateStat(jewelryOnly, type, val); // Reuse hàm updateStat từ CacheListener
                    }
                }
                // Thêm phần element và ability từ gem nếu cần (copy từ CacheListener)
            }

            // Ability
            for (String data : AbilityData.getAbilityList(item)) {
                processAbilityLineLegacy(data, jewelryOnly.bestAbilities); // Reuse hàm từ CacheListener
            }
        }

        // Cộng jewelry vào stats chính
        stats.totalBonusDmg += jewelryOnly.totalBonusDmg;
        // ... (cộng tất cả stats khác tương tự)
        stats.totalPveBonus += jewelryOnly.totalPveBonus;
        stats.totalPvpBonus += jewelryOnly.totalPvpBonus;
        stats.totalCritChance += jewelryOnly.totalCritChance;
        stats.totalCritDamage += jewelryOnly.totalCritDamage;
        stats.totalLifesteal += jewelryOnly.totalLifesteal;
        stats.totalTrueDamage += jewelryOnly.totalTrueDamage;
        stats.totalPenetration += jewelryOnly.totalPenetration;
        stats.totalArmorPen += jewelryOnly.totalArmorPen;
        stats.totalArmor += jewelryOnly.totalArmor;
        stats.totalPveDef += jewelryOnly.totalPveDef;
        stats.totalPvpDef += jewelryOnly.totalPvpDef;
        stats.totalDodge += jewelryOnly.totalDodge;
        stats.totalBlock += jewelryOnly.totalBlock;
        stats.totalThorns += jewelryOnly.totalThorns;
        stats.totalElementDamage += jewelryOnly.totalElementDamage;

        // Ability: giữ cái tốt nhất
        for (Map.Entry<String, double[]> entry : jewelryOnly.bestAbilities.entrySet()) {
            String name = entry.getKey();
            double[] val = entry.getValue();
            double[] current = stats.bestAbilities.get(name);
            if (current == null || val[0] > current[0]) {
                stats.bestAbilities.put(name, val);
            }
        }

        PlayerCombatCache.updateCache(uuid, stats);
    }

    // Reuse hàm updateStat và processAbilityLineLegacy từ CacheListener (copy vào đây nếu cần)
    private static void updateStat(PlayerCombatCache.CombatStats stats, String type, double val) {
        switch (type.toLowerCase()) {
            case "damage" -> stats.totalBonusDmg += val;
            case "pve_damage" -> stats.totalPveBonus += val;
            case "pvp_damage" -> stats.totalPvpBonus += val;
            case "critical_chance" -> stats.totalCritChance += val;
            case "critical_damage" -> stats.totalCritDamage += val;
            case "lifesteal" -> stats.totalLifesteal += val;
            case "true_damage" -> stats.totalTrueDamage += val;
            case "penetration" -> stats.totalPenetration += val;
            case "armor_pen" -> stats.totalArmorPen += val;
            case "armor" -> stats.totalArmor += val;
            case "pve_defense" -> stats.totalPveDef += val;
            case "pvp_defense" -> stats.totalPvpDef += val;
            case "dodge_rate" -> stats.totalDodge += val;
            case "block_rate" -> stats.totalBlock += val;
            case "thorns" -> stats.totalThorns += val;
        }
    }

    private static void processAbilityLineLegacy(String data, Map<String, double[]> best) {
        String[] parts = data.split(":");
        if (parts.length < 3) return;
        try {
            String name = parts[0].toUpperCase();
            int lv = Integer.parseInt(parts[1]);
            double ch = Double.parseDouble(parts[2]);
            double[] curr = best.get(name);
            if (curr == null || lv > (int) curr[0]) {
                best.put(name, new double[]{lv, ch});
            }
        } catch (Exception ignored) {}
    }

    private static class JewelrySlot {
        final String key;
        final String name;
        final int slot;

        JewelrySlot(String key, String name, int slot) {
            this.key = key;
            this.name = name;
            this.slot = slot;
        }
    }

    // ────────────────────────────────────────────────
    //                  LỆNH GÁN TYPE (PDC)
    // ────────────────────────────────────────────────

    public static boolean setJewelryType(Player player, String id) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§cBạn cần cầm vật phẩm trên tay!");
            return false;
        }

        // Trường hợp UNTYPE: Xóa dữ liệu cũ
        if (id == null) {
            item.editMeta(meta -> {
                meta.getPersistentDataContainer().remove(JEWELRY_TYPE_KEY);
            });
            player.sendMessage("§aĐã xóa loại trang sức khỏi vật phẩm này!");
            return true;
        }

        FileConfiguration config = Main.getInstance().getConfig();
        boolean isValid = false;

        // 1. Kiểm tra trong player-slots (Cấu hình mới của Thiên)
        ConfigurationSection playerSlotsSec = config.getConfigurationSection("jewelry.player-slots");
        if (playerSlotsSec != null) {
            for (String key : playerSlotsSec.getKeys(false)) {
                String typeInConfig = playerSlotsSec.getString(key + ".type");
                if (id.equalsIgnoreCase(typeInConfig)) {
                    isValid = true;
                    break;
                }
            }
        }

        // 2. Kiểm tra thêm trong jewelry.slots (Để tương thích với GUI cũ nếu cần)
        if (!isValid) {
            ConfigurationSection oldSlotsSec = config.getConfigurationSection("jewelry.slots");
            if (oldSlotsSec != null && oldSlotsSec.contains(id)) {
                isValid = true;
            }
        }

        // Nếu không tìm thấy loại này trong bất kỳ cấu hình nào
        if (!isValid) {
            player.sendMessage("§cID loại trang sức không tồn tại: §e" + id);
            return false;
        }

        // Gán dữ liệu (PDC) vào item
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(JEWELRY_TYPE_KEY, PersistentDataType.STRING, id.toLowerCase());
        });

        player.sendMessage("§aĐã gán loại §e" + id + " §achơ vật phẩm trên tay!");
        return true;
    }

    public static void loadDataToCache(Player player) {
        UUID uuid = player.getUniqueId();
        if (jewelryCache.containsKey(uuid)) return;

        // Phải tạo instance tạm để load danh sách slot từ config
        JewelryManager temp = new JewelryManager(player);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Main.getInstance().getDataFolder() + "/" + DB_FILE);
             PreparedStatement ps = conn.prepareStatement("SELECT slot_key, item_data FROM jewelry WHERE uuid = ?")) {

            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            Map<Integer, ItemStack> loaded = new HashMap<>();

            while (rs.next()) {
                String key = rs.getString("slot_key");
                String data = rs.getString("item_data");
                ItemStack item = deserializeItem(data);

                temp.jewelrySlots.stream()
                        .filter(s -> s.key.equals(key))
                        .forEach(js -> loaded.put(js.slot, item));
            }
            jewelryCache.put(uuid, loaded);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    public ItemStack createInventoryPlaceholder(int slotIndex) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection pSlots = config.getConfigurationSection("jewelry.player-slots." + slotIndex);

        // Tạo item từ config (ví dụ dùng Paper hoặc Barrier)
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && pSlots != null) {
            meta.setDisplayName(pSlots.getString("name", "&7Slot Trang Sức").replace("&", "§"));
            meta.setCustomModelData(pSlots.getInt("model-data", 0));

            // QUAN TRỌNG: Đánh dấu đây là Placeholder để CacheListener không cộng stats
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_placeholder"), PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
        }
        return item;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInventoryInteract(InventoryClickEvent e) {
        FileConfiguration config = plugin.getConfig();
        // KIỂM TRA CÔNG TẮC: Nếu tắt thì không làm gì cả
        if (!config.getBoolean("jewelry.enable-player-inventory", false)) return;

        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null || clickedInv.getType() != InventoryType.PLAYER) return;

        int slot = e.getSlot();
        ConfigurationSection slotsSec = config.getConfigurationSection("jewelry.player-slots");
        if (slotsSec == null || !slotsSec.contains(String.valueOf(slot))) return;

        // --- BẮT ĐẦU XỬ LÝ SLOT TRANG SỨC TRONG TÚI ĐỒ ---
        e.setCancelled(true); // Chặn cầm kéo bình thường

        Player p = (Player) e.getWhoClicked();
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        String requiredType = slotsSec.getString(slot + ".type");

        // 1. Đặt đồ vào
        if (cursor != null && !cursor.getType().isAir()) {
            // Kiểm tra loại trang sức (NBT jewelry_type)
            String itemType = (cursor.getItemMeta() != null) ?
                    cursor.getItemMeta().getPersistentDataContainer().get(JEWELRY_TYPE_KEY, PersistentDataType.STRING) : null;

            if (itemType != null && itemType.equalsIgnoreCase(requiredType)) {
                // Nếu slot đang có item thật, trả item đó về Cursor
                ItemStack toCursor = (current != null && !isPlaceholder(current)) ? current.clone() : new ItemStack(Material.AIR);

                clickedInv.setItem(slot, cursor.clone());
                e.getView().setCursor(toCursor);

                // Cập nhật stats ngay
                org.ThienNguyen.Listener.CacheListener.refreshCache(p);
            } else {
                p.sendMessage("§cÔ này chỉ dành cho: §e" + requiredType);
            }
            return;
        }

        // 2. Lấy đồ ra
        if (current != null && !current.getType().isAir() && !isPlaceholder(current)) {
            e.getView().setCursor(current.clone());
            // Đặt lại Placeholder sau khi lấy đồ ra
            clickedInv.setItem(slot, createInventoryPlaceholder(slot));
            org.ThienNguyen.Listener.CacheListener.refreshCache(p);
        }
    }
    private boolean isPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "is_placeholder"), PersistentDataType.BOOLEAN);
    }
    @EventHandler
    public void onJoinFillSlots(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("jewelry.enable-player-inventory", false)) return;

        ConfigurationSection pSlots = config.getConfigurationSection("jewelry.player-slots");
        if (pSlots == null) return;

        for (String key : pSlots.getKeys(false)) {
            int slotIdx = Integer.parseInt(key);
            ItemStack item = p.getInventory().getItem(slotIdx);

            // Nếu ô đó trống trơn, điền placeholder vào
            if (item == null || item.getType() == Material.AIR) {
                p.getInventory().setItem(slotIdx, createInventoryPlaceholder(slotIdx));
            }
        }
    }
}