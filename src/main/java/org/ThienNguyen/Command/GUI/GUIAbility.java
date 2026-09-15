package org.ThienNguyen.Command.GUI;

import org.ThienNguyen.Ability.AbilityData;
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
 * GUI liệt kê toàn bộ kỹ năng (ability) có thể gắn lên vật phẩm đang cầm ở tay chính.
 *
 * Đi theo đúng "khuôn mẫu" của {@link GUIStats}: extends {@link AbstractPaginatedGui},
 * nên KHÔNG tự lo layout/phân trang — phần đó do lớp cha xử lý. Lớp này chỉ lo phần
 * nghiệp vụ: danh sách kỹ năng nào tồn tại, vẽ lore cho từng ô ra sao, và click vào
 * 1 kỹ năng thì làm gì.
 *
 * Lớp này KHÔNG tự implements Listener và KHÔNG cần registerEvents() riêng — mọi sự kiện
 * (click GUI, nhập giá trị qua chat) đều đi qua GUIListener dùng chung. Chỉ cần đảm bảo
 * trong onEnable() của plugin chính đã gọi:
 *   GUIListener.init(this);
 *
 * ĐIỀU KHIỂN:
 *   - Click (trái hoặc phải) vào 1 kỹ năng: mở khung chat để nhập CẤP ĐỘ và TỈ LỆ (%),
 *     theo định dạng "<cấp độ> <tỉ lệ>", VD "4 2" = cấp 4, 2% tỉ lệ. Gõ từ khoá huỷ để
 *     dừng thao tác mà không thay đổi gì.
 *   - Shift + Click vào 1 kỹ năng: gỡ bỏ kỹ năng đó khỏi vật phẩm đang cầm NGAY LẬP TỨC
 *     (không cần nhập chat) bằng cách đặt tỉ lệ về 0% (giữ nguyên cấp độ đang có, hoặc
 *     cấp 1 nếu kỹ năng chưa từng được đặt).
 *
 * Dữ liệu được đọc/ghi qua {@link AbilityData}, dưới dạng chuỗi "TÊN:CẤP:TỈ_LỆ" lưu trong
 * PersistentDataContainer của vật phẩm — không cần biết chi tiết cách lưu trữ đó ở đây.
 *
 * Cách dùng trong lệnh của bạn:
 *   if (args.length < 1) {
 *       GUIAbility.openGuiAbility(player);
 *       return true;
 *   }
 */
public class GUIAbility extends AbstractPaginatedGui<String> {

    private static final String GUI_TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Abilities";
    private static final String CANCEL_KEYWORD = "cancel";
    private static final int SLOT_HELD_ITEM = 4;

    private static final GUIAbility INSTANCE = new GUIAbility();
    static {
        GUIListener.registerHandler(GUI_TITLE, INSTANCE);
    }

    private GUIAbility() {
        super(GUI_TITLE);
    }

    /** Mở GUI ở đúng trang người chơi đang xem lần gần nhất. */
    public static void openGuiAbility(Player player) {
        INSTANCE.open(player);
    }

    // ================== DỮ LIỆU HIỂN THỊ CHO TỪNG KỸ NĂNG ==================

