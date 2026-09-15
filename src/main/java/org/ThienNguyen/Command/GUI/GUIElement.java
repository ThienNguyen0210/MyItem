package org.ThienNguyen.Command.GUI;

import org.ThienNguyen.Element.ElementCore;
import org.ThienNguyen.Main;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI liệt kê toàn bộ nguyên tố (element) do người dùng tự định nghĩa trong file cấu hình
 * ({@link Main#getElementConfig()}), để gắn chỉ số TẤN CÔNG / PHÒNG THỦ nguyên tố lên vật
 * phẩm đang cầm ở tay chính, hoặc xoá hẳn 1 nguyên tố khỏi vật phẩm.
 *
 * Khác với {@link GUIAbility} hay {@link GUIEffect} (danh sách cố định trong code), danh
 * sách nguyên tố ở đây được đọc TRỰC TIẾP từ config — mỗi khoá cấp cao nhất trong
 * elements.yml (VD "particle", "density", "color"/"material" là các thuộc tính con) tương
 * ứng với 1 nguyên tố. Vì vậy GUI này không có danh sách hard-code như ABILITY_INFO /
 * EFFECT_INFO, mà load lại từ config mỗi lần mở.
 *
 * Lớp này KHÔNG tự implements Listener và KHÔNG cần registerEvents() riêng — mọi sự kiện
 * (click GUI, nhập giá trị qua chat) đều đi qua GUIListener dùng chung. Chỉ cần đảm bảo
 * trong onEnable() của plugin chính đã gọi:
 *   GUIListener.init(this);
 *
 * ĐIỀU KHIỂN:
 *   - Click TRÁI vào 1 nguyên tố: mở khung chat để nhập cấp độ TẤN CÔNG, theo định dạng
 *     "<cấp độ>", VD "3" = cấp 3. Nhập 0 để gỡ riêng chỉ số tấn công.
 *   - Click PHẢI vào 1 nguyên tố: mở khung chat để nhập cấp độ PHÒNG THỦ, cùng định dạng.
 *     Nhập 0 để gỡ riêng chỉ số phòng thủ.
 *   - Shift + Click (trái hoặc phải) vào 1 nguyên tố: xoá HẲN nguyên tố đó (cả tấn công lẫn
 *     phòng thủ) khỏi vật phẩm đang cầm NGAY LẬP TỨC, không cần nhập chat.
 *
 * Dữ liệu được đọc/ghi qua {@link ElementCore}, dùng các hàm "set...Level" (ghi đè tuyệt
 * đối) thay vì "add..." (cộng dồn) để phù hợp với thao tác chỉnh sửa qua GUI.
 *
 * Cách dùng trong lệnh của bạn:
 *   if (args.length < 1) {
 *       GUIElement.openGuiElement(player);
 *       return true;
 *   }
 */
public class GUIElement extends AbstractPaginatedGui<String> {

    private static final String GUI_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Elements";
    private static final String CANCEL_KEYWORD = "cancel";
    private static final int SLOT_HELD_ITEM = 4;

    private static final GUIElement INSTANCE = new GUIElement();
    static {
        GUIListener.registerHandler(GUI_TITLE, INSTANCE);
    }

    private GUIElement() {
        super(GUI_TITLE);
    }

    /** Mở GUI ở đúng trang người chơi đang xem lần gần nhất. */
    public static void openGuiElement(Player player) {
        INSTANCE.open(player);
    }

    // ================== IMPLEMENT KHUNG CỦA AbstractPaginatedGui ==================

    /**
     * Danh sách nguyên tố = các khoá cấp cao nhất trong file cấu hình nguyên tố.
     * Nếu config chưa được tải hoặc rỗng, trả về danh sách rỗng (GUI sẽ hiện trống).
     */
    @Override
    protected List<String> loadEntries(Player player) {
        FileConfiguration config = Main.getInstance().getElementConfig();
        if (config == null) return new ArrayList<>();
        List<String> ids = new ArrayList<>(config.getKeys(false));
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    @Override
    protected void renderHeader(Inventory inv, Player player) {
        super.renderHeader(inv, player); // phủ viền mặc định trước, rồi ghi đè slot hiển thị item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean hasItem = heldItem != null && heldItem.getType() != Material.AIR;
        if (!hasItem) {
            ItemStack placeholder = new ItemStack(Material.BARRIER);
            ItemMeta meta = placeholder.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Chưa cầm vật phẩm nào");
                meta.setLore(List.of(
                        ChatColor.GRAY + "Hãy cầm 1 vật phẩm ở tay chính",
                        ChatColor.GRAY + "trước khi chỉnh sửa nguyên tố."
                ));
                placeholder.setItemMeta(meta);
            }
            inv.setItem(SLOT_HELD_ITEM, placeholder);
        } else {
            inv.setItem(SLOT_HELD_ITEM, heldItem.clone());
        }
    }

    @Override
    protected ItemStack renderEntry(Player player, String elementId) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean hasItem = heldItem != null && heldItem.getType() != Material.AIR;
        return buildElementItem(hasItem ? heldItem : null, elementId);
    }

    @Override
    protected void onEntryClick(Player player, String elementId, InventoryClickEvent event) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Bạn cần cầm 1 vật phẩm ở tay chính trước khi chỉnh sửa nguyên tố!");
            return;
        }

        // Shift + Click (trái hoặc phải): xoá hẳn nguyên tố khỏi vật phẩm, không cần nhập chat.
        if (event.isShiftClick()) {
            ElementCore.removeElement(heldItem, elementId);
            org.ThienNguyen.Lore.ElementLore.updateLore(heldItem);
            player.sendMessage(ChatColor.RED + "Đã xoá hẳn nguyên tố " + ChatColor.AQUA + elementId
                    + ChatColor.RED + " khỏi vật phẩm đang cầm.");
            INSTANCE.open(player);
            return;
        }

        boolean editDefense = event.isRightClick();

        // Click thường (trái = tấn công, phải = phòng thủ): mở khung chat để nhập "<cấp độ>".
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "» Nhập " + ChatColor.WHITE
                + (editDefense ? "cấp độ phòng thủ" : "cấp độ tấn công")
                + ChatColor.GREEN + " cho nguyên tố " + ChatColor.AQUA + elementId
                + ChatColor.GREEN + " vào khung chat, theo định dạng:");
        player.sendMessage(ChatColor.YELLOW + "<cấp độ>" + ChatColor.GRAY + "   (VD: "
                + ChatColor.WHITE + "3" + ChatColor.GRAY + " = cấp 3, nhập "
                + ChatColor.WHITE + "0" + ChatColor.GRAY + " để gỡ riêng chỉ số này)");
        player.sendMessage(ChatColor.GRAY + "Gõ " + ChatColor.RED + CANCEL_KEYWORD
                + ChatColor.GRAY + " để huỷ.");
        GUIListener.requestChatInput(player, CANCEL_KEYWORD,
                message -> handleElementInput(player, elementId, editDefense, message),
                () -> {
                    player.sendMessage(ChatColor.RED + "Đã huỷ nhập nguyên tố.");
                    INSTANCE.open(player);
                });
    }

    // ================== PHẦN NGHIỆP VỤ ==================

    private static void handleElementInput(Player player, String elementId, boolean editDefense, String message) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Bạn không còn cầm vật phẩm nào, đã huỷ thao tác.");
            INSTANCE.open(player);
            return;
        }
        String[] parts = message.trim().split("\\s+");
        if (parts.length != 1) {
            player.sendMessage(ChatColor.RED + "Sai định dạng! Vui lòng nhập theo dạng "
                    + ChatColor.WHITE + "<cấp độ>" + ChatColor.RED + " (VD: " + ChatColor.WHITE
                    + "3" + ChatColor.RED + ").");
            INSTANCE.open(player);
            return;
        }
        int level;
        try {
            level = Integer.parseInt(parts[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Giá trị không hợp lệ! Cấp độ phải là số nguyên.");
            INSTANCE.open(player);
            return;
        }
        if (level < 0) {
            player.sendMessage(ChatColor.RED + "Cấp độ không được nhỏ hơn 0.");
            INSTANCE.open(player);
            return;
        }

        if (editDefense) {
            ElementCore.setDefenseLevel(heldItem, elementId, level);
        } else {
            ElementCore.setElementLevel(heldItem, elementId, level);
        }
        org.ThienNguyen.Lore.ElementLore.updateLore(heldItem);

        if (level <= 0) {
            player.sendMessage(ChatColor.RED + "Đã gỡ chỉ số " + (editDefense ? "phòng thủ" : "tấn công")
                    + " của nguyên tố " + ChatColor.AQUA + elementId
                    + ChatColor.RED + " khỏi vật phẩm đang cầm.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Đã đặt " + (editDefense ? "phòng thủ" : "tấn công")
                    + " nguyên tố " + ChatColor.AQUA + elementId
                    + ChatColor.GREEN + " = " + ChatColor.WHITE + "Cấp " + level
                    + ChatColor.GREEN + " cho vật phẩm đang cầm.");
        }
        INSTANCE.open(player);
    }

    /**
     * Vẽ ItemStack đại diện cho 1 nguyên tố: tên, chỉ số tấn công/phòng thủ hiện tại trên
     * vật phẩm, và hướng dẫn thao tác. Icon sẽ đổi sang material khác (thay vì NETHER_STAR
     * mặc định) nếu vật phẩm đang cầm đã có giá trị (tấn công hoặc phòng thủ) cho nguyên tố
     * này, để người chơi dễ dàng nhận biết nguyên tố nào đã được gắn ngay từ icon.
     */
    private static ItemStack buildElementItem(ItemStack heldItem, String elementId) {
        FileConfiguration config = Main.getInstance().getElementConfig();

        int atk = heldItem != null ? ElementCore.getAllElements(heldItem).getOrDefault(elementId.toUpperCase(), 0) : 0;
        int def = heldItem != null ? ElementCore.getAllDefenses(heldItem).getOrDefault(elementId.toUpperCase(), 0) : 0;
        boolean active = atk > 0 || def > 0;
        Material icon = active ? Material.GLOWSTONE : Material.NETHER_STAR;

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(guessNameColor(config, elementId) + "" + ChatColor.BOLD + elementId);
            List<String> lore = new ArrayList<>();

            lore.add(atk > 0
                    ? ChatColor.LIGHT_PURPLE + "➤ Tấn công: " + ChatColor.WHITE + "Cấp " + atk
                    : ChatColor.DARK_GRAY + "➤ Tấn công: Chưa kích hoạt");
            lore.add(def > 0
                    ? ChatColor.LIGHT_PURPLE + "➤ Phòng thủ: " + ChatColor.WHITE + "Cấp " + def
                    : ChatColor.DARK_GRAY + "➤ Phòng thủ: Chưa kích hoạt");
            lore.add("");
            lore.add(ChatColor.GREEN + "✎ Click trái: nhập cấp độ tấn công");
            lore.add(ChatColor.GREEN + "✎ Click phải: nhập cấp độ phòng thủ");
            lore.add(ChatColor.RED + "⇧ Shift+Click: Xoá hẳn nguyên tố này");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Suy ra màu hiển thị của tên nguyên tố từ cấu hình "color" (định dạng "r,g,b", chỉ áp
     * dụng khi particle là DUST). Trả về hex color (§x...) nếu đọc được, mặc định GOLD.
     */
    private static String guessNameColor(FileConfiguration config, String elementId) {
        if (config == null) return ChatColor.GOLD.toString();
        String colorStr = config.getString(elementId + ".color");
        if (colorStr == null) return ChatColor.GOLD.toString();
        Color color = parseColorSafe(colorStr);
        if (color == null) return ChatColor.GOLD.toString();

        String hex = String.format("%06X", color.asRGB());
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    private static Color parseColorSafe(String rgb) {
        try {
            String[] parts = rgb.split(",");
            return Color.fromRGB(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (Exception ex) {
            return null;
        }
    }
}