package org.ThienNguyen.Command;

import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.Listener.AIExperienceGUI;
import org.ThienNguyen.Listener.Station.StationCMD;
import org.ThienNguyen.Main;
import org.ThienNguyen.Effect.BuffData;
import org.ThienNguyen.Listener.Station.StationDatabase;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.ThienNguyen.Lore.EffectLore;
import org.ThienNguyen.Skill.ISkill;
import org.bukkit.Bukkit;
import org.ThienNguyen.Skill.SkillManager;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MyItemCommand implements CommandExecutor {
    private final StationDatabase stationDb;
    private final Main plugin;
    public static final NamespacedKey COMBO_KEY = new NamespacedKey(Main.getInstance(), "combo_id");
    private final Stats statsHandler = new Stats();
    public MyItemCommand(Main plugin, StationDatabase stationDb) {
        this.plugin = plugin;
        this.stationDb = stationDb;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        var lang = plugin.getLangManager();
        if (args.length == 0) {
            sendHelp(sender, 1);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cSố trang phải là số nguyên!");
                        return true;
                    }
                }
                sendHelp(sender, page);
            }
            case "loreformat" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 2) {
                    player.sendMessage("§cSử dụng: /mi loreformat <id>");
                    return true;
                }

                String formatId = args[1];
                ItemStack item = player.getInventory().getItemInMainHand();

                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§cBạn phải cầm vật phẩm trên tay!");
                    return true;
                }

                // Debug
                if (!plugin.getLoreFormatConfig().contains(formatId)) {
                    player.sendMessage("§cFormat ID §f" + formatId + " §ckhông tồn tại!");
                    return true;
                }

                ItemMeta meta = item.getItemMeta();
                NamespacedKey key = new NamespacedKey(plugin, "lore_format_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, formatId);
                item.setItemMeta(meta);

                org.ThienNguyen.Lore.LoreGenerator.rebuild(item);

                player.sendMessage("§a[MyItem] §aĐã áp dụng lore format: §e" + formatId);
                return true;
            }
            case "ic" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;

                // Lệnh: /mi ic add <id> hoặc /mi ic unadd
                if (args.length >= 2) {
                    String subAction = args[1].toLowerCase();
                    ItemStack item = player.getInventory().getItemInMainHand();

                    if (item == null || item.getType().isAir()) {
                        player.sendMessage("§c[MyItem] Bạn phải cầm vật phẩm trên tay!");
                        return true;
                    }

                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return true;

                    switch (subAction) {
                        case "add" -> {
                            if (args.length < 3) {
                                player.sendMessage("§eSử dụng: /mi ic add <id>");
                                return true;
                            }
                            String id = args[2];
                            meta.getPersistentDataContainer().set(COMBO_KEY, PersistentDataType.STRING, id);
                            item.setItemMeta(meta);
                            player.sendMessage("§a[MyItem] Đã gắn mã Combo §e" + id + " §avào vật phẩm!");
                        }
                        case "unadd" -> {
                            // Kiểm tra xem item có mã combo không trước khi gỡ
                            if (meta.getPersistentDataContainer().has(COMBO_KEY, PersistentDataType.STRING)) {
                                meta.getPersistentDataContainer().remove(COMBO_KEY);
                                item.setItemMeta(meta);
                                player.sendMessage("§a[MyItem] Đã gỡ mã Combo khỏi vật phẩm thành công!");
                            } else {
                                player.sendMessage("§c[MyItem] Vật phẩm này vốn không có mã Combo nào.");
                            }
                        }
                        default -> player.sendMessage("§eSử dụng: /mi ic <add|unadd> [id]");
                    }
                } else {
                    player.sendMessage("§eSử dụng: /mi ic <add|unadd> [id]");
                }
            }
            case "ai" -> {
                if (!(sender instanceof Player player)) return true;
                if (!AIExperienceGUI.hasAccepted(player)) {
                    AIExperienceGUI.openEulaGUI(player);
                    return true;
                }
                // 1. Lấy FileConfiguration từ hàm getAIConfig() trong Main
                FileConfiguration config = Main.getInstance().getAIConfig();

                // 2. Trỏ đúng vào đường dẫn 'ai.profiles'
                ConfigurationSection profilesSection = config.getConfigurationSection("ai.profiles");

                if (profilesSection == null) {
                    player.sendMessage("§c§l[!] §7Không tìm thấy mục §eai.profiles §7trong AIConfig.yml");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("§e/myitem ai <id> §7- Chọn profile");
                    player.sendMessage("§7Danh sách: §b" + String.join(", ", profilesSection.getKeys(false)));
                    return true;
                }

                String id = args[1].toLowerCase();

                // 3. Lấy profile cụ thể từ Section đã trỏ
                ConfigurationSection profile = profilesSection.getConfigurationSection(id);

                if (profile == null) {
                    player.sendMessage("§c§l[!] §7Profile §f" + id + " §7không tồn tại!");
                    player.sendMessage("§7Gợi ý: §e" + String.join(", ", profilesSection.getKeys(false)));
                    return true;
                }

                player.sendMessage("§a§l✔ §7Đã chọn: §e" + id);
                player.sendMessage("§f> §7" + profile.getString("description", "Không có mô tả"));
                player.sendMessage("§b§l[?] §fVui lòng nhập mô tả item vào chat (hoặc 'cancel'):");
                player.sendMessage("§c§l[?] §fCác biến normal legendv.v không có ý nghĩa gì hết chỉ để làm lệnh chạy vì author mệt rồi:(");

                player.setMetadata("ai_prompt_mode", new FixedMetadataValue(Main.getInstance(), id));

                // Task hủy sau 2 phút
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    if (player.hasMetadata("ai_prompt_mode")) {
                        player.removeMetadata("ai_prompt_mode", Main.getInstance());
                        player.sendMessage("§c§l[!] §7Hết thời gian nhập prompt.");
                    }
                }, 2400L);
            }
            case "getai" -> {
                if (!(sender instanceof Player player)) return true;
                if (!player.hasPermission("myitem.admin")) {
                    player.sendMessage("§cBạn không có quyền lấy vật phẩm từ kho AI!");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage("§6§l[!] §7Cách dùng: §e/mi getai <id>");
                    return true;
                }

                String id = args[1];
                // Gọi hàm lấy item từ file AI/Item.yml
                ItemStack aiItem = org.ThienNguyen.AI.utils.YamlManager.getItemFromAiFolder(id);

                if (aiItem != null) {
                    player.getInventory().addItem(aiItem);
                    player.sendMessage("§a§l✔ §7Đã lấy vật phẩm ID §e" + id + " §7từ kho lưu trữ AI.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1.5f);
                } else {
                    player.sendMessage("§c§l✘ §7Không tìm thấy vật phẩm ID §f" + id + " §ctrong AI/Item.yml!");
                }
            }
            case "save" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 2) {
                    player.sendMessage("§cSử dụng: /mi save <id>");
                    return true;
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType().isAir()) {
                    player.sendMessage("§cBạn phải cầm vật phẩm trên tay!");
                    return true;
                }
                String id = args[1];
                Main.getInstance().getItemDatabase().saveItem(id, item);
                player.sendMessage("§a[MyItem] Đã lưu vật phẩm thành công với ID: §f" + id);
            }

            case "load" -> {
                // 1. Kiểm tra quyền Admin (Sender có thể là Console hoặc Player)
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§c[MyItem] Bạn không có quyền!");
                    return true;
                }

                // 2. Kiểm tra tham số
                if (args.length < 2) {
                    sender.sendMessage("§cSử dụng: /mi load <id> [player]");
                    return true;
                }

                String id = args[1];

                // 3. Xác định mục tiêu (Target) nhận item
                Player target;
                if (args.length >= 3) {
                    // Nếu có nhập tên player ở args[2]
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage("§cNgười chơi §f" + args[2] + " §ckhông online!");
                        return true;
                    }
                } else {
                    // Nếu không nhập tên, người nhận là chính người gửi lệnh
                    if (sender instanceof Player p) {
                        target = p;
                    } else {
                        sender.sendMessage("§cConsole phải nhập tên người nhận: /mi load <id> <player>");
                        return true;
                    }
                }

                // 4. Load item từ Database
                ItemStack loadedItem = Main.getInstance().getItemDatabase().loadItem(id);

                if (loadedItem != null) {
                    // Thêm vào kho đồ của target
                    target.getInventory().addItem(loadedItem);

                    // Thông báo cho người gửi lệnh
                    sender.sendMessage("§a[MyItem] Successfully sent item §f" + id + " §ato §e" + target.getName());
                    // Thông báo cho người nhận (nếu người nhận không phải người gửi)
                    if (target != sender) {
                        target.sendMessage(lang.getMessage("item.receive-msg", "{id}", id));                    }
                } else {
                    sender.sendMessage("§cKhông tìm thấy vật phẩm có ID: " + id);
                }
            }

            case "delete" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 2) {
                    player.sendMessage("§cSử dụng: /mi delete <id>");
                    return true;
                }
                String id = args[1];
                if (Main.getInstance().getItemDatabase().loadItem(id) == null) {
                    player.sendMessage("§cKhông tìm thấy vật phẩm có ID: " + id);
                    return true;
                }
                Main.getInstance().getItemDatabase().deleteItem(id);
                player.sendMessage("§a[MyItem] Đã xóa vật phẩm §f" + id + " §akhỏi database!");
            }

            case "element" -> {
                if (!(sender instanceof Player player)) return true;

                // Cấu trúc: /mi element <type> <id> <level>
                if (args.length < 4) {
                    player.sendMessage("§6§l[MyItem] §7Sử dụng:");
                    player.sendMessage("§e/mi element attack <id> <level> §7- Tăng sát thương");
                    player.sendMessage("§e/mi element defense <id> <level> §7- Chặn sát thương");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§cBạn phải cầm item trên tay!");
                    return true;
                }

                String type = args[1].toLowerCase(); // attack hoặc defense
                String elementId = args[2].toUpperCase();

                // Kiểm tra xem ID nguyên tố có trong file elements.yml không
                if (!Main.getInstance().getElementConfig().contains(elementId)) {
                    player.sendMessage("§cNguyên tố §f" + elementId + " §ckhông tồn tại!");
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[3]);

                    if (type.equals("attack")) {
                        // Sử dụng hàm addElement có sẵn của bạn
                        org.ThienNguyen.Element.ElementCore.addElement(item, elementId, level);
                        player.sendMessage("§a[MyItem] Đã thêm §6Tấn công " + elementId + " §acấp §e" + level);
                    }
                    else if (type.equals("defense")) {
                        // Gọi hàm addDefenseElement (nhớ thêm hàm này vào ElementCore.java như tôi hướng dẫn ở trên)
                        org.ThienNguyen.Element.ElementCore.addDefenseElement(item, elementId, level);
                        player.sendMessage("§b[MyItem] Đã thêm §3Phòng thủ " + elementId + " §bcấp §e" + level);
                    }
                    else {
                        player.sendMessage("§cLoại không hợp lệ! Sử dụng 'attack' hoặc 'defense'.");
                        return true;
                    }

                    // Cập nhật Lore và Cache
                    org.ThienNguyen.Lore.ElementLore.updateLore(item);
                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);

                } catch (NumberFormatException e) {
                    player.sendMessage("§cLevel phải là một số nguyên!");
                }
            }

             case "stats" -> {
                    if (!(sender instanceof Player player)) return true;

                    // Kiểm tra cấu trúc: /mi stats <loại> <giá trị> [slot]
                    if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /mi stats <loại> <giá trị> [any/mainhand/offhand/head/chest/legs/feet]");
                    return true;
                }

                // Lấy slot nếu có, nếu không mặc định là "any"
                String slot = (args.length >= 4) ? args[3].toLowerCase() : "any";
                // Gọi hàm handleCommand với 3 tham số (đã khớp với Stats.java mới)
                statsHandler.handleCommand(player, args, slot);
                 org.ThienNguyen.Listener.CacheListener.refreshCache(player);

             }
            case "evo" -> {
                if (!(sender instanceof Player player)) return true;

                // Cấu trúc: /mi evo <target> <required_amount> <next_item_id>
                // Ví dụ: /mi evo ZOMBIE 100 kiem_cap_2
                // Ví dụ: /mi evo ALL 50 giap_than
                if (args.length < 4) {
                    player.sendMessage("§6§l[Evolution] §cSử dụng: /mi evo <target|ALL> <số lượng> <ID_Item_Mới>");
                    player.sendMessage("§7- target: Tên mob (ZOMBIE, SKELETON...) hoặc ID MythicMobs.");
                    player.sendMessage("§7- ALL: Giết bất cứ con gì cũng được tính.");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§cBạn phải cầm một vật phẩm trên tay!");
                    return true;
                }

                String target = args[1];
                int required;
                try {
                    required = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cSố lượng phải là một con số!");
                    return true;
                }
                String nextId = args[3];

                // Kiểm tra xem ID item mới có tồn tại trong Database không
                if (Main.getInstance().getItemDatabase().loadItem(nextId) == null) {
                    player.sendMessage("§cID '" + nextId + "' không tồn tại trong Item Database!");
                    return true;
                }

                // Gán dữ liệu vào PDC thông qua EvolutionManager
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();

                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.TARGET_KEY, org.bukkit.persistence.PersistentDataType.STRING, target);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.CURRENT_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.REQUIRED_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, required);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.NEXT_ID_KEY, org.bukkit.persistence.PersistentDataType.STRING, nextId);

                item.setItemMeta(meta);

                // Cập nhật Lore lần đầu để người chơi thấy dòng tiến độ
                org.ThienNguyen.Evolution.EvolutionManager.addProgress(player,item, "INITIALIZE_ONLY");

                player.sendMessage("§a§l✔ §fĐã thiết lập tiến hóa cho vật phẩm!");
                player.sendMessage("§7- Mục tiêu: §e" + target);
                player.sendMessage("§7- Cần giết: §e" + required);
                player.sendMessage("§7- Tiến hóa thành: §b" + nextId);

                return true;
            }
            case "ability" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 4) {
                    player.sendMessage("§cSử dụng: /mi ability <tên> <level> <tỷ_lệ>");
                    return true;
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) return true;
                String abilityName = args[1].toUpperCase();
                try {
                    int level = Integer.parseInt(args[2]);
                    double chance = Double.parseDouble(args[3]);
                    AbilityData.setAbility(item, abilityName, level, chance);
                    org.ThienNguyen.Lore.AbilityLore.updateLore(item);
                    player.sendMessage("§aĐã gán kỹ năng §f" + abilityName + " §eLv." + level + " §7(§b" + chance + "%§7)");
                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cSố liệu không hợp lệ!");
                }
            }

            case "buff", "debuff" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /mi " + subCommand + " <tên_hiệu_ứng> <level>");
                    return true;
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) return true;
                String effectName = args[1].toUpperCase();
                PotionEffectType type = PotionEffectType.getByName(effectName);
                if (type == null) {
                    player.sendMessage("§cHiệu ứng không tồn tại!");
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[2]);
                    BuffData.setEffect(item, effectName, level);
                    EffectLore.updateLore(item);
                    String prefix = subCommand.equals("buff") ? "§a[Buff] " : "§c[Debuff] ";
                    player.sendMessage(prefix + "§7Đã gán §f" + effectName + " §eLv." + level);
                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cLevel không hợp lệ!");
                }
            }

            case "skill" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 6) {
                    player.sendMessage("§cSử dụng: /mi skill <type> <tên skill> <trigger> <cooldown> <level>");
                    return true;
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) return true;

                String typeInput = args[1];
                String skillName = args[2];
                String trigger = args[3].toUpperCase();

                try {
                    int cooldown = Integer.parseInt(args[4]);
                    int level = Integer.parseInt(args[5]);

                    ISkill skill = SkillManager.getSkill(skillName);
                    if (skill == null) {
                        player.sendMessage("§cKỹ năng §f" + skillName + " §ckhông tồn tại!");
                        return true;
                    }

                    var meta = item.getItemMeta();
                    if (meta != null) {
                        NamespacedKey key = new NamespacedKey(Main.getInstance(), "item_skills");
                        String skillEntry = skill.getName() + ":" + trigger + ":" + cooldown + ":" + level + ":" + typeInput;
                        String oldData = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        String newData = (oldData == null || oldData.isEmpty()) ? skillEntry : oldData + "," + skillEntry;

                        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, newData);
                        item.setItemMeta(meta);
                        org.ThienNguyen.Lore.SkillLore.updateLore(item);
                        org.ThienNguyen.Listener.CacheListener.refreshCache(player);

                        player.sendMessage("§a[Skill] Đã thêm thành công: §b" + skill.getName());
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cSố liệu không hợp lệ!");
                }
            }
            case "upgrade" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cChỉ người chơi mới có thể sử dụng lệnh này!");
                    return true;
                }

                // Khởi tạo class GUI và mở giao diện cường hóa
                // Lưu ý: Đảm bảo bạn đã import org.ThienNguyen.Utils.GUI;
                new org.ThienNguyen.Utils.GUI().openUpgrade(player);

                return true;
            }
            case "givegem" -> {
                // 1. Kiểm tra quyền
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                    return true;
                }

                // 2. Sửa logic: Không bắt buộc người gửi phải là Player nữa
                // (Admin đứng ở Console vẫn có thể give cho người chơi được)

                // 3. Kiểm tra tham số (Cần ít nhất 4 tham số: givegem, id, player, amount)
                if (args.length < 4) {
                    sender.sendMessage("§6§l[!] §7Sử dụng: §e/myitem givegem <id> <player> <amount>");
                    return true;
                }

                try {
                    int id = Integer.parseInt(args[1]);
                    Player targetPlayer = Bukkit.getPlayer(args[2]);
                    int amount = Integer.parseInt(args[3]);

                    // Kiểm tra người chơi có online không
                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        sender.sendMessage("§c§l✘ §7Người chơi §e" + args[2] + " §7không trực tuyến!");
                        return true;
                    }

                    // 4. Gọi hàm lấy Gem từ config (Lưu ý: class Upgrade đã được sửa để đọc 'amount' từ Gem.yml)
                    ItemStack gem = org.ThienNguyen.Utils.Upgrade.createGemFromConfig(id);

                    if (gem == null) {
                        sender.sendMessage("§c§l✘ §7Không tìm thấy đá có ID §e" + id + " §7trong file §nUpgrade/Gem.yml");
                        return true;
                    }

                    // --- QUAN TRỌNG: Ghi đè số lượng người dùng nhập từ lệnh ---
                    gem.setAmount(amount);

                    // 5. Thêm vào kho đồ người nhận và xử lý nếu túi đồ đầy
                    Map<Integer, ItemStack> overFlow = targetPlayer.getInventory().addItem(gem);
                    if (!overFlow.isEmpty()) {
                        for (ItemStack left : overFlow.values()) {
                            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), left);
                        }
                        targetPlayer.sendMessage(lang.getMessage("upgrade.gem-fullinventory", "{gem}"));
                    }

                    String gemName = gem.getItemMeta().hasDisplayName() ? gem.getItemMeta().getDisplayName() : gem.getType().name();

                    // Giữ nguyên sendMessage của bạn cho người nhận
                    targetPlayer.sendMessage(lang.getMessage("upgrade.gem-received", "{gem}", gemName));

                    // Thông báo cho người gửi lệnh thành công
                    sender.sendMessage("§a§l✔ §7Đã gửi §e" + amount + "x " + gemName + " §7cho §f" + targetPlayer.getName());

                } catch (NumberFormatException e) {
                    sender.sendMessage("§c§l✘ §7ID hoặc Số lượng không hợp lệ!");
                }
                return true;
            }
            case "giveamulet" -> {
                // 1. Kiểm tra quyền admin
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                    return true;
                }

                // 2. Kiểm tra tham số: /mi giveamulet <player> <amount>
                if (args.length < 3) {
                    sender.sendMessage("§6§l[!] §7Sử dụng: §e/myitem giveamulet <player> <amount>");
                    return true;
                }

                Player target = org.bukkit.Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§c[!] Không tìm thấy người chơi: §f" + args[1]);
                    return true;
                }

                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) amount = 1;

                    // 3. Tạo vật phẩm Bùa Hộ Mệnh từ hàm static trong class Upgrade
                    ItemStack amulet = org.ThienNguyen.Utils.Upgrade.createProtectionScroll();

                    if (amulet == null) {
                        sender.sendMessage("§c§l✘ §7Lỗi: Không tìm thấy cấu hình bùa trong §nUpgrade/protection.yml");
                        return true;
                    }

                    amulet.setAmount(amount);

                    // 4. Thêm vào kho đồ hoặc rơi ra đất nếu đầy
                    if (target.getInventory().firstEmpty() == -1) {
                        target.getWorld().dropItemNaturally(target.getLocation(), amulet);
                    } else {
                        target.getInventory().addItem(amulet);
                    }

                    sender.sendMessage(lang.getMessage("upgrade.amulet-sent",
                            "{amount}", String.valueOf(amount),
                            "{player}", target.getName()));

