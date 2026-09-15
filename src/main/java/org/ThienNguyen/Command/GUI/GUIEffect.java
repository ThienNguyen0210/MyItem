package org.ThienNguyen.Command.GUI;

import org.ThienNguyen.Effect.BuffData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI liệt kê toàn bộ hiệu ứng (buff / debuff) có thể gắn lên vật phẩm đang cầm ở tay chính.
 *
 * Đi theo đúng "khuôn mẫu" của {@link GUIAbility}: extends {@link AbstractPaginatedGui},
 * nên KHÔNG tự lo layout/phân trang — phần đó do lớp cha xử lý. Lớp này chỉ lo phần
 * nghiệp vụ: danh sách hiệu ứng nào tồn tại, vẽ lore cho từng ô ra sao, và click vào
 * 1 hiệu ứng thì làm gì.
 *
 * Lớp này KHÔNG tự implements Listener và KHÔNG cần registerEvents() riêng — mọi sự kiện
 * (click GUI, nhập giá trị qua chat) đều đi qua GUIListener dùng chung. Chỉ cần đảm bảo
 * trong onEnable() của plugin chính đã gọi:
 *   GUIListener.init(this);
 *
 * ĐIỀU KHIỂN:
 *   - Click (trái hoặc phải) vào 1 hiệu ứng: mở khung chat để nhập CẤP ĐỘ, theo định dạng
 *     "<cấp độ>", VD "3" = cấp 3. Gõ từ khoá huỷ để dừng thao tác mà không thay đổi gì.
 *   - Shift + Click vào 1 hiệu ứng: gỡ bỏ hiệu ứng đó khỏi vật phẩm đang cầm NGAY LẬP TỨC
 *     (không cần nhập chat) bằng cách đặt cấp độ về 0 (xoá khỏi PDC).
 *
 * Dữ liệu được đọc/ghi qua {@link BuffData}, dưới dạng chuỗi "TÊN:CẤP;" lưu trong
 * PersistentDataContainer của vật phẩm — không cần biết chi tiết cách lưu trữ đó ở đây.
 * Lore tương ứng được đồng bộ qua {@link org.ThienNguyen.Lore.EffectLore#updateLore(ItemStack)}
 * mỗi khi PDC thay đổi (cả khi nhập chat lẫn khi shift-click gỡ bỏ).
 *
 * Cách dùng trong lệnh của bạn:
 *   if (args.length < 1) {
 *       GUIEffect.openGuiEffect(player);
 *       return true;
 *   }
 */
public class GUIEffect extends AbstractPaginatedGui<String> {

    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Effects";
    private static final String CANCEL_KEYWORD = "cancel";
    private static final int SLOT_HELD_ITEM = 4;

    private static final GUIEffect INSTANCE = new GUIEffect();
    static {
        GUIListener.registerHandler(GUI_TITLE, INSTANCE);
    }

    private GUIEffect() {
        super(GUI_TITLE);
    }

    /** Mở GUI ở đúng trang người chơi đang xem lần gần nhất. */
    public static void openGuiEffect(Player player) {
        INSTANCE.open(player);
    }

    // ================== DỮ LIỆU HIỂN THỊ CHO TỪNG HIỆU ỨNG ==================

    private enum EffectType { BUFF, DEBUFF }

    private static final Map<String, EffectInfo> EFFECT_INFO = new LinkedHashMap<>();
    static {
        // I. Buff (hiệu ứng có lợi)
        EFFECT_INFO.put("SPEED", new EffectInfo(EffectType.BUFF, Material.SUGAR, "Tốc Độ",
                "Tăng tốc độ di chuyển."));
        EFFECT_INFO.put("FAST_DIGGING", new EffectInfo(EffectType.BUFF, Material.GOLDEN_PICKAXE, "Đào Nhanh",
                "Tăng tốc độ đào và thao tác."));
        EFFECT_INFO.put("INCREASE_DAMAGE", new EffectInfo(EffectType.BUFF, Material.BLAZE_POWDER, "Sức Mạnh",
                "Tăng sát thương cận chiến gây ra."));
        EFFECT_INFO.put("JUMP", new EffectInfo(EffectType.BUFF, Material.RABBIT_FOOT, "Nhảy Cao",
                "Tăng độ cao nhảy và giảm sát thương rơi."));
        EFFECT_INFO.put("REGENERATION", new EffectInfo(EffectType.BUFF, Material.GLISTERING_MELON_SLICE, "Hồi Máu",
                "Hồi phục máu liên tục theo thời gian."));
        EFFECT_INFO.put("DAMAGE_RESISTANCE", new EffectInfo(EffectType.BUFF, Material.SHIELD, "Kháng Sát Thương",
                "Giảm sát thương phải nhận."));
        EFFECT_INFO.put("FIRE_RESISTANCE", new EffectInfo(EffectType.BUFF, Material.MAGMA_CREAM, "Kháng Lửa",
                "Miễn nhiễm sát thương lửa và dung nham."));
        EFFECT_INFO.put("WATER_BREATHING", new EffectInfo(EffectType.BUFF, Material.TURTLE_HELMET, "Thở Dưới Nước",
                "Không bị mất hơi khi ở dưới nước."));
        EFFECT_INFO.put("HEALTH_BOOST", new EffectInfo(EffectType.BUFF, Material.GOLDEN_APPLE, "Cường Hóa Máu",
                "Tăng lượng máu tối đa."));
        EFFECT_INFO.put("ABSORPTION", new EffectInfo(EffectType.BUFF, Material.ENCHANTED_GOLDEN_APPLE, "Hấp Thụ",
                "Thêm một lớp máu đệm hấp thụ sát thương."));
        EFFECT_INFO.put("NIGHT_VISION", new EffectInfo(EffectType.BUFF, Material.GOLDEN_CARROT, "Dạ Nhãn",
                "Nhìn rõ trong bóng tối và dưới nước."));
        EFFECT_INFO.put("LUCK", new EffectInfo(EffectType.BUFF, Material.EMERALD, "May Mắn",
                "Tăng may mắn khi câu cá và mở rương chiến lợi phẩm."));

        // II. Debuff (hiệu ứng bất lợi)
        EFFECT_INFO.put("SLOWNESS", new EffectInfo(EffectType.DEBUFF, Material.SOUL_SAND, "Chậm Chạp",
                "Giảm tốc độ di chuyển."));
        EFFECT_INFO.put("SLOWNESS_DIGGING", new EffectInfo(EffectType.DEBUFF, Material.WOODEN_HOE, "Đào Chậm",
                "Giảm tốc độ đào và thao tác."));
        EFFECT_INFO.put("NAUSEA", new EffectInfo(EffectType.DEBUFF, Material.PUFFERFISH, "Buồn Nôn",
                "Làm méo và lắc màn hình, gây mất phương hướng."));
        EFFECT_INFO.put("BLINDNESS", new EffectInfo(EffectType.DEBUFF, Material.INK_SAC, "Mù Lòa",
                "Hạn chế tầm nhìn."));
        EFFECT_INFO.put("HUNGER", new EffectInfo(EffectType.DEBUFF, Material.ROTTEN_FLESH, "Đói Lả",
                "Tăng tốc độ tiêu hao thức ăn."));
        EFFECT_INFO.put("WEAKNESS", new EffectInfo(EffectType.DEBUFF, Material.POTION, "Suy Yếu",
                "Giảm sát thương cận chiến gây ra."));
        EFFECT_INFO.put("POISON", new EffectInfo(EffectType.DEBUFF, Material.SPIDER_EYE, "Nọc Độc",
                "Mất máu liên tục nhưng không thể gây tử vong."));
        EFFECT_INFO.put("WITHER", new EffectInfo(EffectType.DEBUFF, Material.WITHER_SKELETON_SKULL, "Héo Mòn",
                "Mất máu liên tục và có thể gây tử vong."));
        EFFECT_INFO.put("GLOWING", new EffectInfo(EffectType.DEBUFF, Material.GLOW_INK_SAC, "Phát Sáng",
                "Hiện viền phát sáng, lộ vị trí kể cả khi ẩn nấp."));
        EFFECT_INFO.put("LEVITATION", new EffectInfo(EffectType.DEBUFF, Material.SHULKER_SHELL, "Bay Lên",
                "Bị đẩy bay lên không trung liên tục."));
        EFFECT_INFO.put("UNLUCK", new EffectInfo(EffectType.DEBUFF, Material.COAL, "Vận Rủi",
                "Giảm may mắn khi câu cá và mở rương chiến lợi phẩm."));
        EFFECT_INFO.put("DARKNESS", new EffectInfo(EffectType.DEBUFF, Material.SCULK, "Bóng Tối",
                "Làm tối màn hình, hạn chế tầm nhìn."));
    }

    // ================== IMPLEMENT KHUNG CỦA AbstractPaginatedGui ==================

    @Override
    protected List<String> loadEntries(Player player) {
        return new ArrayList<>(EFFECT_INFO.keySet());
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
                        ChatColor.GRAY + "trước khi chỉnh sửa hiệu ứng."
                ));
                placeholder.setItemMeta(meta);
            }
            inv.setItem(SLOT_HELD_ITEM, placeholder);
        } else {
            inv.setItem(SLOT_HELD_ITEM, heldItem.clone());
        }
    }

    @Override
    protected ItemStack renderEntry(Player player, String key) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean hasItem = heldItem != null && heldItem.getType() != Material.AIR;
        return buildEffectItem(hasItem ? heldItem : null, key);
    }

    @Override
    protected void onEntryClick(Player player, String key, InventoryClickEvent event) {
        EffectInfo info = EFFECT_INFO.get(key);
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Bạn cần cầm 1 vật phẩm ở tay chính trước khi chỉnh sửa hiệu ứng!");
            return;
        }

        // Shift + Click: gỡ bỏ hiệu ứng ngay lập tức (đặt cấp độ về 0), không cần nhập chat.
        if (event.isShiftClick()) {
            BuffData.setEffect(heldItem, key, 0);
            org.ThienNguyen.Lore.EffectLore.updateLore(heldItem);
            player.sendMessage(ChatColor.RED + "Đã gỡ bỏ hiệu ứng " + ChatColor.AQUA + info.displayName
                    + ChatColor.RED + " khỏi vật phẩm đang cầm.");
            INSTANCE.open(player);
            return;
        }

        // Click thường: mở khung chat để nhập "<cấp độ>".
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "» Nhập " + ChatColor.WHITE + "cấp độ"
                + ChatColor.GREEN + " cho " + ChatColor.AQUA + info.displayName
                + ChatColor.GREEN + " vào khung chat, theo định dạng:");
        player.sendMessage(ChatColor.YELLOW + "<cấp độ>" + ChatColor.GRAY + "   (VD: "
                + ChatColor.WHITE + "3" + ChatColor.GRAY + " = cấp 3)");
        player.sendMessage(ChatColor.GRAY + "Gõ " + ChatColor.RED + CANCEL_KEYWORD
                + ChatColor.GRAY + " để huỷ.");
        GUIListener.requestChatInput(player, CANCEL_KEYWORD,
                message -> handleEffectInput(player, key, message),
                () -> {
                    player.sendMessage(ChatColor.RED + "Đã huỷ nhập hiệu ứng.");
                    INSTANCE.open(player);
                });
    }

    // ================== PHẦN NGHIỆP VỤ ==================

    private static void handleEffectInput(Player player, String key, String message) {
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
        if (level < 1) {
            player.sendMessage(ChatColor.RED + "Cấp độ phải từ 1 trở lên.");
            INSTANCE.open(player);
            return;
        }
        BuffData.setEffect(heldItem, key, level);
        org.ThienNguyen.Lore.EffectLore.updateLore(heldItem);
        EffectInfo info = EFFECT_INFO.get(key);
        String displayName = (info != null) ? info.displayName : key;
        player.sendMessage(ChatColor.GREEN + "Đã đặt " + ChatColor.AQUA + displayName
                + ChatColor.GREEN + " = " + ChatColor.WHITE + "Cấp " + level
                + ChatColor.GREEN + " cho vật phẩm đang cầm.");
        INSTANCE.open(player);
    }

    /**
     * Vẽ ItemStack đại diện cho 1 hiệu ứng: tên, mô tả (lore), phân loại buff/debuff,
     * giá trị hiện tại đang gắn trên vật phẩm (nếu có), và hướng dẫn thao tác.
     */
    private static ItemStack buildEffectItem(ItemStack heldItem, String key) {
        EffectInfo info = EFFECT_INFO.get(key);
        ItemStack item = new ItemStack(info.icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ChatColor nameColor = info.type == EffectType.BUFF ? ChatColor.GREEN : ChatColor.RED;
            meta.setDisplayName(nameColor + "" + ChatColor.BOLD + info.displayName);
            List<String> lore = new ArrayList<>();

            lore.add((info.type == EffectType.BUFF ? ChatColor.DARK_GREEN : ChatColor.DARK_RED)
                    + (info.type == EffectType.BUFF ? "Buff" : "Debuff"));
            lore.add("");
            for (String line : wrapText(info.description, 40)) {
                lore.add(ChatColor.GRAY + line);
            }
            lore.add("");

            Integer currentLevel = readCurrentLevel(heldItem, key);
            if (currentLevel == null || currentLevel <= 0) {
                lore.add(ChatColor.DARK_GRAY + "➤ Hiện tại: Chưa kích hoạt");
            } else {
                lore.add(ChatColor.LIGHT_PURPLE + "➤ Hiện tại: " + ChatColor.WHITE
                        + "Cấp " + currentLevel);
            }
            lore.add("");
            lore.add(ChatColor.GREEN + "✎ Click: nhập cấp độ");
            lore.add(ChatColor.RED + "⇧ Shift+Click: Gỡ bỏ hiệu ứng");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Đọc cấp độ hiện tại của 1 hiệu ứng trên vật phẩm qua {@link BuffData}.
     * Trả về null nếu vật phẩm null hoặc chưa từng gắn hiệu ứng này.
     */
    private static Integer readCurrentLevel(ItemStack item, String key) {
        if (item == null) return null;
        return BuffData.getEffects(item).get(key.toUpperCase());
    }

    /**
     * Bẻ dòng chữ dài thành nhiều dòng lore ngắn hơn (theo từ, không cắt giữa chữ),
     * để lore hiển thị gọn gàng trong inventory thay vì tràn 1 dòng cực dài.
     */
    private static List<String> wrapText(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() > 0 && current.length() + 1 + word.length() > maxLineLength) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) current.append(" ");
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    /**
     * Thông tin hiển thị cho từng hiệu ứng trong GUI.
     */
    private static class EffectInfo {
        final EffectType type;
        final Material icon;
        final String displayName;
        final String description;

        EffectInfo(EffectType type, Material icon, String displayName, String description) {
            this.type = type;
            this.icon = icon;
            this.displayName = displayName;
            this.description = description;
        }
    }
}