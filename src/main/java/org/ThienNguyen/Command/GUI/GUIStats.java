package org.ThienNguyen.Command.GUI;
import org.ThienNguyen.Main;
import org.ThienNguyen.Listener.CacheListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * GUI cho phép chọn 1 chỉ số (stat) để chỉnh sửa NGAY TRÊN vật phẩm đang cầm ở tay chính.
 *
 * Lớp này extends {@link AbstractPaginatedGui}, nên KHÔNG tự lo layout/phân trang nữa —
 * phần đó do lớp cha xử lý (header ở slot 0-8, nội dung phân trang 36 ô/trang, footer có nút
 * Trang Trước/Trang Sau/Đóng). GUIStats chỉ còn lo đúng phần "nghiệp vụ" của riêng nó:
 * danh sách stat nào tồn tại, vẽ từng ô ra sao, và click vào 1 stat thì làm gì.
 *
 * Đây cũng là "mẫu" để các GUI tương lai (GUIElement cho buff nguyên tố, GUIAbility cho
 * kỹ năng, ...) đi theo: chỉ cần extends AbstractPaginatedGui<T> với T phù hợp và implement
 * 3 hàm loadEntries/renderEntry/onEntryClick, không cần viết lại logic phân trang hay layout.
 *
 * Lớp này KHÔNG tự implements Listener và KHÔNG cần registerEvents() riêng.
 * Toàn bộ sự kiện (click GUI, nhập giá trị qua chat) đều đi qua GUIListener dùng chung.
 * Chỉ cần đảm bảo trong onEnable() của plugin chính đã gọi:
 *   GUIListener.init(this);
 *
 * QUAN TRỌNG: cần gán statsHandler 1 lần (ngay nơi statsHandler được khởi tạo/dùng), ví dụ:
 *   GUIStats.setStatsHandler(statsHandler::handleCommand);
 *
 * ĐIỀU KHIỂN:
 *   - Click TRÁI vào 1 stat: mở khung chat để nhập GIÁ TRỊ mới (áp lên vật phẩm đang cầm).
 *     Nếu giá trị nhập có hậu tố "%" -> dùng slot đã đặt cho %; nếu là số thường -> dùng
 *     slot đã đặt cho giá trị thường (flat).
 *   - Click PHẢI vào 1 stat: mở khung chat để nhập SLOT áp dụng cho giá trị THƯỜNG (flat).
 *   - Shift + Click PHẢI vào 1 stat: mở khung chat để nhập SLOT áp dụng cho giá trị %.
 *   - Slot hợp lệ: any, chest, feet, head, legs, mainhand, offhand — có thể ghép nhiều slot
 *     bằng dấu phẩy, VD: "offhand,mainhand".
 *
 * ĐỌC GIÁ TRỊ HIỆN TẠI:
 *   - Giá trị %: đọc đúng "pct_<type>" (DOUBLE) + "slot_pct_<type>" (STRING) — khớp 100%
 *     với cách Stats.updatePDCNumeric(...) lưu ở nhánh isPercent.
 *   - Giá trị phẳng (flat): GIẢ ĐỊNH các class Damage/Health/Armor/... lưu dưới
 *     NamespacedKey(plugin, "<type>") kiểu DOUBLE. Đây là suy đoán hợp lý dựa theo quy ước
 *     đặt tên nhất quán đã thấy trong Stats.java, KHÔNG được xác nhận 100% vì chưa có source
 *     các class Setter đó — nếu sai quy ước, mục "giá trị hiện tại" chỉ hiện "Chưa đặt".
 *
 * LƯU Ý VỀ KEY: khóa (key) của mỗi mục trong STAT_INFO chính là chuỗi "type" được gửi
 * thẳng cho Stats.handleCommand(...). NamespacedKey CHỈ chấp nhận ký tự [a-z0-9._-],
 * KHÔNG được có dấu tiếng Việt hay khoảng trắng, nếu không sẽ crash. Vì vậy key luôn là
 * tiếng Anh/ASCII, còn tên tiếng Việt chỉ nằm ở "displayName" để hiển thị cho người chơi.
 *
 * Cách dùng trong lệnh của bạn:
 *   if (args.length < 3) {
 *       GUIStats.openGuiStats(player);
 *       return true;
 *   }
 */