// Gửi thông báo cho người nhận
                    target.sendMessage(lang.getMessage("upgrade.amulet-received",
                            "{amount}", String.valueOf(amount)));

                } catch (NumberFormatException e) {
                    sender.sendMessage("§c§l✘ §7Số lượng §e" + args[2] + " §7phải là một con số nguyên!");
                }
                return true;
            }
            case "trans" -> {
                if (!(sender instanceof Player player)) return true;
                new org.ThienNguyen.Utils.ChuyenHoa().openGUI(player);
                return true;
            }
            case "unskill" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 2) {
                    player.sendMessage("§cSử dụng: /mi unskill <tên skill>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§cBạn phải cầm vật phẩm trên tay!");
                    return true;
                }

                String skillToRemove = args[1].toUpperCase().trim();
                var meta = item.getItemMeta();
                if (meta == null) return true;

                // 1. Khai báo các Key giống hệt bên SkillLore
                NamespacedKey skillKey = new NamespacedKey(Main.getInstance(), "item_skills");
                NamespacedKey loreStartKey = new NamespacedKey(Main.getInstance(), "skill_lore_start");
                NamespacedKey loreEndKey = new NamespacedKey(Main.getInstance(), "skill_lore_end");

                // 2. Kiểm tra dữ liệu kỹ năng
                String oldData = meta.getPersistentDataContainer().get(skillKey, PersistentDataType.STRING);
                if (oldData == null || oldData.trim().isEmpty()) {
                    player.sendMessage("§cVật phẩm này không có kỹ năng nào để gỡ!");
                    return true;
                }

                // 3. Xử lý xóa kỹ năng trong dữ liệu (Persistent Data)
                List<String> skillList = new ArrayList<>();
                boolean removed = false;
                for (String entry : oldData.split(",")) {
                    entry = entry.trim();
                    if (entry.isEmpty()) continue;
                    String[] parts = entry.split(":", 2);
                    if (parts.length >= 1 && parts[0].trim().equalsIgnoreCase(skillToRemove)) {
                        removed = true;
                        continue;
                    }
                    skillList.add(entry);
                }

                if (!removed) {
                    player.sendMessage("§cKhông tìm thấy kỹ năng §f" + skillToRemove + " §ctrên vật phẩm!");
                    return true;
                }

                // 4. THUẬT TOÁN XÓA LORE: Đọc vị trí khối cũ từ PDC tương tự SkillLore
                if (meta.hasLore()) {
                    List<String> lore = new ArrayList<>(meta.getLore());
                    Integer oldStart = meta.getPersistentDataContainer().get(loreStartKey, PersistentDataType.INTEGER);
                    Integer oldEnd = meta.getPersistentDataContainer().get(loreEndKey, PersistentDataType.INTEGER);

                    // Xóa khối lore cũ nếu tọa độ hợp lệ
                    if (oldStart != null && oldEnd != null && oldStart >= 0 && oldEnd >= oldStart && oldEnd < lore.size()) {
                        for (int i = oldEnd; i >= oldStart; i--) {
                            lore.remove(i);
                        }
                        meta.setLore(lore);
                    }
                }

                // 5. Cập nhật lại dữ liệu kỹ năng vào PDC
                if (skillList.isEmpty()) {
                    meta.getPersistentDataContainer().remove(skillKey);
                    // Xóa luôn tọa độ lore vì không còn kỹ năng nào
                    meta.getPersistentDataContainer().remove(loreStartKey);
                    meta.getPersistentDataContainer().remove(loreEndKey);
                } else {
                    String newData = String.join(",", skillList);
                    meta.getPersistentDataContainer().set(skillKey, PersistentDataType.STRING, newData);
                }

                item.setItemMeta(meta);

                // 6. Gọi update để vẽ lại Lore mới (nếu còn skill khác)
                // Nếu đã hết skill, hàm này sẽ tự dọn dẹp
                org.ThienNguyen.Lore.SkillLore.updateLore(item);

                player.sendMessage("§a[Skill] Đã gỡ kỹ năng §f" + skillToRemove + " §athành công!");
            }
            // Thêm vào switch (subCommand) trong MyItemCommand.java
            case "enchant" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /mi enchant <tên> <level>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                // Chuyển tên người dùng nhập (ví dụ: sharpness) thành NamespacedKey
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(args[1].toLowerCase());
                org.bukkit.enchantments.Enchantment enchant = org.bukkit.enchantments.Enchantment.getByKey(key);

                if (enchant == null) {
                    player.sendMessage("§cKhông tìm thấy Enchant có tên: " + args[1]);
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[2]);
                    // addUnsafeEnchantment để phá vỡ giới hạn cấp độ (lv 1000 thoải mái)
                    item.addUnsafeEnchantment(enchant, level);

                    // Cập nhật lại Lore tùy chỉnh
                    org.ThienNguyen.Enchant.EnchantVanila.updateEnchantLore(item);

                    player.sendMessage("§aĐã ép §e" + args[1] + " §acấp §f" + level);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cLevel phải là số!");
                }
            }

            case "gemstone" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
                    return true;
                }

                // /mi gemstone give <type> <id> <player> <amount>
                if (args.length < 5 || !args[1].equalsIgnoreCase("give")) {
                    sender.sendMessage("§c[!] Sử dụng: /mi gemstone give <gem|drill|remover> <id> <player> <amount>");
                    sender.sendMessage("§7Ví dụ:");
                    sender.sendMessage("§e/mi gemstone give gem ruby1 Steve 1");
                    sender.sendMessage("§e/mi gemstone give drill drill_common Steve 5");
                    sender.sendMessage("§e/mi gemstone give remover remover_basic Steve 10");
                    return true;
                }

                String type = args[2].toLowerCase();
                String id = args[3];
                Player target = Bukkit.getPlayer(args[4]);
                int amount = 1;

                try {
                    if (args.length >= 6) {
                        amount = Integer.parseInt(args[5]);
                    }
                } catch (NumberFormatException ignored) {
                    sender.sendMessage("§cSố lượng phải là số nguyên!");
                    return true;
                }

                if (target == null) {
                    sender.sendMessage("§c[!] Không tìm thấy người chơi: " + args[4]);
                    return true;
                }

                ItemStack itemResult = null;
                String itemTag = "";

                switch (type) {
                    case "gem" -> {
                        FileConfiguration gemConfig = Main.getInstance().getGemConfig();
                        if (gemConfig.contains(id)) {
                            itemResult = createGemItem(id, gemConfig, "GEMSTONE");
                            itemTag = "GEMSTONE";
                        }
                    }
                    case "drill" -> {
                        FileConfiguration ducLoConfig = Main.getInstance().getDucLoConfig();
                        if (ducLoConfig.contains(id)) {
                            itemResult = createGemItem(id, ducLoConfig, "DRILL");
                            itemTag = "DRILL";
                        }
                    }
                    case "remover" -> {
                        // Hỗ trợ đá gỡ ngọc
                        FileConfiguration removerConfig = Main.getInstance().getGemConfig(); // Có thể dùng chung gemConfig hoặc tạo riêng
                        // Nếu bạn muốn config riêng cho remover, thay bằng getRemoverConfig() sau này

                        if (removerConfig.contains(id)) {
                            itemResult = createGemItem(id, removerConfig, "REMOVER");
                            itemTag = "REMOVER";
                        }
                    }
                    default -> {
                        sender.sendMessage("§cLoại không hợp lệ! Chỉ hỗ trợ: §egem §7| §edrill §7| §eremover");
                        return true;
                    }
                }

                if (itemResult != null) {
                    itemResult.setAmount(amount);
                    target.getInventory().addItem(itemResult);

                    sender.sendMessage("§a[GemSystem] §fĐã trao §e" + amount + "x " + id +
                            " §7(" + type.toUpperCase() + ") §acho §e" + target.getName());

                    target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
                } else {
                    sender.sendMessage("§c[!] ID §f" + id + " §ckhông tồn tại trong loại §f" + type);
                }
                return true;
            }
            case "editor" -> {
                if (!(sender instanceof Player player)) return true;

                String link = "https://windycraft.com/editor";

                // Tạo tin nhắn có thể click
                net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent("§a[MyItem] §fTruy cập trang thiết kế tại: ");

                net.md_5.bungee.api.chat.TextComponent linkComponent = new net.md_5.bungee.api.chat.TextComponent("§b§n" + link);

                // Hiệu ứng khi di chuột vào (Hover)
                linkComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.ComponentBuilder("§eClick để sao chép link thiết kế!").create()
                ));

                // Hành động khi Click: Sao chép vào bộ nhớ tạm (Copy to Clipboard)
                linkComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.COPY_TO_CLIPBOARD,
                        link
                ));

                message.addExtra(linkComponent);

                // Gửi tin nhắn cho người chơi
                player.spigot().sendMessage(message);
            }
            case "consume" -> {
                // Kiểm tra quyền hạn
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
                    return true;
                }

                // Cú pháp: /myitem consume give <id> <amount> <player>
                if (args.length < 5) {
                    sender.sendMessage("§cSử dụng: /myitem consume give <id> <amount> <player>");
                    return true;
                }

                if (args[1].equalsIgnoreCase("give")) {
                    String consumeId = args[2];

                    // Kiểm tra số lượng có phải là số không
                    int amount;
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cSố lượng phải là một con số!");
                        return true;
                    }

                    // Kiểm tra người chơi
                    Player target = Bukkit.getPlayer(args[4]);
                    if (target == null) {
                        sender.sendMessage("§cNgười chơi §f" + args[4] + " §ckhông trực tuyến!");
                        return true;
                    }

                    // Gọi hàm tạo item từ ConsumeManager
                    ItemStack consumeItem = org.ThienNguyen.Consume.ConsumeManager.getConsumeItem(consumeId, amount);

                    if (consumeItem == null) {
                        sender.sendMessage("§cID vật phẩm §f" + consumeId + " §ckhông tồn tại trong Consume.yml!");
                        return true;
                    }

                    // Đưa item cho người chơi
                    target.getInventory().addItem(consumeItem);
                    sender.sendMessage("§aĐã gửi §f" + amount + "x " + consumeId + " §acho §e" + target.getName());
                }
            }
            case "connect" -> {
                // 1. Kiểm tra quyền hạn (Thường admin mới được dùng lệnh này để tránh spam)
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                    return true;
                }

                // 2. Kiểm tra nếu sender là người chơi (vì cần lấy inventory để give đồ)
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cLệnh này chỉ dành cho người chơi trong game!");
                    return true;
                }

                // 3. Kiểm tra xem có nhập mã code chưa (/mi connect <mã>)
                if (args.length < 2) {
                    player.sendMessage("§8[§4§l?§8] §cSử dụng: /myitem connect <mã_web>");
                    return true;
                }

                // 4. Lấy mã code và gọi class Web xử lý
                String code = args[1].toUpperCase();
                org.ThienNguyen.Webapi.Web.connectItem(player, code);
            }
            case "reload" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                    return true;
                }
                Main.getInstance().reloadPluginConfigs();
                sender.sendMessage("§a[MyItem] Đã nạp lại toàn bộ cấu hình hệ thống!");
            }
            // [THÊM VÀO TRONG HÀM ONCOMMAND - PHẦN SWITCH CASE]

            case "particle" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cChỉ người chơi mới có thể dùng lệnh này!");
                    return true;
                }
                if (!player.hasPermission("myitem.admin")) {
                    player.sendMessage("§cBạn không có quyền!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cSử dụng: /mi particle <id_trong_particle_yml>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§cHãy cầm một vật phẩm trên tay!");
                    return true;
                }

                String particleId = args[1];
                // Kiểm tra xem ID có tồn tại trong config không (tùy chọn nhưng nên có)
                if (!Main.getInstance().getParticleConfig().contains("effects." + particleId)) {
                    player.sendMessage("§e[!] Cảnh báo: ID '" + particleId + "' chưa được định nghĩa trong Particle.yml");
                }

                var meta = item.getItemMeta();
                if (meta != null) {
                    // Ghi ID particle vào PDC
                    meta.getPersistentDataContainer().set(
                            new NamespacedKey(Main.getInstance(), "item_particle"),
                            PersistentDataType.STRING,
                            particleId
                    );
                    item.setItemMeta(meta);
                    player.sendMessage("§a[✓] Đã thêm hiệu ứng '" + particleId + "' vào vật phẩm!");

                    // Cập nhật lại stats ngay lập tức để hiện particle
                    new org.ThienNguyen.Listener.StatsListener().updatePlayerStats(player);
                }
                return true;
            }

            case "unparticle" -> {
                if (!(sender instanceof Player player)) return true;
                if (!player.hasPermission("myitem.admin")) return true;

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§cHãy cầm một vật phẩm trên tay để xóa hiệu ứng!");
                    return true;
                }

                var meta = item.getItemMeta();
                if (meta != null) {
                    NamespacedKey key = new NamespacedKey(Main.getInstance(), "item_particle");

                    // Kiểm tra xem item có particle không trước khi xóa
                    if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        player.sendMessage("§c[!] Vật phẩm này không có hiệu ứng particle nào để xóa.");
                        return true;
                    }

                    // Chỉ xóa đúng cái NBT lưu Particle ID
                    meta.getPersistentDataContainer().remove(key);
                    item.setItemMeta(meta);

                    player.sendMessage("§e[✓] Đã gỡ bỏ hiệu ứng particle khỏi vật phẩm trên tay!");

                    // Cập nhật lại stats để hiệu ứng biến mất ngay lập tức
                    new org.ThienNguyen.Listener.StatsListener().updatePlayerStats(player);
                }
                return true;
            }

            case "version" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§c[MyItem] Bạn không có quyền!");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cLệnh này chỉ dành cho người chơi!");
                    return true;
                }
                org.ThienNguyen.Webapi.Update.openVersionGUI((Player) sender, plugin);
            }
            case "tooltip" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§c[MyItem] Bạn không có quyền!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cLệnh này chỉ dành cho người chơi!");
                    return true;
                }

                // Kiểm tra số lượng đối số: /mi tooltip <add/undo> [loại]
                if (args.length < 2) {
                    player.sendMessage("§8§m-------§r §6§lTOOLTIP SYSTEM §8§m-------");
                    player.sendMessage("§7» §e/mi tooltip add <loại> §f: Thêm khung tooltip");
                    player.sendMessage("§7» §e/mi tooltip undo §f: Hoàn tác về trạng thái cũ");
                    player.sendMessage("§8§m---------------------------");
                    return true;
                }

                String subAction = args[1].toLowerCase();

                switch (subAction) {
                    case "add" -> {
                        if (args.length < 3) {
                            player.sendMessage("§c[!] Vui lòng nhập loại tooltip (ví dụ: normal, godlike...)");
                            return true;
                        }
                        String type = args[2].toLowerCase();
                        // Gọi Utils xử lý logic
                        org.ThienNguyen.Utils.Tooltips.applyTooltip(player, type);
                    }
                    case "undo" -> {
                        // Gọi Utils xử lý hoàn tác
                        org.ThienNguyen.Utils.Tooltips.handleUndo(player);
                    }
                    default -> player.sendMessage("§c[!] Hành động không hợp lệ. Sử dụng 'add' hoặc 'undo'.");
                }
            }
            case "update" -> {
                // 1. Kiểm tra quyền
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§c[MyItem] Bạn không có quyền!");
                    return true;
                }

                // 2. Nếu người dùng chỉ gõ /myitem update (không có version) -> Mở GUI danh sách
                if (args.length < 2) {
                    if (sender instanceof Player player) {
                        org.ThienNguyen.Webapi.Update.openUpdateListGUI(player, plugin);
                    } else {
                        sender.sendMessage("§cConsole phải nhập rõ version: /myitem update <version>");
                    }
                    return true;
                }

                // 3. Nếu có nhập version (ví dụ: /myitem update 1.0-SNAPSHOT)
                String updateVersion = args[1];

                if (sender instanceof Player player) {
                    // Nếu là người chơi: Gọi hàm tải và gửi thông báo
                    org.ThienNguyen.Webapi.Update.downloadAndUpdate(plugin, updateVersion, player);
                } else {
                    // Nếu là Console: Bạn cần một hàm download dành riêng cho Console (không có biến player)
                    // Hoặc tạm thời thông báo Console không hỗ trợ tải trực tiếp nếu chưa làm hàm riêng
                    sender.sendMessage("§e[MyItem] Console đang tải bản " + updateVersion + " (Xem log tại Console)...");
                    // org.ThienNguyen.Webapi.Update.downloadAndUpdateConsole(plugin, updateVersion);
                }
                return true;
            }
            case "sync" -> {
                // Gọi sang class StationCMD để xử lý logic update
                return new StationCMD(plugin, stationDb).onCommand(sender, command, label, args);
            }
            case "tiers" -> { // Sử dụng dấu ngoặc nhọn để tạo scope riêng, tránh trùng biến player
                if (args.length < 2) {
                    sender.sendMessage("§cSử dụng: /mi tiers <id>");
                    return true;
                }

                if (!(sender instanceof Player pTiers)) {
                    sender.sendMessage("§cChỉ người chơi mới có thể dùng lệnh này!");
                    return true;
                }

                ItemStack itemTier = pTiers.getInventory().getItemInMainHand();
                if (itemTier == null || itemTier.getType().isAir()) {
                    pTiers.sendMessage("§cBạn phải cầm vật phẩm trên tay!");
                    return true;
                }

                // Gọi class xử lý
                org.ThienNguyen.Lore.TiersLore.applyTier(itemTier, args[1].toLowerCase());
                pTiers.sendMessage("§a§l✔ §7Đã cập nhật phẩm chất vật phẩm!");
                break; // Bắt buộc có break khi dùng kiểu case ":"
            }
            default -> sendHelp(sender, 1);
        }
        return true;
    }

    private boolean checkAdmin(Player p) {
        if (!p.hasPermission("myitem.admin")) {
            p.sendMessage("§cBạn không có quyền thực hiện hành động này!");
            return false;
        }
        return true;
    }
    private ItemStack createGemItem(String id, FileConfiguration config, String itemTag) {
        try {
            String matStr = config.getString(id + ".material", "STONE");
            org.bukkit.Material mat = org.bukkit.Material.valueOf(matStr.toUpperCase());
            ItemStack item = new ItemStack(mat);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                // Đặt tên và Lore
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        config.getString(id + ".display-name", id)));

                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList(id + ".lore")) {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);

                // Đặt Model ID (nếu có)
                if (config.contains(id + ".model-id")) {
                    meta.setCustomModelData(config.getInt(id + ".model-id"));
                }

                // LƯU PDC: Để biết đây là đá gì và loại gì
                NamespacedKey typeKey = new NamespacedKey(Main.getInstance(), "gem_item_type");
                NamespacedKey idKey = new NamespacedKey(Main.getInstance(), "gem_item_id");

                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, itemTag);
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);

                item.setItemMeta(meta);
            }
            return item;
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Lỗi khi tạo đá gemstone: " + id);
            return null;
        }
    }
    private void sendHelp(CommandSender sender, int page) {
        List<String> helpLines = new ArrayList<>();
        String miPrefix = "§8[§4§l?§8]§3 /myitem ";
        String basicPrefix = "§8[§4§l?§8]§3 /";
        String rpgPrefix = "§8[§4§l?§8]§3 /rpginv ";

        // Trang 1: Quản lý và Chỉ số
        helpLines.add(miPrefix + "save <id> §7- Lưu item vào database");
        helpLines.add(miPrefix + "load <id> §7- Lấy item từ database");
        helpLines.add(miPrefix + "delete <id> §7- Xóa item database");
        helpLines.add(miPrefix + "stats <loại> <giá trị> §7- Chỉnh chỉ số");
        helpLines.add(miPrefix + "element <id> <lv> §7- Cường hóa nguyên tố");

        // Trang 2: Kỹ năng và Hiệu ứng
        helpLines.add(miPrefix + "ability <tên> <lv> <%> §7- Gán nội tại");
        helpLines.add(miPrefix + "buff <tên> <lv> §7- Gán hiệu ứng tốt");
        helpLines.add(miPrefix + "debuff <tên> <lv> §7- Gán hiệu ứng xấu");
        helpLines.add(miPrefix + "skill <type> <tên> <trig> <cd> <lv>");
        helpLines.add(miPrefix + "unskill <tên> §7- Gỡ kỹ năng khỏi item");

        // Trang 3: Chỉnh sửa cơ bản
        helpLines.add(basicPrefix + "setname <tên> §7- Đổi tên vật phẩm");
        helpLines.add(basicPrefix + "setlore <line> <text> §7- Sửa lore");
        helpLines.add(basicPrefix + "material <loại> §7- Đổi vật liệu");
        helpLines.add(basicPrefix + "setmodel <id> §7- Đặt CustomModelData");
        helpLines.add(basicPrefix + "unbreaking §7- Làm item không hỏng");

        // Trang 4: Thuộc tính và Gemstone hệ cũ
        helpLines.add(basicPrefix + "attribute <attr> <val> §7- Thuộc tính gốc");
        helpLines.add(basicPrefix + "itemflag <flag> §7- Ẩn flags vật phẩm");
        helpLines.add(miPrefix + "enchant <enchant> <level> §7- Enchant item");
        helpLines.add(miPrefix + "gemstone give <typegem> <id> <p> <amt> §7- Gem cũ");
        helpLines.add(miPrefix + "reload §7- Nạp lại toàn bộ config");

        // Trang 5: Hệ thống Cường hóa & Trang sức (Cập nhật mới)
        helpLines.add(miPrefix + "upgrade §7- Mở giao diện Cường Hóa");
        helpLines.add(miPrefix + "trans §7- Mở giao diện Chuyển Hóa Cấp Độ");
        helpLines.add(miPrefix + "givegem <id> §7- lấy đá cường hoá");
        helpLines.add(miPrefix + "giveamulet §7- Lấy bùa hộ mệnh");

        helpLines.add(rpgPrefix + "§7- Mở kho đồ trang sức cá nhân");
        helpLines.add(rpgPrefix + "type <id> §7- Gán loại trang sức cầm tay");
        helpLines.add(rpgPrefix + "untype §7- Xóa loại trang sức khỏi item");
        helpLines.add(miPrefix + "connect <mã> §7- lấy item đã edit từ web §e(premium)");
        helpLines.add(miPrefix + "editor <mã> §7- lấy link edit item §e(premium)");
        helpLines.add(miPrefix + "sync addcode §7- thêm mã code cho item cầm tay và biến nó thành dạng có thể sửa mọi noi ");
        helpLines.add(miPrefix + "sync clear §7- xoá mã station item cầm tay  ");
        helpLines.add(miPrefix + "sync update §7- cập nhật chỉ số , lore các item có cùng mã station khác ");
        helpLines.add(miPrefix + "sync check §7- kiểm tra mã station và version item ");
        helpLines.add(miPrefix + "update §7- tải các bản mới của myitem mà không cần lên web tìm hay đọc các cập nhật ngay trong game ");
        helpLines.add(miPrefix + "version §7- check phiên bản hiện tại ");
        helpLines.add(miPrefix + "tiers §7- gán phẩm chất cho item");
        helpLines.add(miPrefix + "particle §7- gắn các hạt particle cho item");
        helpLines.add(miPrefix + "unparticle §7- gỡ particle");
        helpLines.add(miPrefix + "consume §7- Vật phẩm tiêu thụ");
        helpLines.add(miPrefix + "tooltip §7- Thêm tooltip cho item cầm tay (cần resourcepack)");
        helpLines.add(miPrefix + "ic §7- Thêm item đi theo combo chỉnh trong ItemCombo.yml");
        helpLines.add(miPrefix + "evo <entity> <amount> <evoDatabase> §7- Tiến hoá cho item");
        helpLines.add(miPrefix + "getai <id>  §7- Nhận Item từ AI trong Item.yml §c§lNEW");
        helpLines.add(miPrefix + "ai <profile> §7- Tạo item từ AI §c§lNEW");

        int itemsPerPage = 5;
        int maxPages = (int) Math.ceil((double) helpLines.size() / itemsPerPage);

        if (page < 1 || page > maxPages) page = 1;

        sender.sendMessage("§7[§b◀§7]§8§m ------§7[§2 Page§f (§d" + page + "/" + maxPages + "§f) §7]§8§m ------§7 [§b▶§7]");

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, helpLines.size());

        for (int i = start; i < end; i++) {
            sender.sendMessage(helpLines.get(i));
        }

        // Thêm dòng giới thiệu tác giả và ghi chú
        sender.sendMessage("§7[§b◀§7]§8§m ----------------------------§7 [§b▶§7]");
        sender.sendMessage("§bDiscord:§f cache1236799");

    }
}