    private static final Map<String, AbilityInfo> ABILITY_INFO = new LinkedHashMap<>();
    static {
        // I. Sát Thương Trực Tiếp / Đòn Đánh Đơn
        ABILITY_INFO.put("LIGHTNING", new AbilityInfo(Material.TRIDENT, "Thiên Lôi Giáng Thế",
                "Gọi sét đánh vào mục tiêu và tích sát thương cộng thêm cho đòn đánh kế tiếp.",
                "5% + (Cấp − 1) × 3% sát thương gốc (lưu làm sát thương cộng thêm)"));
        ABILITY_INFO.put("FIRST_STRIKE", new AbilityInfo(Material.GOLDEN_SWORD, "Nhất Kích Phủ Đầu",
                "Gây một đòn đánh bất ngờ, đồng thời khiến mục tiêu choáng váng trong thời gian ngắn.",
                "40% + (Cấp − 1) × 20% sát thương gốc"));
        ABILITY_INFO.put("DARK_IMPACT", new AbilityInfo(Material.NETHERITE_SWORD, "Ám Kích Hắc Ảnh",
                "Gây một đòn hắc ám mạnh và xoay mục tiêu 180° nếu là người chơi.",
                "80% + (Cấp − 1) × 15% sát thương gốc"));
        ABILITY_INFO.put("BUBBLE_DEFLECTOR", new AbilityInfo(Material.SHIELD, "Lá Chắn Bong Bóng Phản Đòn",
                "Phản lại một phần sát thương lên kẻ tấn công.",
                "60% + (Cấp − 1) × 10% sát thương gốc"));
        ABILITY_INFO.put("TNT_STUCK", new AbilityInfo(Material.TNT, "Bom Gắn Đầu",
                "Gắn bom lên mục tiêu, sau 5 giây phát nổ và gây sát thương diện rộng.",
                "80% + (Cấp − 1) × 20% sát thương gốc"));
        ABILITY_INFO.put("EXPLODE", new AbilityInfo(Material.FIRE_CHARGE, "Vụ Nổ Hủy Diệt",
                "Gây một vụ nổ tại vị trí mục tiêu và đốt cháy các sinh vật xung quanh.",
                "60% + (Cấp − 1) × 25% sát thương gốc"));
        ABILITY_INFO.put("LILAC_BLOOM_BOMB", new AbilityInfo(Material.LILAC, "Bom Hoa Tử Đinh Hương",
                "Tạo vòng hoa quanh mục tiêu rồi phát nổ sau 2 giây.",
                "50% + (Cấp − 1) × 15% sát thương gốc"));
        ABILITY_INFO.put("STAR_FALL", new AbilityInfo(Material.NETHER_STAR, "Sao Băng Giáng Họa",
                "Đánh dấu vị trí mục tiêu rồi triệu hồi sao băng tấn công sau 1 giây.",
                "20% + (Cấp − 1) × 10% sát thương gốc"));

        // II. Sát Thương Diện Rộng (AOE)
        ABILITY_INFO.put("STAR_RITUAL", new AbilityInfo(Material.END_ROD, "Nghi Lễ Ngôi Sao",
                "Tạo ma trận ngôi sao dưới chân và gây sát thương lên kẻ địch xung quanh.",
                "60% + (Cấp − 1) × 7% sát thương gốc (1 lần)"));
        ABILITY_INFO.put("SUN_STRIKE_AOE", new AbilityInfo(Material.GLOWSTONE, "Địa Chấn Ánh Dương",
                "Gọi tia sáng đánh lần lượt các mục tiêu trong khu vực.",
                "10% + (Cấp − 1) × 4% sát thương gốc / mục tiêu"));
        ABILITY_INFO.put("FLAME_PULSE", new AbilityInfo(Material.BLAZE_POWDER, "Sóng Xung Kích Lửa",
                "Tạo vòng lửa lan rộng từ mục tiêu và thiêu đốt kẻ địch trong vùng.",
                "50% + Cấp × 10% sát thương gốc / mục tiêu"));
        ABILITY_INFO.put("FIRE_RAIN", new AbilityInfo(Material.MAGMA_CREAM, "Mưa Lửa Địa Ngục",
                "Tạo vùng lửa quanh mục tiêu và liên tục gây sát thương theo thời gian.",
                "4% + Cấp × 2% sát thương gốc / đợt (mỗi 0.5 giây)"));
        ABILITY_INFO.put("SHADOW_WAVE", new AbilityInfo(Material.WITHER_ROSE, "Sóng Bóng Tối Nuốt Chửng",
                "Phát ra sóng bóng tối, gây hiệu ứng Bóng Tối và Héo Mòn lên kẻ địch.",
                "5% sát thương gốc (1 lần)"));
        ABILITY_INFO.put("WIND_TORNADO", new AbilityInfo(Material.FEATHER, "Lốc Xoáy Cuồng Phong",
                "Triệu hồi lốc xoáy bay về phía trước, hất tung kẻ địch trên đường đi.",
                "10% + (Cấp − 1) × 5% sát thương gốc / mục tiêu"));
        ABILITY_INFO.put("FIRE_TRIPLE_SHOT", new AbilityInfo(Material.BLAZE_ROD, "Tam Hỏa Xạ",
                "Bắn liên tiếp 3 luồng lửa vào mục tiêu.",
                "10% + (Cấp − 1) × 5% sát thương gốc / phát (tối đa 3 phát)"));
        ABILITY_INFO.put("ELECTRIC_BLADE", new AbilityInfo(Material.LIGHTNING_ROD, "Lưỡi Đao Sấm Sét",
                "Phóng điện từ mục tiêu và lan sang tối đa 4 kẻ địch gần nhất.",
                "20% + (Cấp − 1) × 10% sát thương gốc / lần đánh (tối đa 5 mục tiêu)"));
        ABILITY_INFO.put("FAIRY_CHAIN", new AbilityInfo(Material.CHAIN, "Xích Tiên Nữ",
                "Trói kẻ địch trong phạm vi gần. Nếu mục tiêu ở trong vùng đủ lâu, xích phát nổ.",
                "10% + (Cấp − 1) × 4% sát thương gốc"));
        ABILITY_INFO.put("LEAF_STORM", new AbilityInfo(Material.OAK_LEAVES, "Bão Lá Cuồng Phong",
                "Tạo bão lá xung quanh người dùng, gây sát thương liên tục trong 3 giây.",
                "10% + (Cấp − 1) × 5% sát thương gốc / đợt (tối đa 3 đợt)"));
        ABILITY_INFO.put("PLASMA_ORB", new AbilityInfo(Material.END_CRYSTAL, "Cầu Năng Lượng Plasma",
                "Triệu hồi cầu plasma bay về phía trước và gây sát thương lên sinh vật xung quanh.",
                "8% + (Cấp − 1) × 4% sát thương gốc / giây"));
        ABILITY_INFO.put("FIRE_ORB", new AbilityInfo(Material.FIRE_CHARGE, "Quả Cầu Hỏa Diệm",
                "Tạo cầu lửa thiêu đốt khu vực rồi phát nổ sau 5 giây.",
                "Giai đoạn đốt: 6% + (Cấp − 1) × 3% / giây. Giai đoạn nổ: 10% + (Cấp − 1) × 5%"));

        // III. Sát Thương Theo Thời Gian / Độc & Bệnh Dịch
        ABILITY_INFO.put("BLEED", new AbilityInfo(Material.REDSTONE, "Chảy Máu",
                "Gây hiệu ứng chảy máu khiến mục tiêu mất máu liên tục trong 3 giây.",
                "1% + Cấp × 0.5% sát thương gốc / giây (3 đợt)"));
        ABILITY_INFO.put("FIRE_VORTEX", new AbilityInfo(Material.CAMPFIRE, "Cuồng Phong Hỏa Diệm",
                "Tạo vòng xoáy lửa quanh mục tiêu và gây sát thương liên tục trong 3 giây.",
                "2% + Cấp × 1% sát thương gốc / giây"));
        ABILITY_INFO.put("DARK_FLAME", new AbilityInfo(Material.SOUL_CAMPFIRE, "Hắc Diệm Thiêu Đốt",
                "Ngọn lửa hắc ám thiêu đốt mục tiêu liên tục trong 5 giây.",
                "10% + (Cấp − 1) × 5% sát thương gốc / giây"));
        ABILITY_INFO.put("PLAGUE_SPREAD", new AbilityInfo(Material.ROTTEN_FLESH, "Ôn Dịch Lan Truyền",
                "Gây bệnh lên mục tiêu. Khi mục tiêu chết, bệnh lây sang sinh vật khác gần đó.",
                "5% + (Cấp − 1) × 5% sát thương gốc / giây (trong 5 giây)"));
        ABILITY_INFO.put("VENOM_SPREAD", new AbilityInfo(Material.SPIDER_EYE, "Vùng Độc Lan Tỏa",
                "Tạo vùng độc gây hiệu ứng Độc liên tục lên sinh vật bên trong.",
                "Không gây sát thương trực tiếp; gây hiệu ứng Độc liên tục"));
        ABILITY_INFO.put("POISON", new AbilityInfo(Material.FERMENTED_SPIDER_EYE, "Nọc Độc",
                "Gây hiệu ứng Độc lên mục tiêu trong thời gian dựa trên cấp kỹ năng.",
                "Theo hiệu ứng Poison của Minecraft"));
        ABILITY_INFO.put("WITHER", new AbilityInfo(Material.WITHER_SKELETON_SKULL, "Héo Mòn",
                "Gây hiệu ứng Héo Mòn lên mục tiêu trong thời gian dựa trên cấp kỹ năng.",
                "Theo hiệu ứng Wither của Minecraft"));

        // IV. Hiệu Ứng Bất Lợi / Khống Chế
        ABILITY_INFO.put("FREEZE", new AbilityInfo(Material.ICE, "Đóng Băng Toàn Thân",
                "Đóng băng mục tiêu tại chỗ, vô hiệu hóa khả năng di chuyển và AI.",
                "Không gây sát thương"));
        ABILITY_INFO.put("ROOTS", new AbilityInfo(Material.OAK_SAPLING, "Rễ Cây Trói Buộc",
                "Trói chân mục tiêu bằng hiệu ứng Chậm Chạp cực mạnh.",
                "Không gây sát thương"));
        ABILITY_INFO.put("DISARM", new AbilityInfo(Material.SHEARS, "Tước Vũ Khí",
                "Tước khả năng gây sát thương cận chiến của mục tiêu trong thời gian ngắn.",
                "Không gây sát thương"));
        ABILITY_INFO.put("CURSE", new AbilityInfo(Material.ENCHANTED_BOOK, "Lời Nguyền Suy Yếu",
                "Làm mục tiêu yếu đi và tích sát thương cộng thêm cho đòn đánh tiếp theo.",
                "5% + (Cấp − 1) × 3% sát thương gốc (lưu làm sát thương cộng thêm)"));
        ABILITY_INFO.put("AIR_SHOCK", new AbilityInfo(Material.FEATHER, "Chấn Động Không Khí",
                "Hất tung mục tiêu lên không trung và tích sát thương cộng thêm cho đòn đánh kế tiếp.",
                "5% + (Cấp − 1) × 3% sát thương gốc (lưu làm sát thương cộng thêm)"));
        ABILITY_INFO.put("BUBBLE", new AbilityInfo(Material.WATER_BUCKET, "Giam Cầm Bong Bóng",
                "Nhốt mục tiêu trong bong bóng, khiến chúng bay lên và lơ lửng trong thời gian ngắn.",
                "3% + Cấp × 2% sát thương gốc (lưu làm sát thương cộng thêm)"));
        ABILITY_INFO.put("BLACK_HOLE", new AbilityInfo(Material.ENDER_EYE, "Hố Đen Vũ Trụ",
                "Tạo hố đen hút các sinh vật xung quanh về tâm.",
                "Không gây sát thương"));
        ABILITY_INFO.put("SHADOW_DEVOUR", new AbilityInfo(Material.BLACK_DYE, "Bóng Tối Nuốt Chửng",
                "Tạo vùng bóng tối và liên tục gây hiệu ứng Darkness cho sinh vật bên trong.",
                "Không gây sát thương"));
        ABILITY_INFO.put("CONFUSE", new AbilityInfo(Material.PUFFERFISH, "Hoa Mắt Choáng Váng",
                "Gây hiệu ứng Buồn Nôn khiến mục tiêu mất phương hướng.",
                "Không gây sát thương"));
        ABILITY_INFO.put("BLIND", new AbilityInfo(Material.INK_SAC, "Mù Lòa",
                "Gây hiệu ứng Mù, hạn chế tầm nhìn của mục tiêu.",
                "Không gây sát thương"));
        ABILITY_INFO.put("SLOWNESS", new AbilityInfo(Material.SOUL_SAND, "Chậm Chạp",
                "Làm giảm tốc độ di chuyển của mục tiêu.",
                "Không gây sát thương"));
        ABILITY_INFO.put("WEAK", new AbilityInfo(Material.POTION, "Suy Yếu",
                "Làm giảm sức mạnh tấn công của mục tiêu.",
                "Không gây sát thương trực tiếp"));
        ABILITY_INFO.put("TIRED", new AbilityInfo(Material.IRON_PICKAXE, "Rã Rời Kiệt Sức",
                "Làm giảm tốc độ đào và thao tác của mục tiêu.",
                "Không gây sát thương"));
        ABILITY_INFO.put("HUNGER", new AbilityInfo(Material.ROTTEN_FLESH, "Đói Lả",
                "Gây hiệu ứng Đói lên người chơi mục tiêu.",
                "Không gây sát thương trực tiếp"));
        ABILITY_INFO.put("BADLUCK", new AbilityInfo(Material.COAL_BLOCK, "Vận Rủi",
                "Gây hiệu ứng Vận Rủi, làm giảm may mắn của mục tiêu.",
                "Không gây sát thương"));

        // V. Hỗ Trợ / Hồi Máu / Triệu Hồi
        ABILITY_INFO.put("ANGEL", new AbilityInfo(Material.ENCHANTED_GOLDEN_APPLE, "Thiên Thần Hộ Mệnh",
                "Hồi phục một phần máu tối đa của người sử dụng.",
                "Hồi phục: 5% + Cấp × 2% máu tối đa"));
        ABILITY_INFO.put("VAMPIRISM", new AbilityInfo(Material.GHAST_TEAR, "Hút Máu Ma Cà Rồng",
                "Chuyển một phần sát thương gây ra thành lượng máu hồi phục cho người dùng.",
                "Hồi phục: 20% + (Cấp − 1) × 10% sát thương vừa gây ra"));
        ABILITY_INFO.put("SPIRIT_WOLF", new AbilityInfo(Material.BONE, "Sói Tâm Linh",
                "Triệu hồi sói linh hồn truy đuổi mục tiêu và tấn công trong 5 giây.",
                "25% + (Cấp − 1) × 10% sát thương gốc / đòn cắn"));

        // VI. Đòn Tấn Công Tầm Xa Xuyên Thấu
        ABILITY_INFO.put("SONIC_WAVE", new AbilityInfo(Material.ECHO_SHARD, "Sóng Âm Thanh Xuyên Phá",
                "Phóng sóng âm xuyên thẳng về phía trước, gây sát thương cho các sinh vật trên đường đi.",
                "6% + Cấp × 3% sát thương gốc / mục tiêu"));
    }

