package org.ThienNguyen.Command;

import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.Listener.AIExperienceGUI;
import org.ThienNguyen.Listener.Station.StationCMD;
import org.ThienNguyen.Main;
import org.ThienNguyen.Effect.BuffData;
import org.ThienNguyen.Command.ItemStorageManager;
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
import org.bukkit.configuration.file.YamlConfiguration;
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

                FileConfiguration config = Main.getInstance().getAIConfig();


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

                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§c[MyItem] Bạn không có quyền!");
                    return true;
                }


                if (args.length < 2) {
                    sender.sendMessage("§cSử dụng: /mi load <id> [player]");
                    return true;
                }

                String id = args[1];


                Player target;
                if (args.length >= 3) {

                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage("§cNgười chơi §f" + args[2] + " §ckhông online!");
                        return true;
                    }
                } else {

                    if (sender instanceof Player p) {
                        target = p;
                    } else {
                        sender.sendMessage("§cConsole phải nhập tên người nhận: /mi load <id> <player>");
                        return true;
                    }
                }


                ItemStack loadedItem = Main.getInstance().getItemDatabase().loadItem(id);

                if (loadedItem != null) {

                    target.getInventory().addItem(loadedItem);


                    sender.sendMessage("§a[MyItem] Successfully sent item §f" + id + " §ato §e" + target.getName());

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

                String type = args[1].toLowerCase();
                String elementId = args[2].toUpperCase();


                if (!Main.getInstance().getElementConfig().contains(elementId)) {
                    player.sendMessage("§cNguyên tố §f" + elementId + " §ckhông tồn tại!");
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[3]);

                    if (type.equals("attack")) {

                        org.ThienNguyen.Element.ElementCore.addElement(item, elementId, level);
                        player.sendMessage("§a[MyItem] Đã thêm §6Tấn công " + elementId + " §acấp §e" + level);
                    }
                    else if (type.equals("defense")) {

                        org.ThienNguyen.Element.ElementCore.addDefenseElement(item, elementId, level);
                        player.sendMessage("§b[MyItem] Đã thêm §3Phòng thủ " + elementId + " §bcấp §e" + level);
                    }
                    else {
                        player.sendMessage("§cLoại không hợp lệ! Sử dụng 'attack' hoặc 'defense'.");
                        return true;
                    }


                    org.ThienNguyen.Lore.ElementLore.updateLore(item);
                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);

                } catch (NumberFormatException e) {
                    player.sendMessage("§cLevel phải là một số nguyên!");
                }
            }

            case "stats" -> {
                if (!(sender instanceof Player player)) return true;


                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /mi stats <loại> <giá trị> [any/mainhand/offhand/head/chest/legs/feet]");
                    return true;
                }


                String slot = (args.length >= 4) ? args[3].toLowerCase() : "any";

                statsHandler.handleCommand(player, args, slot);
                org.ThienNguyen.Listener.CacheListener.refreshCache(player);

            }
            case "evo" -> {
                if (!(sender instanceof Player player)) return true;




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


                if (Main.getInstance().getItemDatabase().loadItem(nextId) == null) {
                    player.sendMessage("§cID '" + nextId + "' không tồn tại trong Item Database!");
                    return true;
                }


                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();

                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.TARGET_KEY, org.bukkit.persistence.PersistentDataType.STRING, target);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.CURRENT_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.REQUIRED_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, required);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.NEXT_ID_KEY, org.bukkit.persistence.PersistentDataType.STRING, nextId);

                item.setItemMeta(meta);


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



                new org.ThienNguyen.Utils.GUI().openUpgrade(player);

                return true;
            }
            case "givegem" -> {

                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                    return true;
                }





                if (args.length < 4) {
                    sender.sendMessage("§6§l[!] §7Sử dụng: §e/myitem givegem <id> <player> <amount>");
                    return true;
                }

                try {
                    int id = Integer.parseInt(args[1]);
                    Player targetPlayer = Bukkit.getPlayer(args[2]);
                    int amount = Integer.parseInt(args[3]);


                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        sender.sendMessage("§c§l✘ §7Người chơi §e" + args[2] + " §7không trực tuyến!");
                        return true;
                    }


                    ItemStack gem = org.ThienNguyen.Utils.Upgrade.createGemFromConfig(id);

                    if (gem == null) {
                        sender.sendMessage("§c§l✘ §7Không tìm thấy đá có ID §e" + id + " §7trong file §nUpgrade/Gem.yml");
                        return true;
                    }


                    gem.setAmount(amount);


                    Map<Integer, ItemStack> overFlow = targetPlayer.getInventory().addItem(gem);
                    if (!overFlow.isEmpty()) {
                        for (ItemStack left : overFlow.values()) {
                            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), left);
                        }
                        targetPlayer.sendMessage(lang.getMessage("upgrade.gem-fullinventory", "{gem}"));
                    }

                    String gemName = gem.getItemMeta().hasDisplayName() ? gem.getItemMeta().getDisplayName() : gem.getType().name();


                    targetPlayer.sendMessage(lang.getMessage("upgrade.gem-received", "{gem}", gemName));


                    sender.sendMessage("§a§l✔ §7Đã gửi §e" + amount + "x " + gemName + " §7cho §f" + targetPlayer.getName());

                } catch (NumberFormatException e) {
                    sender.sendMessage("§c§l✘ §7ID hoặc Số lượng không hợp lệ!");
                }
                return true;
            }
            case "giveamulet" -> {

                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                    return true;
                }


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


                    ItemStack amulet = org.ThienNguyen.Utils.Upgrade.createProtectionScroll();

                    if (amulet == null) {
                        sender.sendMessage("§c§l✘ §7Lỗi: Không tìm thấy cấu hình bùa trong §nUpgrade/protection.yml");
                        return true;
                    }

                    amulet.setAmount(amount);


                    if (target.getInventory().firstEmpty() == -1) {
                        target.getWorld().dropItemNaturally(target.getLocation(), amulet);
                    } else {
                        target.getInventory().addItem(amulet);
                    }

                    sender.sendMessage(lang.getMessage("upgrade.amulet-sent",
                            "{amount}", String.valueOf(amount),
                            "{player}", target.getName()));


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


                NamespacedKey skillKey = new NamespacedKey(Main.getInstance(), "item_skills");
                NamespacedKey loreStartKey = new NamespacedKey(Main.getInstance(), "skill_lore_start");
                NamespacedKey loreEndKey = new NamespacedKey(Main.getInstance(), "skill_lore_end");


                String oldData = meta.getPersistentDataContainer().get(skillKey, PersistentDataType.STRING);
                if (oldData == null || oldData.trim().isEmpty()) {
                    player.sendMessage("§cVật phẩm này không có kỹ năng nào để gỡ!");
                    return true;
                }


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


                if (meta.hasLore()) {
                    List<String> lore = new ArrayList<>(meta.getLore());
                    Integer oldStart = meta.getPersistentDataContainer().get(loreStartKey, PersistentDataType.INTEGER);
                    Integer oldEnd = meta.getPersistentDataContainer().get(loreEndKey, PersistentDataType.INTEGER);


                    if (oldStart != null && oldEnd != null && oldStart >= 0 && oldEnd >= oldStart && oldEnd < lore.size()) {
                        for (int i = oldEnd; i >= oldStart; i--) {
                            lore.remove(i);
                        }
                        meta.setLore(lore);
                    }
                }


                if (skillList.isEmpty()) {
                    meta.getPersistentDataContainer().remove(skillKey);

                    meta.getPersistentDataContainer().remove(loreStartKey);
                    meta.getPersistentDataContainer().remove(loreEndKey);
                } else {
                    String newData = String.join(",", skillList);
                    meta.getPersistentDataContainer().set(skillKey, PersistentDataType.STRING, newData);
                }

                item.setItemMeta(meta);



                org.ThienNguyen.Lore.SkillLore.updateLore(item);

                player.sendMessage("§a[Skill] Đã gỡ kỹ năng §f" + skillToRemove + " §athành công!");
            }

            case "enchant" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 3) {
                    player.sendMessage("§cSử dụng: /mi enchant <tên> <level>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();

                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(args[1].toLowerCase());
                org.bukkit.enchantments.Enchantment enchant = org.bukkit.enchantments.Enchantment.getByKey(key);

                if (enchant == null) {
                    player.sendMessage("§cKhông tìm thấy Enchant có tên: " + args[1]);
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[2]);

                    item.addUnsafeEnchantment(enchant, level);


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

                        FileConfiguration removerConfig = Main.getInstance().getGemConfig();


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


                net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent("§a[MyItem] §fTruy cập trang thiết kế tại: ");

                net.md_5.bungee.api.chat.TextComponent linkComponent = new net.md_5.bungee.api.chat.TextComponent("§b§n" + link);


                linkComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.ComponentBuilder("§eClick để sao chép link thiết kế!").create()
                ));


                linkComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.COPY_TO_CLIPBOARD,
                        link
                ));

                message.addExtra(linkComponent);


                player.spigot().sendMessage(message);
            }
            case "expire" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cChỉ người chơi mới có thể dùng lệnh này!");
                    return true;
                }
                if (!player.hasPermission("myitem.admin")) {
                    player.sendMessage("§cBạn không có quyền!");
                    return true;
                }

                FileConfiguration expConfig = Main.getInstance().setupConfig("Listener/Expire.yml");

                if (args.length < 2) {
                    player.sendMessage("§6§l[!] §7Sử dụng: §e/myitem expire <thời gian>");
                    player.sendMessage("§7Định dạng: §fmo§7(tháng), §fd§7(ngày), §fh§7(giờ), §fm§7(phút), §fs§7(giây)");
                    player.sendMessage("§7Ví dụ: §e/myitem expire 1h 5m 2s §7hoặc §e/myitem expire 2d");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(expConfig.getString("messages.no-item", "§c[!] Bạn phải cầm vật phẩm trên tay!"));
                    return true;
                }


                long durationMs = org.ThienNguyen.Listener.Expire.parseDuration(args, 1);

                if (durationMs <= 0) {
                    player.sendMessage(expConfig.getString("messages.invalid-format", "§c[!] Định dạng thời gian không hợp lệ!"));
                    return true;
                }

                long finalExpiryTimestamp = System.currentTimeMillis() + durationMs;

                var meta = item.getItemMeta();
                if (meta != null) {

                    meta.getPersistentDataContainer().set(
                            org.ThienNguyen.Listener.Expire.getInstance().getExpireKey(),
                            PersistentDataType.LONG,
                            finalExpiryTimestamp
                    );
                    item.setItemMeta(meta);


                    org.ThienNguyen.Lore.LoreGenerator.rebuild(item);

                    String successMsg = expConfig.getString("messages.success-applied", "§a[✓] Đã gắn hạn sử dụng thành công!");


                    StringBuilder timeVisual = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        timeVisual.append(args[i]).append(" ");
                    }
                    player.sendMessage(successMsg.replace("{time}", timeVisual.toString().trim()));


                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                }
                return true;
            }
            case "consume" -> {

                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền sử dụng lệnh này!");
                    return true;
                }


                if (args.length < 5) {
                    sender.sendMessage("§cSử dụng: /myitem consume give <id> <amount> <player>");
                    return true;
                }

                if (args[1].equalsIgnoreCase("give")) {
                    String consumeId = args[2];


                    int amount;
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cSố lượng phải là một con số!");
                        return true;
                    }


                    Player target = Bukkit.getPlayer(args[4]);
                    if (target == null) {
                        sender.sendMessage("§cNgười chơi §f" + args[4] + " §ckhông trực tuyến!");
                        return true;
                    }


                    ItemStack consumeItem = org.ThienNguyen.Consume.ConsumeManager.getConsumeItem(consumeId, amount);

                    if (consumeItem == null) {
                        sender.sendMessage("§cID vật phẩm §f" + consumeId + " §ckhông tồn tại trong Consume.yml!");
                        return true;
                    }


                    target.getInventory().addItem(consumeItem);
                    sender.sendMessage("§aĐã gửi §f" + amount + "x " + consumeId + " §acho §e" + target.getName());
                }
            }
            case "connect" -> {

                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
                    return true;
                }


                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cLệnh này chỉ dành cho người chơi trong game!");
                    return true;
                }


                if (args.length < 2) {
                    player.sendMessage("§8[§4§l?§8] §cSử dụng: /myitem connect <mã_web>");
                    return true;
                }


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

                if (!Main.getInstance().getParticleConfig().contains("effects." + particleId)) {
                    player.sendMessage("§e[!] Cảnh báo: ID '" + particleId + "' chưa được định nghĩa trong Particle.yml");
                }

                var meta = item.getItemMeta();
                if (meta != null) {

                    meta.getPersistentDataContainer().set(
                            new NamespacedKey(Main.getInstance(), "item_particle"),
                            PersistentDataType.STRING,
                            particleId
                    );
                    item.setItemMeta(meta);
                    player.sendMessage("§a[✓] Đã thêm hiệu ứng '" + particleId + "' vào vật phẩm!");


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


                    if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        player.sendMessage("§c[!] Vật phẩm này không có hiệu ứng particle nào để xóa.");
                        return true;
                    }


                    meta.getPersistentDataContainer().remove(key);
                    item.setItemMeta(meta);

                    player.sendMessage("§e[✓] Đã gỡ bỏ hiệu ứng particle khỏi vật phẩm trên tay!");


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

                        org.ThienNguyen.Utils.Tooltips.applyTooltip(player, type);
                    }
                    case "undo" -> {

                        org.ThienNguyen.Utils.Tooltips.handleUndo(player);
                    }
                    default -> player.sendMessage("§c[!] Hành động không hợp lệ. Sử dụng 'add' hoặc 'undo'.");
                }
            }
            case "update" -> {

                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage("§c[MyItem] Bạn không có quyền!");
                    return true;
                }


                if (args.length < 2) {
                    if (sender instanceof Player player) {
                        org.ThienNguyen.Webapi.Update.openUpdateListGUI(player, plugin);
                    } else {
                        sender.sendMessage("§cConsole phải nhập rõ version: /myitem update <version>");
                    }
                    return true;
                }


                String updateVersion = args[1];

                if (sender instanceof Player player) {

                    org.ThienNguyen.Webapi.Update.downloadAndUpdate(plugin, updateVersion, player);
                } else {


                    sender.sendMessage("§e[MyItem] Console đang tải bản " + updateVersion + " (Xem log tại Console)...");

                }
                return true;
            }
            case "sync" -> {

                return new StationCMD(plugin, stationDb).onCommand(sender, command, label, args);
            }
            case "tiers" -> {
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


                org.ThienNguyen.Lore.TiersLore.applyTier(itemTier, args[1].toLowerCase());
                pTiers.sendMessage("§a§l✔ §7Đã cập nhật phẩm chất vật phẩm!");
                break;
            }
            case "checkitem" -> {
                if (!(sender instanceof Player player)) return true;

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage("§c[MyItem] Hãy cầm một vật phẩm trên tay để kiểm tra!");
                    return true;
                }

                player.sendMessage("§8§m----------------§7[ §bCHECK ITEM §7]§8§m----------------");
                player.sendMessage("§2➤ Type/Material: §f" + item.getType().name());

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (meta.hasCustomModelData()) {
                        player.sendMessage("§2➤ Custom Model ID: §e" + meta.getCustomModelData());
                    } else {
                        player.sendMessage("§2➤ Custom Model ID: §7Không có");
                    }

                    player.sendMessage("§2➤ Persistent Data (PDC):");
                    var pdc = meta.getPersistentDataContainer();
                    var keys = pdc.getKeys();

                    if (keys.isEmpty()) {
                        player.sendMessage("  §7§o(Không có dữ liệu PDC ẩn trên vật phẩm này)");
                    } else {
                        for (NamespacedKey key : keys) {
                            String valueStr = "§7[Unknown Type]";

                            if (pdc.has(key, PersistentDataType.STRING)) {
                                valueStr = "§6(String) §f\"" + pdc.get(key, PersistentDataType.STRING) + "\"";
                            } else if (pdc.has(key, PersistentDataType.DOUBLE)) {
                                valueStr = "§a(Double) §f" + pdc.get(key, PersistentDataType.DOUBLE);
                            } else if (pdc.has(key, PersistentDataType.INTEGER)) {
                                valueStr = "§b(Integer) §f" + pdc.get(key, PersistentDataType.INTEGER);
                            } else if (pdc.has(key, PersistentDataType.FLOAT)) {
                                valueStr = "§d(Float) §f" + pdc.get(key, PersistentDataType.FLOAT);
                            } else if (pdc.has(key, PersistentDataType.LONG)) {
                                valueStr = "§e(Long) §f" + pdc.get(key, PersistentDataType.LONG);
                            } else if (pdc.has(key, PersistentDataType.BYTE)) {
                                valueStr = "§5(Byte) §f" + pdc.get(key, PersistentDataType.BYTE);
                            } else if (pdc.has(key, PersistentDataType.SHORT)) {
                                valueStr = "§3(Short) §f" + pdc.get(key, PersistentDataType.SHORT);
                            }

                            player.sendMessage("  §b• §7" + key.toString() + " §7➔ " + valueStr);
                        }
                    }
                } else {
                    player.sendMessage("§c➤ ItemMeta: Không hợp lệ!");
                }
                player.sendMessage("§8§m------------------------------------------------");
            }
            case "storage" -> {
                if (args.length < 2) {
                    sender.sendMessage("§e/myitem mi create <type> §7- Tạo file yml mới");
                    sender.sendMessage("§e/myitem mi save <type> <id> §7- Lưu item vào file");
                    sender.sendMessage("§e/myitem mi load <type> <id> §7- Lấy item từ bộ nhớ tạm");
                    sender.sendMessage("§e/myitem mi reload §7- Nạp lại toàn bộ item từ folder");
                    sender.sendMessage("§e/myitem mi browse §7- Mở GUI duyệt item");
                    return true;
                }

                ItemStorageManager ism = plugin.getItemStorageManager();
                String action = args[1].toLowerCase();

                switch (action) {
                    case "browse" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage("§cChỉ player mới dùng được lệnh này!");
                            return true;
                        }
                        plugin.getMiBrowseGUI().openTypePage(p, 0);
                    }
                    case "create" -> {
                        if (args.length < 3) {
                            sender.sendMessage("§cVD: /myitem mi create Sword");
                            return true;
                        }
                        String type = args[2];
                        if (ism.createTypeFile(type)) {
                            sender.sendMessage("§a[MyItem] Đã tạo file: ManagerItem/" + type + ".yml");
                        } else {
                            sender.sendMessage("§cFile đã tồn tại hoặc không thể tạo.");
                        }
                    }
                    case "save" -> {
                        if (!(sender instanceof Player p)) return true;
                        if (args.length < 4) {
                            p.sendMessage("§cVD: /myitem mi save <type> <id>");
                            return true;
                        }

                        String type = args[2];
                        String id = args[3];
                        ItemStack item = p.getInventory().getItemInMainHand();

                        if (item.getType().isAir()) {
                            p.sendMessage("§cHãy cầm item trên tay để lưu!");
                            return true;
                        }

                        if (ism.saveItemToType(type, id, item)) {
                            p.sendMessage("§a[MyItem] Đã lưu item §f" + id + " §avào §e" + type + ".yml §a(Đã cập nhật bộ nhớ)");
                        } else {
                            p.sendMessage("§cFile " + type + ".yml không tồn tại. Hãy dùng 'create' trước!");
                        }
                    }

                    case "load" -> {
                        if (!(sender instanceof Player p)) return true;
                        if (args.length < 4) {
                            p.sendMessage("§cVD: /myitem mi load <type> <id>");
                            return true;
                        }

                        // args[2] = type (dùng để hiển thị thông báo rõ hơn, cache không phân biệt type)
                        String type = args[2];
                        String id = args[3];
                        ItemStack item = ism.getItem(id);

                        if (item != null) {
                            p.getInventory().addItem(item);
                            p.sendMessage("§a[MyItem] Đã lấy vật phẩm §f" + id + " §atừ §e" + type + ".yml §a(bộ nhớ tạm).");
                        } else {
                            p.sendMessage("§cKhông tìm thấy vật phẩm §f" + id + " §ctrong §e" + type + ".yml§c! Thử /myitem mi reload.");
                        }
                    }
                    case "reload" -> {
                        ism.loadAllItems();
                        sender.sendMessage("§a[MyItem] Đã nạp lại toàn bộ vật phẩm từ folder ManagerItem!");
                    }

                    default -> sender.sendMessage("§cKhông rõ hành động: " + action);
                }
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

                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        config.getString(id + ".display-name", id)));

                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList(id + ".lore")) {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);


                if (config.contains(id + ".model-id")) {
                    meta.setCustomModelData(config.getInt(id + ".model-id"));
                }


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


        helpLines.add(miPrefix + "save <id> §7- Lưu item vào database");
        helpLines.add(miPrefix + "load <id> §7- Lấy item từ database");
        helpLines.add(miPrefix + "delete <id> §7- Xóa item database");
        helpLines.add(miPrefix + "stats <loại> <giá trị> §7- Chỉnh chỉ số");
        helpLines.add(miPrefix + "element <id> <lv> §7- Cường hóa nguyên tố");


        helpLines.add(miPrefix + "ability <tên> <lv> <%> §7- Gán nội tại");
        helpLines.add(miPrefix + "buff <tên> <lv> §7- Gán hiệu ứng tốt");
        helpLines.add(miPrefix + "debuff <tên> <lv> §7- Gán hiệu ứng xấu");
        helpLines.add(miPrefix + "skill <type> <tên> <trig> <cd> <lv>");
        helpLines.add(miPrefix + "unskill <tên> §7- Gỡ kỹ năng khỏi item");


        helpLines.add(basicPrefix + "setname <tên> §7- Đổi tên vật phẩm");
        helpLines.add(basicPrefix + "setlore <line> <text> §7- Sửa lore");
        helpLines.add(basicPrefix + "material <loại> §7- Đổi vật liệu");
        helpLines.add(basicPrefix + "setmodel <id> §7- Đặt CustomModelData");
        helpLines.add(basicPrefix + "unbreaking §7- Làm item không hỏng");


        helpLines.add(basicPrefix + "attribute <attr> <val> §7- Thuộc tính gốc");
        helpLines.add(basicPrefix + "itemflag <flag> §7- Ẩn flags vật phẩm");
        helpLines.add(miPrefix + "enchant <enchant> <level> §7- Enchant item");
        helpLines.add(miPrefix + "gemstone give <typegem> <id> <p> <amt> §7- Gem cũ");
        helpLines.add(miPrefix + "reload §7- Nạp lại toàn bộ config");


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
        helpLines.add(miPrefix + "getai <id>  §7- Nhận Item từ AI trong Item.yml ");
        helpLines.add(miPrefix + "ai <profile> §7- Tạo item từ AI ");
        helpLines.add(miPrefix + "expire <time> §7- Thiết lập hạn sử dụng cho vật phẩm");
        helpLines.add(miPrefix + "storage <create/save/load/browse> §7- Quản lí item (ManagerItem)");
        helpLines.add(miPrefix + "checkitem §7- Kiểm tra data vật phẩm §c§lNEW");

        int itemsPerPage = 5;
        int maxPages = (int) Math.ceil((double) helpLines.size() / itemsPerPage);

        if (page < 1 || page > maxPages) page = 1;

        sender.sendMessage("§7[§b◀§7]§8§m ------§7[§2 Page§f (§d" + page + "/" + maxPages + "§f) §7]§8§m ------§7 [§b▶§7]");

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, helpLines.size());

        for (int i = start; i < end; i++) {
            sender.sendMessage(helpLines.get(i));
        }


        sender.sendMessage("§7[§b◀§7]§8§m ----------------------------§7 [§b▶§7]");
        sender.sendMessage("§bDiscord:§f cache1236799");

    }
}