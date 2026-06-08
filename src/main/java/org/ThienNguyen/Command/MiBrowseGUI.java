package org.ThienNguyen.Command;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MiBrowseGUI implements Listener {

    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int PAGE_SIZE = CONTENT_SLOTS.length;

    private static final int SLOT_PREV  = 45;
    private static final int SLOT_BACK  = 47;
    private static final int SLOT_TITLE = 49;
    private static final int SLOT_NEXT  = 53;

    private static final Map<String, Material> TYPE_ICON_MAP = new HashMap<>() {{
        put("sword",      Material.DIAMOND_SWORD);
        put("bow",        Material.BOW);
        put("armor",      Material.DIAMOND_CHESTPLATE);
        put("helmet",     Material.DIAMOND_HELMET);
        put("chestplate", Material.DIAMOND_CHESTPLATE);
        put("leggings",   Material.DIAMOND_LEGGINGS);
        put("boots",      Material.DIAMOND_BOOTS);
        put("axe",        Material.DIAMOND_AXE);
        put("pickaxe",    Material.DIAMOND_PICKAXE);
        put("shovel",     Material.DIAMOND_SHOVEL);
        put("hoe",        Material.DIAMOND_HOE);
        put("staff",      Material.STICK);
        put("wand",       Material.BLAZE_ROD);
        put("shield",     Material.SHIELD);
        put("gem",        Material.EMERALD);
        put("potion",     Material.POTION);
        put("food",       Material.COOKED_BEEF);
        put("misc",       Material.CHEST);
        put("special",    Material.NETHER_STAR);
        put("amulet",     Material.GOLD_NUGGET);
    }};

    private static final Map<UUID, BrowseState> playerState = new HashMap<>();

    private final Main plugin;

    public MiBrowseGUI(Main plugin) {
        this.plugin = plugin;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Mở GUI
    // ════════════════════════════════════════════════════════════════════════

    public void openTypePage(Player player, int page) {
        ItemStorageManager ism = plugin.getItemStorageManager();
        List<String> types = ism.getTypeNames();
        types.sort(String.CASE_INSENSITIVE_ORDER);

        int maxPage = Math.max(1, (int) Math.ceil((double) types.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, maxPage - 1));

        Inventory inv = Bukkit.createInventory(new BrowseHolder(), 54,
                ChatColor.DARK_AQUA + "✦ " + ChatColor.WHITE + "ManagerItem" +
                        ChatColor.GRAY + "  [" + (page + 1) + "/" + maxPage + "]");

        fillBorder(inv);
        fillTitleSlot(inv, ChatColor.AQUA + "📁 Chọn Type", Material.BOOK,
                Arrays.asList(
                        ChatColor.GRAY + "Tổng: " + ChatColor.WHITE + types.size() + " type",
                        "",
                        ChatColor.YELLOW + "Click vào type để xem item"
                ));

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < types.size(); i++) {
            String typeName = types.get(start + i);
            inv.setItem(CONTENT_SLOTS[i], buildTypeIcon(typeName, ism.getIdsByType(typeName).size()));
        }

        if (page > 0)
            inv.setItem(SLOT_PREV, buildNav(Material.ARROW, ChatColor.YELLOW + "◀ Trang trước", ""));
        if (page < maxPage - 1)
            inv.setItem(SLOT_NEXT, buildNav(Material.ARROW, ChatColor.YELLOW + "Trang sau ▶", ""));

        // Cập nhật state TRƯỚC khi mở inventory
        playerState.put(player.getUniqueId(), new BrowseState(BrowseMode.TYPE, null, page));
        player.openInventory(inv);
    }

    public void openItemPage(Player player, String type, int page) {
        ItemStorageManager ism = plugin.getItemStorageManager();
        List<String> ids = ism.getIdsByType(type);
        ids.sort(String.CASE_INSENSITIVE_ORDER);

        int maxPage = Math.max(1, (int) Math.ceil((double) ids.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, maxPage - 1));

        Inventory inv = Bukkit.createInventory(new BrowseHolder(), 54,
                ChatColor.DARK_AQUA + "✦ " + ChatColor.WHITE + type +
                        ChatColor.GRAY + "  [" + (page + 1) + "/" + maxPage + "]");

        fillBorder(inv);
        fillTitleSlot(inv, ChatColor.GOLD + "📦 " + type, Material.CHEST,
                Arrays.asList(
                        ChatColor.GRAY + "Tổng: " + ChatColor.WHITE + ids.size() + " item",
                        ChatColor.GRAY + "Trang: " + ChatColor.WHITE + (page + 1) + "/" + maxPage,
                        "",
                        ChatColor.YELLOW + "Click trái " + ChatColor.GRAY + "→ lấy item"
                ));

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < ids.size(); i++) {
            String id = ids.get(start + i);
            ItemStack real = ism.getItem(id);
            if (real == null) continue;

            ItemStack display = real.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + id);
                lore.add(ChatColor.YELLOW + "▶ Click trái để lấy");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(CONTENT_SLOTS[i], display);
        }

        inv.setItem(SLOT_BACK, buildNav(Material.BARRIER, ChatColor.RED + "◀ Quay lại", ChatColor.GRAY + "Về trang chọn Type"));
        if (page > 0)
            inv.setItem(SLOT_PREV, buildNav(Material.ARROW, ChatColor.YELLOW + "◀ Trang trước", ""));
        if (page < maxPage - 1)
            inv.setItem(SLOT_NEXT, buildNav(Material.ARROW, ChatColor.YELLOW + "Trang sau ▶", ""));

        // Cập nhật state TRƯỚC khi mở inventory
        playerState.put(player.getUniqueId(), new BrowseState(BrowseMode.ITEM, type, page));
        player.openInventory(inv);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Events
    // ════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof BrowseHolder)) return;

        e.setCancelled(true);

        BrowseState state = playerState.get(player.getUniqueId());
        if (state == null) return;

        int slot = e.getRawSlot();
        // slot >= 54 là inventory của player bên dưới → cancel nhưng không xử lý
        if (slot < 0 || slot >= 54) return;

        if (state.mode == BrowseMode.TYPE) {
            handleTypeClick(player, state, slot);
        } else {
            handleItemClick(player, state, slot);
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getInventory().getHolder() instanceof BrowseHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // Delay 1 tick: nếu player đang mở GUI mới (back/prev/next/give)
        // thì state đã được ghi đè rồi, không cần xóa.
        // Nếu đóng thật (nhấn ESC) thì sau 1 tick không có GUI BrowseHolder nào → xóa state.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                playerState.remove(uuid);
                return;
            }
            // Nếu GUI hiện tại không phải BrowseHolder → player đã thoát thật → xóa state
            if (!(p.getOpenInventory().getTopInventory().getHolder() instanceof BrowseHolder)) {
                playerState.remove(uuid);
            }
            // Nếu vẫn là BrowseHolder → player đang ở GUI mới → giữ state
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Handlers
    // ════════════════════════════════════════════════════════════════════════

    private void handleTypeClick(Player player, BrowseState state, int slot) {
        if (slot == SLOT_PREV && state.page > 0) {
            openTypePage(player, state.page - 1);
            return;
        }
        if (slot == SLOT_NEXT) {
            openTypePage(player, state.page + 1);
            return;
        }

        int contentIndex = getContentIndex(slot);
        if (contentIndex < 0) return;

        ItemStorageManager ism = plugin.getItemStorageManager();
        List<String> types = ism.getTypeNames();
        types.sort(String.CASE_INSENSITIVE_ORDER);
        int globalIndex = state.page * PAGE_SIZE + contentIndex;
        if (globalIndex >= types.size()) return;

        openItemPage(player, types.get(globalIndex), 0);
    }

    private void handleItemClick(Player player, BrowseState state, int slot) {
        if (slot == SLOT_BACK) {
            openTypePage(player, 0);
            return;
        }
        if (slot == SLOT_PREV && state.page > 0) {
            openItemPage(player, state.currentType, state.page - 1);
            return;
        }
        if (slot == SLOT_NEXT) {
            openItemPage(player, state.currentType, state.page + 1);
            return;
        }

        // Click vào item → give
        int contentIndex = getContentIndex(slot);
        if (contentIndex < 0) return;

        ItemStorageManager ism = plugin.getItemStorageManager();
        List<String> ids = ism.getIdsByType(state.currentType);
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        int globalIndex = state.page * PAGE_SIZE + contentIndex;
        if (globalIndex >= ids.size()) return;

        String id = ids.get(globalIndex);
        ItemStack item = ism.getItem(id);
        if (item == null) {
            player.sendMessage(ChatColor.RED + "[MyItem] Không tìm thấy item: " + id);
            return;
        }

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            overflow.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════

    private void fillBorder(Inventory inv) {
        ItemStack glass = makeGlass();
        for (int i = 0;  i < 9;  i++) inv.setItem(i, glass);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9,     glass);
            inv.setItem(row * 9 + 8, glass);
        }
    }

    private ItemStack makeGlass() {
        ItemStack g = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = g.getItemMeta();
        if (m != null) { m.setDisplayName(" "); g.setItemMeta(m); }
        return g;
    }

    private void fillTitleSlot(Inventory inv, String name, Material mat, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) { m.setDisplayName(name); m.setLore(lore); item.setItemMeta(m); }
        inv.setItem(SLOT_TITLE, item);
    }

    private ItemStack buildNav(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (!lore.isEmpty()) m.setLore(Collections.singletonList(lore));
            item.setItemMeta(m);
        }
        return item;
    }

    private ItemStack buildTypeIcon(String typeName, int itemCount) {
        String key = typeName.toLowerCase();
        Material mat = TYPE_ICON_MAP.getOrDefault(key, Material.BOOKSHELF);
        if (mat == Material.BOOKSHELF) {
            for (Map.Entry<String, Material> entry : TYPE_ICON_MAP.entrySet()) {
                if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                    mat = entry.getValue();
                    break;
                }
            }
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "📂 " + typeName);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "File: " + ChatColor.WHITE + typeName + ".yml",
                    ChatColor.GRAY + "Số item: " + ChatColor.YELLOW + itemCount,
                    "",
                    ChatColor.GREEN + "▶ Click để xem item"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private int getContentIndex(int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++)
            if (CONTENT_SLOTS[i] == slot) return i;
        return -1;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Inner classes
    // ════════════════════════════════════════════════════════════════════════

    private enum BrowseMode { TYPE, ITEM }

    private static class BrowseState {
        final BrowseMode mode;
        final String currentType;
        final int page;
        BrowseState(BrowseMode mode, String currentType, int page) {
            this.mode = mode; this.currentType = currentType; this.page = page;
        }
    }

    private static class BrowseHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}