public class GUIStats extends AbstractPaginatedGui<String> {
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Stats";
    private static final String CANCEL_KEYWORD = "cancel";
    private static final String KEY_CLASS_REQUIRE = "class_require";
    private static final List<String> VALID_SLOT_TOKENS =
            List.of("any", "chest", "feet", "head", "legs", "mainhand", "offhand");
    private static final GUIStats INSTANCE = new GUIStats();
    static {
        GUIListener.registerHandler(GUI_TITLE, INSTANCE);
    }
    private GUIStats() {
        super(GUI_TITLE);
    }
    private static IStatsHandler statsHandler;
    public static void setStatsHandler(IStatsHandler handler) {
        statsHandler = handler;
    }
    /** Mở GUI ở đúng trang người chơi đang xem lần gần nhất (giữ tương thích với code cũ). */
    public static void openGuiStats(Player player) {
        INSTANCE.open(player);
    }
    private static final Map<String, String> flatSlotPref = new HashMap<>();
    private static final Map<String, String> pctSlotPref = new HashMap<>();
    private static final Map<String, StatInfo> STAT_INFO = new LinkedHashMap<>();
    static {
        STAT_INFO.put("damage", new StatInfo(Material.IRON_SWORD, "Sát Thương", "Lượng sát thương vật lý gây ra khi tấn công."));
        STAT_INFO.put("health", new StatInfo(Material.GOLDEN_APPLE, "Máu", "Tổng lượng máu tối đa của người dùng."));
        STAT_INFO.put("armor", new StatInfo(Material.IRON_CHESTPLATE, "Giáp", "Chỉ số giáp giúp giảm sát thương nhận vào."));
        STAT_INFO.put("pve_damage", new StatInfo(Material.ZOMBIE_HEAD, "Sát Thương PvE", "Sát thương gây thêm khi đánh quái vật."));
        STAT_INFO.put("pvp_damage", new StatInfo(Material.PLAYER_HEAD, "Sát Thương PvP", "Sát thương gây thêm khi đánh người chơi khác."));
        STAT_INFO.put("pve_defense", new StatInfo(Material.SHIELD, "Phòng Thủ PvE", "Giảm sát thương nhận từ quái vật."));
        STAT_INFO.put("pvp_defense", new StatInfo(Material.TURTLE_HELMET, "Phòng Thủ PvP", "Giảm sát thương nhận từ người chơi khác."));
        STAT_INFO.put("critical_chance", new StatInfo(Material.BLAZE_POWDER, "Tỉ Lệ Chí Mạng", "Xác suất gây ra đòn đánh chí mạng."));
        STAT_INFO.put("critical_damage", new StatInfo(Material.BLAZE_ROD, "Sát Thương Chí Mạng", "Lượng sát thương tăng thêm khi đánh chí mạng."));
        STAT_INFO.put("lifesteal", new StatInfo(Material.REDSTONE, "Hút Máu", "Hồi máu dựa trên phần trăm sát thương gây ra."));
        STAT_INFO.put("dodge_rate", new StatInfo(Material.FEATHER, "Tỉ Lệ Né Tránh", "Xác suất né hoàn toàn một đòn tấn công."));
        STAT_INFO.put("block_rate", new StatInfo(Material.SHIELD, "Tỉ Lệ Đỡ Đòn", "Xác suất đỡ được đòn tấn công của đối phương."));
        STAT_INFO.put("penetration", new StatInfo(Material.ARROW, "Xuyên Giáp", "Bỏ qua một phần giáp của mục tiêu."));
        STAT_INFO.put("level_require", new StatInfo(
                Material.EXPERIENCE_BOTTLE,
                "Yêu Cầu Cấp Độ",
                "Cấp độ tối thiểu để sử dụng vật phẩm.\n(Hỗ trợ từ Fabled / MMOCore)"
        ));
        STAT_INFO.put("true_damage", new StatInfo(Material.NETHER_STAR, "Sát Thương Chuẩn", "Sát thương không bị giảm bởi giáp hay hiệu ứng."));
        STAT_INFO.put("thorns", new StatInfo(Material.CACTUS, "Gai Phản Đòn", "Phản lại một phần sát thương cho kẻ tấn công."));
        STAT_INFO.put(KEY_CLASS_REQUIRE, new StatInfo(
                Material.BOOK,
                "Yêu Cầu Lớp Nhân Vật",
                "Lớp nhân vật cần có để dùng vật phẩm (nhập tên lớp, không phải số).\n(Hỗ trợ từ Fabled / MMOCore)"
        ));
        STAT_INFO.put("max_mana", new StatInfo(
                Material.LAPIS_LAZULI,
                "Mana Tối Đa",
                "Tổng lượng mana tối đa.\n(Hỗ trợ từ Fabled / MMOCore)"
        ));
        STAT_INFO.put("mana_regen", new StatInfo(
                Material.GLOWSTONE_DUST,
                "Hồi Mana",
                "Tốc độ hồi phục mana mỗi giây.\n(Hỗ trợ từ Fabled / MMOCore)"
        ));
        STAT_INFO.put("exp_bonus", new StatInfo(
                Material.EXPERIENCE_BOTTLE,
                "Thưởng Kinh Nghiệm",
                "Tăng thêm phần trăm kinh nghiệm nhận được.\n(Hỗ trợ từ Fabled / MMOCore)"
        ));
        STAT_INFO.put("attack_speed", new StatInfo(Material.SUGAR, "Tốc Độ Đánh", "Tốc độ thực hiện đòn tấn công."));
        STAT_INFO.put("movement_speed", new StatInfo(Material.LEATHER_BOOTS, "Tốc Độ Di Chuyển", "Tốc độ di chuyển của người chơi."));
        STAT_INFO.put("health_regen", new StatInfo(Material.GOLDEN_CARROT, "Hồi Máu", "Tốc độ hồi phục máu mỗi giây."));
        STAT_INFO.put("armor_pen", new StatInfo(Material.ARROW, "Xuyên Giáp (Armor Pen)", "Giảm hiệu quả giáp của mục tiêu."));
        STAT_INFO.put("all_damage", new StatInfo(Material.DIAMOND_SWORD, "Toàn Bộ Sát Thương", "Tăng tất cả các loại sát thương."));
        STAT_INFO.put("all_defense", new StatInfo(Material.DIAMOND_CHESTPLATE, "Toàn Bộ Phòng Thủ", "Tăng tất cả các loại phòng thủ."));
        STAT_INFO.put("bow_damage", new StatInfo(Material.BOW, "Sát Thương Cung", "Sát thương gây thêm khi dùng cung."));
        STAT_INFO.put("knockback_resistance", new StatInfo(Material.ANVIL, "Kháng Hất Lùi", "Giảm hiệu ứng bị hất lùi khi trúng đòn."));
        STAT_INFO.put("death_damage", new StatInfo(Material.WITHER_SKELETON_SKULL, "Sát Thương Tử Vong", "Sát thương đặc biệt gây ra khi mục tiêu tử vong."));
        STAT_INFO.put("durability", new StatInfo(Material.ANVIL, "Độ Bền", "Độ bền tối đa của vật phẩm."));
        STAT_INFO.put("magic_damage", new StatInfo(Material.BLAZE_ROD, "Sát Thương Phép", "Sát thương gây ra từ các đòn phép thuật."));
        STAT_INFO.put("magic_defense", new StatInfo(Material.ENCHANTED_BOOK, "Kháng Phép", "Giảm sát thương nhận từ phép thuật."));
        STAT_INFO.put("accuracy", new StatInfo(Material.ARROW, "Độ Chính Xác", "Giảm khả năng bị né tránh khi tấn công."));
        STAT_INFO.put("crit_damage_reduction", new StatInfo(Material.SHIELD, "Giảm Thiệt Hại Tới Hạn", "Giảm sát thương nhận từ đòn chí mạng."));
        STAT_INFO.put("effect_resistance", new StatInfo(Material.MILK_BUCKET, "Kháng Hiệu Ứng", "Giảm thời gian hoặc xác suất dính hiệu ứng bất lợi."));
        STAT_INFO.put("cooldown_reduction", new StatInfo(
                Material.CLOCK,
                "Giảm Hồi Chiêu",
                "Giảm thời gian hồi chiêu của kỹ năng.\n(Hỗ trợ từ Fabled / MMOCore)"
        ));
        STAT_INFO.put("damage_reduction", new StatInfo(Material.NETHERITE_CHESTPLATE, "Giảm Thiệt Hại", "Giảm sát thương nhận vào."));
        STAT_INFO.put("deep_wound", new StatInfo(Material.SPLASH_POTION, "Vết Thương Sâu", "Giảm khả năng hồi phục của mục tiêu."));
    }
    private static final int SLOT_HELD_ITEM = 4;