    // ================== IMPLEMENT KHUNG CỦA AbstractPaginatedGui ==================

    @Override
    protected List<String> loadEntries(Player player) {
        return new ArrayList<>(ABILITY_INFO.keySet());
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
                        ChatColor.GRAY + "trước khi chỉnh sửa kỹ năng."
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
        return buildAbilityItem(hasItem ? heldItem : null, key);
    }

    @Override
    protected void onEntryClick(Player player, String key, InventoryClickEvent event) {
        AbilityInfo info = ABILITY_INFO.get(key);
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Bạn cần cầm 1 vật phẩm ở tay chính trước khi chỉnh sửa kỹ năng!");
            return;
        }

        // Shift + Click: gỡ bỏ kỹ năng ngay lập tức (đặt tỉ lệ về 0%), không cần nhập chat.
        if (event.isShiftClick()) {
            String[] current = readAbilityEntry(heldItem, key);
            int level = 1;
            if (current != null) {
                try {
                    level = Integer.parseInt(current[0]);
                } catch (NumberFormatException ignored) {
                    level = 1;
                }
            }
            AbilityData.setAbility(heldItem, key, level, 0.0);
            org.ThienNguyen.Lore.AbilityLore.updateLore(heldItem);
            player.sendMessage(ChatColor.RED + "Đã gỡ bỏ kỹ năng " + ChatColor.AQUA + info.displayName
                    + ChatColor.RED + " khỏi vật phẩm đang cầm (đặt tỉ lệ về 0%).");
            INSTANCE.open(player);
            return;
        }

