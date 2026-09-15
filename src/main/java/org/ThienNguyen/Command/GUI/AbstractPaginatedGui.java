package org.ThienNguyen.Command.GUI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lớp cha DÙNG CHUNG cho MỌI gui dạng "danh sách có phân trang" trong plugin
 * (GUIStats hiện tại, và sau này GUIElement, GUIAbility, GUIEffect, ...).
 *
 * Lớp con CHỈ cần:
 *   1. extends AbstractPaginatedGui<T>  (T là kiểu dữ liệu của 1 mục trong danh sách,
 *      VD String cho GUIStats, hoặc 1 class ElementInfo/AbilityInfo cho sau này)
 *   2. Gọi super(TITLE) trong constructor
 *   3. Implement 3 hàm bắt buộc: loadEntries(), renderEntry(), onEntryClick()
 *   4. Đăng ký với GUIListener.registerHandler(TITLE, INSTANCE) như bình thường
 *      (việc này KHÔNG thể tự động hoá trong lớp cha vì mỗi GUI có tiêu đề/instance
 *      riêng, nên vẫn cần 1 dòng static block ở lớp con).
 *
 * Lớp cha lo sẵn TOÀN BỘ phần "khung":
 *   - Layout cố định 54 ô, chia 3 vùng:
 *       + Header (slot 0-8)   : tuỳ biến qua renderHeader()/onHeaderClick(), mặc định viền trống.
 *       + Content (slot 9-44) : 36 ô/trang, tự động phân trang khi danh sách vượt quá 36 mục.
 *       + Footer (slot 45-53) : nút Trang Trước / số trang / Trang Sau, tự vẽ, không cần lớp con đụng vào.
 *   - Tự nhớ trang hiện tại của từng người chơi (không cần lớp con tự quản lý UUID -> page).
 *   - Tự map lại đúng slot click -> đúng "entry" trong danh sách, kể cả khi đang ở trang 2, 3, ...
 *   - Việc ngăn lấy/thả item ra khỏi GUI đã được xử lý tập trung ở GUIListener
 *     (setCancelled + chặn cả InventoryDragEvent), lớp con không cần lo phần này.
 */
public abstract class AbstractPaginatedGui<T> implements IGuiHandler {

    protected static final int INVENTORY_SIZE = 54;

    /** Slot đầu tiên của vùng nội dung (sau header). */
    protected static final int CONTENT_START = 9;
    /** Slot đầu tiên của vùng footer (kết thúc vùng nội dung, không bao gồm). */
    protected static final int FOOTER_START = 45;
    /** Số ô nội dung tối đa mỗi trang (4 hàng x 9 = 36). */
    protected static final int ENTRIES_PER_PAGE = FOOTER_START - CONTENT_START;

    private static final int PREV_PAGE_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private final String title;

    /** Trang hiện tại của mỗi người chơi, để họ mở lại đúng chỗ đang xem / bấm nút chuyển trang. */
    private final Map<UUID, Integer> pageByPlayer = new HashMap<>();

    protected AbstractPaginatedGui(String title) {
        this.title = title;
    }

    public final String getTitle() {
        return title;
    }

    // ================== API CHO LỚP CON GỌI ==================

    /**
     * Mở GUI cho người chơi ở đúng trang họ đang xem lần gần nhất (hoặc trang 0 nếu chưa mở bao giờ).
     */
    public final void open(Player player) {
        openPage(player, pageByPlayer.getOrDefault(player.getUniqueId(), 0));
    }