    // ================== IMPLEMENT KHUNG CỦA AbstractPaginatedGui ==================

    @Override
    protected List<String> loadEntries(Player player) {
        return new ArrayList<>(STAT_INFO.keySet());
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
                        ChatColor.GRAY + "trước khi chỉnh sửa chỉ số."
                ));
                placeholder.setItemMeta(meta);
            }
            inv.setItem(SLOT_HELD_ITEM, placeholder);
        } else {
            inv.setItem(SLOT_HELD_ITEM, heldItem.clone());
        }
    }

    @Override
    protected ItemStack renderEntry(Player player, String statKey) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean hasItem = heldItem != null && heldItem.getType() != Material.AIR;
        ItemStack refItem = hasItem ? heldItem : null;
        StatInfo info = STAT_INFO.get(statKey);
        boolean isClassRequire = statKey.equals(KEY_CLASS_REQUIRE);
        String flatSlot = isClassRequire ? "any" : getFlatSlot(player, statKey, refItem);
        String pctSlot = isClassRequire ? "any" : getPctSlot(player, statKey, refItem);
        return buildStatItem(refItem, statKey, info, flatSlot, pctSlot);
    }

    @Override
    protected void onEntryClick(Player player, String statKey, InventoryClickEvent event) {
        StatInfo info = STAT_INFO.get(statKey);
        if (!info.supported) {
            player.sendMessage(ChatColor.RED + "Chỉ số " + ChatColor.WHITE + info.displayName
                    + ChatColor.RED + " chưa được plugin hỗ trợ, không thể chỉnh sửa.");
            return;
        }
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Bạn cần cầm 1 vật phẩm ở tay chính trước khi chỉnh sửa!");
            return;
        }
        boolean isClassRequire = statKey.equals(KEY_CLASS_REQUIRE);
        if (event.isRightClick()) {
            if (isClassRequire) {
                player.sendMessage(ChatColor.YELLOW + "Yêu Cầu Lớp Nhân Vật không áp dụng theo slot, không thể đổi.");
                return;
            }
            boolean isPctSlotEdit = event.isShiftClick();
            player.closeInventory();
            String kindLabel = isPctSlotEdit ? "phần trăm (%)" : "số thường (flat)";
            player.sendMessage(ChatColor.GREEN + "» Nhập slot áp dụng cho giá trị " + kindLabel + " của "
                    + ChatColor.AQUA + info.displayName + ChatColor.GREEN + " vào khung chat.");
            player.sendMessage(ChatColor.GRAY + "Slot hợp lệ: " + ChatColor.WHITE
                    + "any, chest, feet, head, legs, mainhand, offhand");
            player.sendMessage(ChatColor.GRAY + "Có thể ghép nhiều slot bằng dấu phẩy, VD: "
                    + ChatColor.WHITE + "offhand,mainhand");
            player.sendMessage(ChatColor.GRAY + "Gõ " + ChatColor.RED + CANCEL_KEYWORD + ChatColor.GRAY + " để huỷ.");
            GUIListener.requestChatInput(player, CANCEL_KEYWORD,
                    message -> handleSlotInput(player, statKey, message, isPctSlotEdit),
                    () -> {
                        player.sendMessage(ChatColor.RED + "Đã huỷ đổi slot.");
                        INSTANCE.open(player);
                    });
            return;
        }
        player.closeInventory();
        if (isClassRequire) {
            player.sendMessage(ChatColor.GREEN + "» Nhập tên lớp nhân vật yêu cầu (VD: Warrior) vào khung chat.");
        } else {
            String flatSlot = getFlatSlot(player, statKey, heldItem);
            String pctSlot = getPctSlot(player, statKey, heldItem);
            player.sendMessage(ChatColor.GREEN + "» Nhập giá trị mới cho " + ChatColor.AQUA + info.displayName
                    + ChatColor.GREEN + " vào khung chat.");
            player.sendMessage(ChatColor.GRAY + "Số thường (VD: 10) → slot: " + ChatColor.WHITE + flatSlot);
            player.sendMessage(ChatColor.GRAY + "Kèm % (VD: 10%) → slot: " + ChatColor.WHITE + pctSlot);
        }
        player.sendMessage(ChatColor.GRAY + "Gõ " + ChatColor.RED + CANCEL_KEYWORD + ChatColor.GRAY + " để huỷ.");
        GUIListener.requestChatInput(player, CANCEL_KEYWORD,
                message -> handleValueInput(player, statKey, message),
                () -> {
                    player.sendMessage(ChatColor.RED + "Đã huỷ nhập giá trị.");
                    INSTANCE.open(player);
                });
    }

    // ================== PHẦN NGHIỆP VỤ GIỮ NGUYÊN NHƯ CŨ ==================

    private static ItemStack buildStatItem(ItemStack heldItem, String statKey, StatInfo info,
                                           String flatSlot, String pctSlot) {
        ItemStack item = new ItemStack(info.icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String color = info.supported ? ChatColor.AQUA.toString() : ChatColor.DARK_GRAY.toString();
            meta.setDisplayName(color + info.displayName);
            List<String> lore = new ArrayList<>();
            if (info.description != null && !info.description.isEmpty()) {
                String[] descLines = info.description.split("\n");
                for (String line : descLines) {
                    lore.add(ChatColor.GRAY + line);
                }
            }
            lore.add("");
            boolean isClassRequire = statKey.equals(KEY_CLASS_REQUIRE);
            if (info.supported) {
                if (isClassRequire) {
                    String currentClass = readClassRequireValue(heldItem);
                    lore.add(ChatColor.YELLOW + "➤ Hiện tại: " + ChatColor.WHITE
                            + (currentClass != null ? currentClass : ChatColor.DARK_GRAY + "Chưa đặt"));
                } else {
                    lore.add(currentValueLoreLine(heldItem, statKey));
                }
            }
            lore.add("");
            if (!info.supported) {
                lore.add(ChatColor.RED + "⚠ Chưa được plugin hỗ trợ");
            } else if (isClassRequire) {
                lore.add(ChatColor.GREEN + "✎ Trái: nhập tên lớp yêu cầu");
            } else {
                lore.add(ChatColor.GREEN + "✎ Trái: nhập giá trị");
                lore.add(ChatColor.LIGHT_PURPLE + "⇄ Phải: đổi slot (thường): " + ChatColor.WHITE + flatSlot);
                lore.add(ChatColor.LIGHT_PURPLE + "⇧⇄ Shift+Phải: đổi slot (%): " + ChatColor.WHITE + pctSlot);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    /**
     * Dòng lore hiển thị giá trị hiện tại (gộp cả flat lẫn % nếu cả 2 cùng tồn tại).
     */
    private static String currentValueLoreLine(ItemStack item, String type) {
        Double flatValue = readFlatValue(item, type);
        Double pctValue = readPctValue(item, type);
        if (flatValue == null && pctValue == null) {
            return ChatColor.DARK_GRAY + "➤ Hiện tại: Chưa đặt";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.YELLOW).append("➤ Hiện tại: ").append(ChatColor.WHITE);
        boolean first = true;
        if (flatValue != null) {
            sb.append(trimNumber(flatValue));
            first = false;
        }
        if (pctValue != null) {
            if (!first) sb.append(ChatColor.GRAY).append(" / ").append(ChatColor.WHITE);
            sb.append(trimNumber(pctValue)).append("%");
        }
        return sb.toString();
    }
    private static void handleValueInput(Player player, String statKey, String message) {
        boolean isClassRequire = statKey.equals(KEY_CLASS_REQUIRE);
        if (!isClassRequire && !isValidNumericInput(message)) {
            player.sendMessage(ChatColor.RED + "Giá trị không hợp lệ! Vui lòng nhập một con số (VD: 10 hoặc 10%).");
            INSTANCE.open(player);
            return;
        }
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Bạn không còn cầm vật phẩm nào, đã huỷ thao tác.");
            INSTANCE.open(player);
            return;
        }
        if (statsHandler == null) {
            player.sendMessage(ChatColor.RED + "Lỗi: GUIStats chưa được liên kết với statsHandler."
                    + " Hãy gọi GUIStats.setStatsHandler(statsHandler::handleCommand).");
            INSTANCE.open(player);
            return;
        }
        String targetSlot;
        if (isClassRequire) {
            targetSlot = "any";
        } else {
            boolean isPercent = message.endsWith("%");
            targetSlot = isPercent ? getPctSlot(player, statKey, heldItem) : getFlatSlot(player, statKey, heldItem);
        }
        String[] cmdArgs = new String[]{"stats", statKey, message};
        statsHandler.handleCommand(player, cmdArgs, targetSlot);
        CacheListener.refreshCache(player);
        StatInfo info = STAT_INFO.get(statKey);
        String displayName = (info != null) ? info.displayName : statKey;
        player.sendMessage(ChatColor.GREEN + "Đã đặt " + ChatColor.AQUA + displayName
                + ChatColor.GREEN + " = " + ChatColor.WHITE + message
                + ChatColor.GREEN + " (slot: " + ChatColor.WHITE + targetSlot + ChatColor.GREEN
                + ") cho vật phẩm đang cầm.");
        INSTANCE.open(player);
    }
    private static void handleSlotInput(Player player, String statKey, String rawInput, boolean isPct) {
        String normalized = normalizeSlotInput(rawInput);
        if (normalized == null) {
            player.sendMessage(ChatColor.RED + "Slot không hợp lệ! Giá trị cho phép: any, chest, feet, head, legs, "
                    + "mainhand, offhand (có thể ghép nhiều bằng dấu phẩy, VD: offhand,mainhand).");
            INSTANCE.open(player);
            return;
        }
        String key = prefKey(player, statKey);
        if (isPct) {
            pctSlotPref.put(key, normalized);
        } else {
            flatSlotPref.put(key, normalized);
        }
        StatInfo info = STAT_INFO.get(statKey);
        String displayName = (info != null) ? info.displayName : statKey;
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Đã đặt slot (" + (isPct ? "%" : "thường") + ") cho "
                + ChatColor.AQUA + displayName + ChatColor.LIGHT_PURPLE + " thành: " + ChatColor.WHITE + normalized);
        INSTANCE.open(player);
    }
    /**
     * Kiểm tra + chuẩn hoá chuỗi slot người chơi nhập: cho phép ghép nhiều token bằng dấu phẩy,
     * mỗi token phải nằm trong VALID_SLOT_TOKENS. Trả về null nếu không hợp lệ.
     */
    private static String normalizeSlotInput(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split(",");
        List<String> cleaned = new ArrayList<>();
        for (String part : parts) {
            String token = part.trim().toLowerCase();
            if (token.isEmpty() || !VALID_SLOT_TOKENS.contains(token)) return null;
            if (!cleaned.contains(token)) cleaned.add(token);
        }
        if (cleaned.isEmpty()) return null;
        return String.join(",", cleaned);
    }
    private static Double readPctValue(ItemStack item, String type) {
        return readDouble(item, "pct_" + type);
    }
    private static String readPctSlotTag(ItemStack item, String type) {
        return readString(item, "slot_pct_" + type);
    }
    private static String readFlatSlotTag(ItemStack item, String type) {
        return readString(item, "slot_" + type);
    }
    /**
     * Đọc giá trị phẳng (flat). GIẢ ĐỊNH quy ước NamespacedKey(plugin, "<type>") kiểu DOUBLE
     * — xem ghi chú ở đầu file. "durability" là ngoại lệ đã biết chắc: lưu dưới "UNBREAKING".
     */
    private static Double readFlatValue(ItemStack item, String type) {
        if (type.equals("durability")) {
            return readDouble(item, "UNBREAKING");
        }
        return readDouble(item, type);
    }
    private static String readClassRequireValue(ItemStack item) {
        return readString(item, KEY_CLASS_REQUIRE);
    }
    private static Double readDouble(ItemStack item, String rawKey) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Main.getInstance(), rawKey);
        if (!pdc.has(key, PersistentDataType.DOUBLE)) return null;
        return pdc.get(key, PersistentDataType.DOUBLE);
    }
    private static String readString(ItemStack item, String rawKey) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Main.getInstance(), rawKey);
        if (!pdc.has(key, PersistentDataType.STRING)) return null;
        return pdc.get(key, PersistentDataType.STRING);
    }
    private static String trimNumber(double value) {
        if (!Double.isInfinite(value) && !Double.isNaN(value) && value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
    private static String prefKey(Player player, String statKey) {
        return player.getUniqueId() + ":" + statKey;
    }
    /**
     * Slot áp dụng cho giá trị THƯỜNG: ưu tiên lựa chọn người chơi đã tự đặt trong phiên này,
     * nếu chưa có thì đồng bộ theo slot THẬT đang lưu trên item ("slot_"+type), mặc định "any".
     */
    private static String getFlatSlot(Player player, String statKey, ItemStack heldItem) {
        String key = prefKey(player, statKey);
        if (flatSlotPref.containsKey(key)) return flatSlotPref.get(key);
        String stored = readFlatSlotTag(heldItem, statKey);
        return stored != null ? stored : "any";
    }
    /**
     * Slot áp dụng cho giá trị %: ưu tiên lựa chọn người chơi đã tự đặt trong phiên này,
     * nếu chưa có thì đồng bộ theo slot THẬT đang lưu trên item ("slot_pct_"+type), mặc định "any".
     */
    private static String getPctSlot(Player player, String statKey, ItemStack heldItem) {
        String key = prefKey(player, statKey);
        if (pctSlotPref.containsKey(key)) return pctSlotPref.get(key);
        String stored = readPctSlotTag(heldItem, statKey);
        return stored != null ? stored : "any";
    }
    private static boolean isValidNumericInput(String raw) {
        String numPart = raw.endsWith("%") ? raw.substring(0, raw.length() - 1) : raw;
        if (numPart.isEmpty()) return false;
        try {
            Double.parseDouble(numPart);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
    /**
     * Thông tin hiển thị cho từng stat trong GUI.
     */
    private static class StatInfo {
        final Material icon;
        final String displayName;
        final String description;
        final boolean supported;
        StatInfo(Material icon, String displayName, String description) {
            this(icon, displayName, description, true);
        }
        StatInfo(Material icon, String displayName, String description, boolean supported) {
            this.icon = icon;
            this.displayName = displayName;
            this.description = description;
            this.supported = supported;
        }
    }
}