        // Click thường: mở khung chat để nhập "<cấp độ> <tỉ lệ>".
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "» Nhập " + ChatColor.WHITE + "cấp độ và tỉ lệ (%)"
                + ChatColor.GREEN + " cho " + ChatColor.AQUA + info.displayName
                + ChatColor.GREEN + " vào khung chat, theo định dạng:");
        player.sendMessage(ChatColor.YELLOW + "<cấp độ> <tỉ lệ>" + ChatColor.GRAY + "   (VD: "
                + ChatColor.WHITE + "4 2" + ChatColor.GRAY + " = cấp 4, 2% tỉ lệ)");
        player.sendMessage(ChatColor.GRAY + "Gõ " + ChatColor.RED + CANCEL_KEYWORD
                + ChatColor.GRAY + " để huỷ.");
        GUIListener.requestChatInput(player, CANCEL_KEYWORD,
                message -> handleAbilityInput(player, key, message),
                () -> {
                    player.sendMessage(ChatColor.RED + "Đã huỷ nhập kỹ năng.");
                    INSTANCE.open(player);
                });
    }

    // ================== PHẦN NGHIỆP VỤ ==================

    private static void handleAbilityInput(Player player, String key, String message) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Bạn không còn cầm vật phẩm nào, đã huỷ thao tác.");
            INSTANCE.open(player);
            return;
        }
        String[] parts = message.trim().split("\\s+");
        if (parts.length != 2) {
            player.sendMessage(ChatColor.RED + "Sai định dạng! Vui lòng nhập theo dạng "
                    + ChatColor.WHITE + "<cấp độ> <tỉ lệ>" + ChatColor.RED + " (VD: " + ChatColor.WHITE
                    + "4 2" + ChatColor.RED + ").");
            INSTANCE.open(player);
            return;
        }
        int level;
        double chance;
        try {
            level = Integer.parseInt(parts[0]);
            chance = Double.parseDouble(parts[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Giá trị không hợp lệ! Tỉ lệ phải là số, cấp độ phải là số nguyên.");
            INSTANCE.open(player);
            return;
        }
        if (chance < 0 || chance > 100) {
            player.sendMessage(ChatColor.RED + "Tỉ lệ phải nằm trong khoảng 0 - 100.");
            INSTANCE.open(player);
            return;
        }
        if (level < 1) {
            player.sendMessage(ChatColor.RED + "Cấp độ phải từ 1 trở lên.");
            INSTANCE.open(player);
            return;
        }
        AbilityData.setAbility(heldItem, key, level, chance);
        org.ThienNguyen.Lore.AbilityLore.updateLore(heldItem);
        AbilityInfo info = ABILITY_INFO.get(key);
        String displayName = (info != null) ? info.displayName : key;
        player.sendMessage(ChatColor.GREEN + "Đã đặt " + ChatColor.AQUA + displayName
                + ChatColor.GREEN + " = " + ChatColor.WHITE + "Cấp " + level
                + ChatColor.GREEN + ", Tỉ lệ " + ChatColor.WHITE + trimNumber(chance) + "%"
                + ChatColor.GREEN + " cho vật phẩm đang cầm.");
        INSTANCE.open(player);
    }

    /**
     * Vẽ ItemStack đại diện cho 1 kỹ năng: tên, mô tả (lore), thông tin sát thương/hiệu ứng,
     * giá trị hiện tại đang gắn trên vật phẩm (nếu có), và hướng dẫn thao tác.
     */
    private static ItemStack buildAbilityItem(ItemStack heldItem, String key) {
        AbilityInfo info = ABILITY_INFO.get(key);
        ItemStack item = new ItemStack(info.icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + info.displayName);
            List<String> lore = new ArrayList<>();

            for (String line : wrapText(info.lore, 40)) {
                lore.add(ChatColor.GRAY + line);
            }
            lore.add("");
            lore.add(ChatColor.GOLD + "Sát thương / Hiệu ứng:");
            for (String line : wrapText(info.damageInfo, 40)) {
                lore.add(ChatColor.YELLOW + line);
            }
            lore.add("");

            String[] current = readAbilityEntry(heldItem, key);
            if (current == null) {
                lore.add(ChatColor.DARK_GRAY + "➤ Hiện tại: Chưa kích hoạt");
            } else {
                double chanceVal = parseDoubleSafe(current[1]);
                if (chanceVal <= 0) {
                    lore.add(ChatColor.DARK_GRAY + "➤ Hiện tại: Đã tắt (0%)");
                } else {
                    lore.add(ChatColor.LIGHT_PURPLE + "➤ Hiện tại: " + ChatColor.WHITE
                            + "Cấp " + current[0] + ChatColor.GRAY + " - " + ChatColor.WHITE
                            + trimNumber(chanceVal) + "%");
                }
            }
            lore.add("");
            lore.add(ChatColor.GREEN + "✎ Click: nhập tỉ lệ % và cấp độ");
            lore.add(ChatColor.RED + "⇧ Shift+Click: Gỡ bỏ kỹ năng (tỉ lệ về 0%)");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Đọc entry "CẤP:TỈ_LỆ" hiện tại của 1 kỹ năng trên vật phẩm, dựa trên chuỗi
     * "TÊN:CẤP:TỈ_LỆ" mà AbilityData lưu trữ. Trả về null nếu vật phẩm null hoặc
     * chưa từng gắn kỹ năng này.
     */
    private static String[] readAbilityEntry(ItemStack item, String key) {
        if (item == null) return null;
        List<String> entries = AbilityData.getAbilityList(item);
        String prefix = key.toUpperCase() + ":";
        for (String entry : entries) {
            if (entry.startsWith(prefix)) {
                String[] parts = entry.split(":");
                if (parts.length >= 3) {
                    return new String[]{parts[1], parts[2]};
                }
            }
        }
        return null;
    }

    private static double parseDoubleSafe(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static String trimNumber(double value) {
        if (!Double.isInfinite(value) && !Double.isNaN(value) && value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
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
     * Thông tin hiển thị cho từng kỹ năng trong GUI.
     */
    private static class AbilityInfo {
        final Material icon;
        final String displayName;
        final String lore;
        final String damageInfo;

        AbilityInfo(Material icon, String displayName, String lore, String damageInfo) {
            this.icon = icon;
            this.displayName = displayName;
            this.lore = lore;
            this.damageInfo = damageInfo;
        }
    }
}