    /**
     * Mở GUI cho người chơi ở 1 trang cụ thể (trang được kẹp về khoảng hợp lệ nếu vượt quá).
     */
    public final void openPage(Player player, int page) {
        List<T> entries = loadEntries(player);
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) ENTRIES_PER_PAGE));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        pageByPlayer.put(player.getUniqueId(), clampedPage);

        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, title);
        renderHeader(inv, player);
        renderContent(inv, player, entries, clampedPage);
        renderFooter(inv, clampedPage, totalPages);
        player.openInventory(inv);
    }

    // ================== HÀM BẮT BUỘC LỚP CON PHẢI IMPLEMENT ==================

    /**
     * Trả về TOÀN BỘ danh sách mục sẽ hiển thị (chưa cắt trang), theo đúng thứ tự mong muốn.
     * Được gọi lại mỗi lần mở/chuyển trang/click, nên có thể phụ thuộc vào trạng thái hiện tại
     * của player (VD: vật phẩm đang cầm) mà không sợ dữ liệu cũ.
     */
    protected abstract List<T> loadEntries(Player player);

    /**
     * Vẽ ItemStack đại diện cho 1 "entry" trong danh sách để đặt vào ô nội dung.
     */
    protected abstract ItemStack renderEntry(Player player, T entry);

    /**
     * Được gọi khi người chơi click vào 1 ô nội dung (đã xác định đúng entry tương ứng).
     * Sự kiện đã được setCancelled(true) sẵn từ GUIListener.
     */
    protected abstract void onEntryClick(Player player, T entry, InventoryClickEvent event);

    // ================== HOOK TUỲ CHỌN LỚP CON CÓ THỂ GHI ĐÈ ==================

    /** Mặc định: phủ viền trống toàn bộ header (slot 0-8). Ghi đè để hiển thị thêm (VD: vật phẩm đang cầm). */
    protected void renderHeader(Inventory inv, Player player) {
        ItemStack filler = getFillerItem();
        for (int slot = 0; slot < CONTENT_START; slot++) {
            inv.setItem(slot, filler);
        }
    }

    /** Mặc định: không làm gì khi click vào header. Ghi đè nếu header có nút tương tác riêng. */
    protected void onHeaderClick(Player player, int slot, InventoryClickEvent event) {
        // no-op mặc định
    }

    /** Vật phẩm dùng để lấp các ô trống/viền. Ghi đè nếu muốn đổi giao diện. */
    protected ItemStack getFillerItem() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        return filler;
    }

    /**
     * Được gọi khi người chơi đóng GUI (qua GUIListener). Mặc định giữ nguyên trạng thái trang
     * để lần sau mở lại đúng chỗ cũ. Ghi đè nếu muốn dọn dẹp gì thêm.
     */
    @Override
    public void onClose(Player player) {
        // giữ nguyên pageByPlayer để mở lại đúng trang cũ; ghi đè nếu cần khác
    }

    // ================== PHẦN NỘI BỘ: VẼ NỘI DUNG + FOOTER, XỬ LÝ CLICK ==================

    private void renderContent(Inventory inv, Player player, List<T> entries, int page) {
        int startIndex = page * ENTRIES_PER_PAGE;
        ItemStack filler = getFillerItem();
        for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
            int slot = CONTENT_START + i;
            int entryIndex = startIndex + i;
            if (entryIndex < entries.size()) {
                inv.setItem(slot, renderEntry(player, entries.get(entryIndex)));
            } else {
                inv.setItem(slot, filler);
            }
        }
    }

    private void renderFooter(Inventory inv, int page, int totalPages) {
        ItemStack filler = getFillerItem();
        for (int slot = FOOTER_START; slot < INVENTORY_SIZE; slot++) {
            inv.setItem(slot, filler);
        }
        if (page > 0) {
            inv.setItem(PREV_PAGE_SLOT, navItem(Material.ARROW,
                    ChatColor.YELLOW + "« Trang Trước", "Xem trang " + page));
        }
        inv.setItem(PAGE_INFO_SLOT, navItem(Material.PAPER,
                ChatColor.GOLD + "Trang " + (page + 1) + "/" + totalPages, null));
        if (page < totalPages - 1) {
            inv.setItem(NEXT_PAGE_SLOT, navItem(Material.ARROW,
                    ChatColor.YELLOW + "Trang Sau »", "Xem trang " + (page + 2)));
        }
    }

    private static ItemStack navItem(Material material, String name, String desc) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (desc != null) {
                meta.setLore(List.of(ChatColor.GRAY + desc));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Điều phối click theo vùng (header / content / footer). final vì đây là "khung" dùng chung,
     * lớp con không nên ghi đè trực tiếp — hãy dùng onHeaderClick()/onEntryClick() thay vào đó.
     */
    @Override
    public final void onClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= INVENTORY_SIZE) return; // click ở inventory của người chơi, không phải GUI này

        if (slot < CONTENT_START) {
            onHeaderClick(player, slot, event);
            return;
        }
        if (slot < FOOTER_START) {
            handleContentClick(player, slot, event);
            return;
        }
        handleFooterClick(player, slot, event);
    }

    private void handleContentClick(Player player, int slot, InventoryClickEvent event) {
        int page = pageByPlayer.getOrDefault(player.getUniqueId(), 0);
        List<T> entries = loadEntries(player);
        int indexInPage = slot - CONTENT_START;
        int entryIndex = page * ENTRIES_PER_PAGE + indexInPage;
        if (entryIndex < 0 || entryIndex >= entries.size()) return; // ô trống cuối trang
        onEntryClick(player, entries.get(entryIndex), event);
    }

    private void handleFooterClick(Player player, int slot, InventoryClickEvent event) {
        int page = pageByPlayer.getOrDefault(player.getUniqueId(), 0);
        if (slot == PREV_PAGE_SLOT) {
            openPage(player, page - 1);
        } else if (slot == NEXT_PAGE_SLOT) {
            openPage(player, page + 1);
        }
        // các slot footer khác (viền/số trang) không làm gì khi click
    }
}