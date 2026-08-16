package org.ThienNguyen.Command;

import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.Listener.AIExperienceGUI;
import org.ThienNguyen.Listener.Station.StationCMD;
import org.ThienNguyen.Main;
import org.ThienNguyen.Effect.BuffData;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

    // ── Prefix ───────────────────────────────────────────────────────────────
    // Palette: §8 dark-gray brackets · §b aqua plugin name · §7 gray body text
    // Icons  : §a✔ success · §c✖ error · §e⚠ warning
    private static final String PFX     = "§8[§bMyItem§8] §7";   // normal info
    private static final String PFX_OK  = "§8[§bMyItem§8] §a✔ §7"; // success
    private static final String PFX_ERR = "§8[§bMyItem§8] §c✖ §7"; // error
    private static final String PFX_WRN = "§8[§bMyItem§8] §e⚠ §7"; // warning

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
                        sender.sendMessage(PFX_ERR + "Page number must be an integer.");
                        return true;
                    }
                }
                sendHelp(sender, page);
            }

            case "loreformat" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 2) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi loreformat <id>");
                    return true;
                }

                String formatId = args[1];
                ItemStack item = player.getInventory().getItemInMainHand();

                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                if (!plugin.getLoreFormatConfig().contains(formatId)) {
                    player.sendMessage(PFX_ERR + "Format ID §f" + formatId + " §7does not exist.");
                    return true;
                }

                ItemMeta meta = item.getItemMeta();
                NamespacedKey key = new NamespacedKey(plugin, "lore_format_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, formatId);
                item.setItemMeta(meta);

                org.ThienNguyen.Lore.LoreGenerator.rebuild(item);

                player.sendMessage(PFX_OK + "Lore format §e" + formatId + " §7applied successfully.");
                return true;
            }

            case "ic" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;

                if (args.length >= 2) {
                    String subAction = args[1].toLowerCase();
                    ItemStack item = player.getInventory().getItemInMainHand();

                    if (item == null || item.getType().isAir()) {
                        player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                        return true;
                    }

                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return true;

                    switch (subAction) {
                        case "add" -> {
                            if (args.length < 3) {
                                player.sendMessage(PFX_ERR + "Usage: §f/mi ic add <id>");
                                return true;
                            }
                            String id = args[2];
                            meta.getPersistentDataContainer().set(COMBO_KEY, PersistentDataType.STRING, id);
                            item.setItemMeta(meta);
                            player.sendMessage(PFX_OK + "Combo ID §e" + id + " §7bound to item.");
                        }
                        case "unadd" -> {
                            if (meta.getPersistentDataContainer().has(COMBO_KEY, PersistentDataType.STRING)) {
                                meta.getPersistentDataContainer().remove(COMBO_KEY);
                                item.setItemMeta(meta);
                                player.sendMessage(PFX_OK + "Combo ID removed from item.");
                            } else {
                                player.sendMessage(PFX_WRN + "This item has no Combo ID attached.");
                            }
                        }
                        default -> player.sendMessage(PFX_ERR + "Usage: §f/mi ic <add|unadd> [id]");
                    }
                } else {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi ic <add|unadd> [id]");
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
                    player.sendMessage(PFX_ERR + "Section §eai.profiles §7not found in AIConfig.yml.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(PFX + "Usage: §f/myitem ai <id> §7— Choose a profile.");
                    player.sendMessage(PFX + "Available: §b" + String.join(", ", profilesSection.getKeys(false)));
                    return true;
                }

                String id = args[1].toLowerCase();
                ConfigurationSection profile = profilesSection.getConfigurationSection(id);

                if (profile == null) {
                    player.sendMessage(PFX_ERR + "Profile §f" + id + " §7does not exist.");
                    player.sendMessage(PFX + "Suggestions: §e" + String.join(", ", profilesSection.getKeys(false)));
                    return true;
                }

                player.sendMessage(PFX_OK + "Selected profile: §e" + id);
                player.sendMessage(PFX + profile.getString("description", "No description available."));
                player.sendMessage(PFX + "Type your item description in chat §7(or §fcancel§7):");
                player.sendMessage(PFX_WRN + "Stat variables like §fnormal§7/§flegend §7are placeholders — they have no functional effect.");

                player.setMetadata("ai_prompt_mode", new FixedMetadataValue(Main.getInstance(), id));

                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    if (player.hasMetadata("ai_prompt_mode")) {
                        player.removeMetadata("ai_prompt_mode", Main.getInstance());
                        player.sendMessage(PFX_ERR + "Prompt input timed out.");
                    }
                }, 2400L);
            }

            case "getai" -> {
                if (!(sender instanceof Player player)) return true;
                if (!player.hasPermission("myitem.admin")) {
                    player.sendMessage(PFX_ERR + "You do not have permission to retrieve AI items.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi getai <id>");
                    return true;
                }

                String id = args[1];
                ItemStack aiItem = org.ThienNguyen.AI.utils.YamlManager.getItemFromAiFolder(id);

                if (aiItem != null) {
                    player.getInventory().addItem(aiItem);
                    player.sendMessage(PFX_OK + "Retrieved item §e" + id + " §7from the AI storage.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1.5f);
                } else {
                    player.sendMessage(PFX_ERR + "Item §f" + id + " §7not found in §nAI/Item.yml§7.");
                }
            }

            case "save" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 2) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi save <id>");
                    return true;
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }
                String id = args[1];
                Main.getInstance().getItemDatabase().saveItem(id, item);
                player.sendMessage(PFX_OK + "Item saved to database with ID: §f" + id);
            }

            case "load" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(PFX_ERR + "Usage: §f/mi load <id> [player]");
                    return true;
                }

                String id = args[1];
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(PFX_ERR + "Player §f" + args[2] + " §7is not online.");
                        return true;
                    }
                } else {
                    if (sender instanceof Player p) {
                        target = p;
                    } else {
                        sender.sendMessage(PFX_ERR + "Console must specify a target: §f/mi load <id> <player>");
                        return true;
                    }
                }

                ItemStack loadedItem = Main.getInstance().getItemDatabase().loadItem(id);
                if (loadedItem != null) {
                    target.getInventory().addItem(loadedItem);
                    sender.sendMessage(PFX_OK + "Sent item §f" + id + " §7to §e" + target.getName() + "§7.");
                    if (target != sender) {
                        target.sendMessage(lang.getMessage("item.receive-msg", "{id}", id));
                    }
                } else {
                    sender.sendMessage(PFX_ERR + "No item found with ID: §f" + id);
                }
            }

            case "delete" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 2) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi delete <id>");
                    return true;
                }
                String id = args[1];
                if (Main.getInstance().getItemDatabase().loadItem(id) == null) {
                    player.sendMessage(PFX_ERR + "No item found with ID: §f" + id);
                    return true;
                }
                Main.getInstance().getItemDatabase().deleteItem(id);
                player.sendMessage(PFX_OK + "Item §f" + id + " §7deleted from database.");
            }

            case "element" -> {
                if (!(sender instanceof Player player)) return true;

                if (args.length < 4) {
                    player.sendMessage(PFX + "Usage:");
                    player.sendMessage("  §f/mi element attack <id> <level> §7— Add attack element");
                    player.sendMessage("  §f/mi element defense <id> <level> §7— Add defense element");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                String type = args[1].toLowerCase();
                String elementId = args[2].toUpperCase();

                if (!Main.getInstance().getElementConfig().contains(elementId)) {
                    player.sendMessage(PFX_ERR + "Element §f" + elementId + " §7does not exist.");
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[3]);
                    if (type.equals("attack")) {
                        org.ThienNguyen.Element.ElementCore.addElement(item, elementId, level);
                        player.sendMessage(PFX_OK + "Added §6Attack §7element §e" + elementId + " §7Lv.§e" + level);
                    } else if (type.equals("defense")) {
                        org.ThienNguyen.Element.ElementCore.addDefenseElement(item, elementId, level);
                        player.sendMessage(PFX_OK + "Added §bDefense §7element §e" + elementId + " §7Lv.§e" + level);
                    } else {
                        player.sendMessage(PFX_ERR + "Invalid type. Use §fattack §7or §fdefense§7.");
                        return true;
                    }
                    org.ThienNguyen.Lore.ElementLore.updateLore(item);
                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                } catch (NumberFormatException e) {
                    player.sendMessage(PFX_ERR + "Level must be an integer.");
                }
            }

            case "stats" -> {
                if (!(sender instanceof Player player)) return true;

                if (args.length < 3) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi stats <type> <value> [any/mainhand/offhand/head/chest/legs/feet]");
                    return true;
                }

                String slot = (args.length >= 4) ? args[3].toLowerCase() : "any";
                statsHandler.handleCommand(player, args, slot);
                org.ThienNguyen.Listener.CacheListener.refreshCache(player);
            }
            case "owner-tag" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;

                if (args.length < 2) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi owner-tag <player1,player2,...>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                String owners = args[1].toLowerCase().trim();
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return true;

                NamespacedKey ownerKey = new NamespacedKey(plugin, "owner_tag");
                meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owners);
                item.setItemMeta(meta);

                org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
                org.ThienNguyen.Listener.CacheListener.refreshCache(player);

                player.sendMessage(PFX_OK + "Applied owner tag(s): §e" + owners);
                return true;
            }

            case "del-tag" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                ItemMeta meta = item.getItemMeta();
                if (meta == null) return true;

                NamespacedKey ownerKey = new NamespacedKey(plugin, "owner_tag");
                if (!meta.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
                    player.sendMessage(PFX_WRN + "This item has no owner tag.");
                    return true;
                }

                meta.getPersistentDataContainer().remove(ownerKey);
                item.setItemMeta(meta);

                org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
                org.ThienNguyen.Listener.CacheListener.refreshCache(player);

                player.sendMessage(PFX_OK + "Removed owner tag from item.");
                return true;
            }
            case "evo" -> {
                if (!(sender instanceof Player player)) return true;

                if (args.length < 4) {
                    player.sendMessage(PFX + "Usage: §f/mi evo <target|ALL> <amount> <new_item_id>");
                    player.sendMessage(PFX + "§7- §ftarget §7: Mob name (§eZOMBIE§7, §eSKELETON§7...) or MythicMobs ID.");
                    player.sendMessage(PFX + "§7- §fALL §7: Any kill counts.");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                String target = args[1];
                int required;
                try {
                    required = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(PFX_ERR + "Amount must be a number.");
                    return true;
                }
                String nextId = args[3];

                if (Main.getInstance().getItemDatabase().loadItem(nextId) == null) {
                    player.sendMessage(PFX_ERR + "ID '§f" + nextId + "§7' does not exist in the Item Database.");
                    return true;
                }

                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();

                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.TARGET_KEY, org.bukkit.persistence.PersistentDataType.STRING, target);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.CURRENT_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.REQUIRED_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, required);
                pdc.set(org.ThienNguyen.Evolution.EvolutionManager.NEXT_ID_KEY, org.bukkit.persistence.PersistentDataType.STRING, nextId);

                item.setItemMeta(meta);
                org.ThienNguyen.Evolution.EvolutionManager.addProgress(player, item, "INITIALIZE_ONLY");

                player.sendMessage(PFX_OK + "Evolution configured for item.");
                player.sendMessage(PFX + "Target  : §e" + target);
                player.sendMessage(PFX + "Kills   : §e" + required);
                player.sendMessage(PFX + "Evolves → §b" + nextId);
                return true;
            }

            case "ability" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 4) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi ability <name> <level> <chance%>");
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
                    player.sendMessage(PFX_OK + "Ability §f" + abilityName + " §7Lv.§e" + level + " §7(§b" + chance + "%§7) applied.");
                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                } catch (NumberFormatException e) {
                    player.sendMessage(PFX_ERR + "Invalid number format.");
                }
            }

            case "buff", "debuff" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 3) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi " + subCommand + " <effect_name> <level>");
                    return true;
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) return true;
                String effectName = args[1].toUpperCase();
                PotionEffectType type = PotionEffectType.getByName(effectName);
                if (type == null) {
                    player.sendMessage(PFX_ERR + "Effect §f" + effectName + " §7does not exist.");
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[2]);
                    BuffData.setEffect(item, effectName, level);
                    EffectLore.updateLore(item);
                    boolean isBuff = subCommand.equals("buff");
                    String tag = isBuff ? "§aBuff" : "§cDebuff";
                    player.sendMessage(PFX_OK + tag + " §f" + effectName + " §7Lv.§e" + level + " §7applied.");
                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                } catch (NumberFormatException e) {
                    player.sendMessage(PFX_ERR + "Level must be an integer.");
                }
            }

            case "skill" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;
                if (args.length < 6) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi skill <type> <name> <trigger> <cooldown> <level>");
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
                        player.sendMessage(PFX_ERR + "Skill §f" + skillName + " §7does not exist.");
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

                        player.sendMessage(PFX_OK + "Skill §b" + skill.getName() + " §7added to item.");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(PFX_ERR + "Invalid number format.");
                }
            }

            case "upgrade" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PFX_ERR + "This command can only be used by players.");
                    return true;
                }
                new org.ThienNguyen.Utils.GUI().openUpgrade(player);
                return true;
            }

            case "givegem" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(PFX_ERR + "Usage: §f/myitem givegem <id> <player> <amount>");
                    return true;
                }

                try {
                    int id = Integer.parseInt(args[1]);
                    Player targetPlayer = Bukkit.getPlayer(args[2]);
                    int amount = Integer.parseInt(args[3]);

                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        sender.sendMessage(PFX_ERR + "Player §e" + args[2] + " §7is not online.");
                        return true;
                    }

                    ItemStack gem = org.ThienNguyen.Utils.Upgrade.createGemFromConfig(id);
                    if (gem == null) {
                        sender.sendMessage(PFX_ERR + "Gem ID §e" + id + " §7not found in §nUpgrade/Gem.yml§7.");
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

                    String gemName = gem.getItemMeta().hasDisplayName()
                            ? gem.getItemMeta().getDisplayName()
                            : gem.getType().name();

                    targetPlayer.sendMessage(lang.getMessage("upgrade.gem-received", "{gem}", gemName));
                    sender.sendMessage(PFX_OK + "Sent §e" + amount + "x " + gemName + " §7to §f" + targetPlayer.getName() + "§7.");

                } catch (NumberFormatException e) {
                    sender.sendMessage(PFX_ERR + "Invalid ID or amount.");
                }
                return true;
            }

            case "giveamulet" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(PFX_ERR + "Usage: §f/myitem giveamulet <player> <amount>");
                    return true;
                }

                Player target = org.bukkit.Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(PFX_ERR + "Player not found: §f" + args[1]);
                    return true;
                }

                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) amount = 1;

                    ItemStack amulet = org.ThienNguyen.Utils.Upgrade.createProtectionScroll();
                    if (amulet == null) {
                        sender.sendMessage(PFX_ERR + "Protection scroll config not found in §nUpgrade/protection.yml§7.");
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
                    sender.sendMessage(PFX_ERR + "Amount §f" + args[2] + " §7must be an integer.");
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
                    player.sendMessage(PFX_ERR + "Usage: §f/mi unskill <skill_name>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
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
                    player.sendMessage(PFX_WRN + "This item has no skills to remove.");
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
                    player.sendMessage(PFX_ERR + "Skill §f" + skillToRemove + " §7not found on this item.");
                    return true;
                }

                if (meta.hasLore()) {
                    List<String> lore = new ArrayList<>(meta.getLore());
                    Integer oldStart = meta.getPersistentDataContainer().get(loreStartKey, PersistentDataType.INTEGER);
                    Integer oldEnd   = meta.getPersistentDataContainer().get(loreEndKey,   PersistentDataType.INTEGER);
                    if (oldStart != null && oldEnd != null && oldStart >= 0 && oldEnd >= oldStart && oldEnd < lore.size()) {
                        for (int i = oldEnd; i >= oldStart; i--) lore.remove(i);
                        meta.setLore(lore);
                    }
                }

                if (skillList.isEmpty()) {
                    meta.getPersistentDataContainer().remove(skillKey);
                    meta.getPersistentDataContainer().remove(loreStartKey);
                    meta.getPersistentDataContainer().remove(loreEndKey);
                } else {
                    meta.getPersistentDataContainer().set(skillKey, PersistentDataType.STRING, String.join(",", skillList));
                }

                item.setItemMeta(meta);
                org.ThienNguyen.Lore.SkillLore.updateLore(item);
                player.sendMessage(PFX_OK + "Skill §f" + skillToRemove + " §7removed from item.");
                return true;
            }

            case "enchant" -> {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 3) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi enchant <name> <level>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();

                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(args[1].toLowerCase());
                org.bukkit.enchantments.Enchantment enchant = org.bukkit.enchantments.Enchantment.getByKey(key);

                if (enchant == null) {
                    player.sendMessage(PFX_ERR + "Enchantment not found: §f" + args[1]);
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[2]);
                    item.addUnsafeEnchantment(enchant, level);
                    org.ThienNguyen.Enchant.EnchantVanila.updateEnchantLore(item);
                    player.sendMessage(PFX_OK + "Applied §e" + args[1] + " §7Lv.§f" + level + " §7to item.");
                } catch (NumberFormatException e) {
                    player.sendMessage(PFX_ERR + "Level must be a number.");
                }
            }

// Updated block for the "gemstone" case in your command handler.
// Only two things changed from your version:
//   1. Usage text / examples now mention "socket_remover" as a valid type.
//   2. A new switch branch reads SOCKET_REMOVER items out of Gem.yml,
//      the same way "remover" already does.

            case "gemstone" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }

                if (args.length < 5 || !args[1].equalsIgnoreCase("give")) {
                    sender.sendMessage(PFX_ERR + "Usage: §f/mi gemstone give <gem|drill|remover|socket_remover> <id> <player> <amount>");
                    sender.sendMessage(PFX + "Examples:");
                    sender.sendMessage("  §f/mi gemstone give gem ruby1 Steve 1");
                    sender.sendMessage("  §f/mi gemstone give drill drill_common Steve 5");
                    sender.sendMessage("  §f/mi gemstone give remover remover_basic Steve 10");
                    sender.sendMessage("  §f/mi gemstone give socket_remover socket_remover_legendary Steve 3");
                    return true;
                }

                String type = args[2].toLowerCase();
                String id = args[3];
                Player target = Bukkit.getPlayer(args[4]);
                int amount = 1;

                try {
                    if (args.length >= 6) amount = Integer.parseInt(args[5]);
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(PFX_ERR + "Amount must be an integer.");
                    return true;
                }

                if (target == null) {
                    sender.sendMessage(PFX_ERR + "Player not found: §f" + args[4]);
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
                        FileConfiguration typeConfig = Main.getInstance().getGemTypeConfig();
                        String drillPath = org.ThienNguyen.GemSocket.GemType.resolveDrillPath(id);
                        if (drillPath != null) {
                            // drillPath (e.g. "legendary.drills.DRILL_LEGENDARY") is used to read
                            // material/display-name/lore/model-id, but the item's stored gem_item_id
                            // must stay as the short `id`, since GemDucLo looks drills up by that id.
                            itemResult = createGemItem(id, drillPath, typeConfig, "DRILL");
                            itemTag = "DRILL";
                        } else {
                            sender.sendMessage(PFX_ERR + "Drill '" + id + "' was not found in type.yml.");
                        }
                    }
                    case "remover" -> {
                        FileConfiguration removerConfig = Main.getInstance().getGemConfig();
                        if (removerConfig.contains(id)) {
                            itemResult = createGemItem(id, removerConfig, "REMOVER");
                            itemTag = "REMOVER";
                        }
                    }
                    case "socket_remover" -> {
                        // Configured in Gem.yml exactly like "remover" — each entry needs
                        // a "type" field (a rarity, or "ANY") that GemThaoLo reads to
                        // decide which empty sockets this item is allowed to remove.
                        FileConfiguration socketRemoverConfig = Main.getInstance().getGemConfig();
                        if (socketRemoverConfig.contains(id)) {
                            itemResult = createGemItem(id, socketRemoverConfig, "SOCKET_REMOVER");
                            itemTag = "SOCKET_REMOVER";
                        }
                    }
                    default -> {
                        sender.sendMessage(PFX_ERR + "Invalid type. Use: §egem §7| §edrill §7| §eremover §7| §esocket_remover");
                        return true;
                    }
                }

                if (itemResult != null) {
                    itemResult.setAmount(amount);
                    target.getInventory().addItem(itemResult);
                    sender.sendMessage(PFX_OK + "Gave §e" + amount + "x §f" + id +
                            " §7(" + type.toUpperCase() + ") to §e" + target.getName() + "§7.");
                    target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
                } else {
                    sender.sendMessage(PFX_ERR + "ID §f" + id + " §7not found in type §f" + type + "§7.");
                }
                return true;
            }

            case "editor" -> {
                if (!(sender instanceof Player player)) return true;

                String link = "https://windycraft.com/editor";

                net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(
                        PFX + "Open the item designer at: ");

                net.md_5.bungee.api.chat.TextComponent linkComponent =
                        new net.md_5.bungee.api.chat.TextComponent("§b§n" + link);

                linkComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new net.md_5.bungee.api.chat.ComponentBuilder("§eClick to copy the designer link!").create()
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
                    sender.sendMessage(PFX_ERR + "This command can only be used by players.");
                    return true;
                }
                if (!player.hasPermission("myitem.admin")) {
                    player.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }

                FileConfiguration expConfig = Main.getInstance().setupConfig("Listener/Expire.yml");

                if (args.length < 2) {
                    player.sendMessage(PFX_ERR + "Usage: §f/myitem expire <time>");
                    player.sendMessage(PFX + "Format: §fmo§7(month) §fd§7(day) §fh§7(hour) §fm§7(min) §fs§7(sec)");
                    player.sendMessage(PFX + "Example: §f/myitem expire 1h 5m 2s §7or §f/myitem expire 2d");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(expConfig.getString("messages.no-item",
                            PFX_ERR + "You must be holding an item in your main hand."));
                    return true;
                }

                long durationMs = org.ThienNguyen.Listener.Expire.parseDuration(args, 1);

                if (durationMs <= 0) {
                    player.sendMessage(expConfig.getString("messages.invalid-format",
                            PFX_ERR + "Invalid time format."));
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

                    StringBuilder timeVisual = new StringBuilder();
                    for (int i = 1; i < args.length; i++) timeVisual.append(args[i]).append(" ");

                    String successMsg = expConfig.getString("messages.success-applied",
                            PFX_OK + "Expiry applied: {time}");
                    player.sendMessage(successMsg.replace("{time}", timeVisual.toString().trim()));
                    org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                }
                return true;
            }

            case "consume" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (args.length < 5) {
                    sender.sendMessage(PFX_ERR + "Usage: §f/myitem consume give <id> <amount> <player>");
                    return true;
                }

                if (args[1].equalsIgnoreCase("give")) {
                    String consumeId = args[2];
                    int amount;
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(PFX_ERR + "Amount must be a number.");
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[4]);
                    if (target == null) {
                        sender.sendMessage(PFX_ERR + "Player §f" + args[4] + " §7is not online.");
                        return true;
                    }

                    ItemStack consumeItem = org.ThienNguyen.Consume.ConsumeManager.getConsumeItem(consumeId, amount);
                    if (consumeItem == null) {
                        sender.sendMessage(PFX_ERR + "Item ID §f" + consumeId + " §7does not exist in §nConsume.yml§7.");
                        return true;
                    }

                    target.getInventory().addItem(consumeItem);
                    sender.sendMessage(PFX_OK + "Sent §f" + amount + "x " + consumeId + " §7to §e" + target.getName() + "§7.");
                }
            }

            case "connect" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PFX_ERR + "This command can only be used by players.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(PFX_ERR + "Usage: §f/myitem connect <web_code>");
                    return true;
                }
                String code = args[1].toUpperCase();
                org.ThienNguyen.Webapi.Web.connectItem(player, code);
            }

            case "reload" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                Main.getInstance().reloadPluginConfigs();
                sender.sendMessage(PFX_OK + "All configurations have been reloaded successfully.");
            }

            case "particle" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PFX_ERR + "This command can only be used by players.");
                    return true;
                }
                if (!player.hasPermission("myitem.admin")) {
                    player.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(PFX_ERR + "Usage: §f/mi particle <id>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                String particleId = args[1];
                if (!Main.getInstance().getParticleConfig().contains("effects." + particleId)) {
                    player.sendMessage(PFX_WRN + "ID '§f" + particleId + "§7' is not defined in §nParticle.yml§7. It will still be stored.");
                }

                var meta = item.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(
                            new NamespacedKey(Main.getInstance(), "item_particle"),
                            PersistentDataType.STRING,
                            particleId
                    );
                    item.setItemMeta(meta);
                    player.sendMessage(PFX_OK + "Particle effect §f" + particleId + " §7applied to item.");
                    new org.ThienNguyen.Listener.StatsListener().updatePlayerStats(player);
                }
                return true;
            }

            case "unparticle" -> {
                if (!(sender instanceof Player player)) return true;
                if (!player.hasPermission("myitem.admin")) return true;

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                var meta = item.getItemMeta();
                if (meta != null) {
                    NamespacedKey key = new NamespacedKey(Main.getInstance(), "item_particle");
                    if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        player.sendMessage(PFX_WRN + "This item has no particle effect attached.");
                        return true;
                    }
                    meta.getPersistentDataContainer().remove(key);
                    item.setItemMeta(meta);
                    player.sendMessage(PFX_OK + "Particle effect removed from item.");
                    new org.ThienNguyen.Listener.StatsListener().updatePlayerStats(player);
                }
                return true;
            }

            case "version" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(PFX_ERR + "This command can only be used by players.");
                    return true;
                }
                org.ThienNguyen.Webapi.Update.openVersionGUI((Player) sender, plugin);
            }

            case "tooltip" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PFX_ERR + "This command can only be used by players.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(PFX + "§8§m────────§r §eTooltip System §8§m────────");
                    player.sendMessage(PFX + "§f/mi tooltip add <type> §7— Apply tooltip frame");
                    player.sendMessage(PFX + "§f/mi tooltip undo §7— Restore original state");
                    player.sendMessage(PFX + "§8§m─────────────────────────");
                    return true;
                }

                String subAction = args[1].toLowerCase();
                switch (subAction) {
                    case "add" -> {
                        if (args.length < 3) {
                            player.sendMessage(PFX_ERR + "Please specify a tooltip type (e.g. §fnormal§7, §fgodlike§7).");
                            return true;
                        }
                        org.ThienNguyen.Utils.Tooltips.applyTooltip(player, args[2].toLowerCase());
                    }
                    case "undo" -> org.ThienNguyen.Utils.Tooltips.handleUndo(player);
                    default -> player.sendMessage(PFX_ERR + "Invalid action. Use §fadd §7or §fundo§7.");
                }
            }

            case "update" -> {
                if (!sender.hasPermission("myitem.admin")) {
                    sender.sendMessage(PFX_ERR + "You do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    if (sender instanceof Player player) {
                        org.ThienNguyen.Webapi.Update.openUpdateListGUI(player, plugin);
                    } else {
                        sender.sendMessage(PFX_ERR + "Console must specify a version: §f/myitem update <version>");
                    }
                    return true;
                }
                String updateVersion = args[1];
                if (sender instanceof Player player) {
                    org.ThienNguyen.Webapi.Update.downloadAndUpdate(plugin, updateVersion, player);
                } else {
                    sender.sendMessage(PFX + "Downloading version §e" + updateVersion + "§7... (see console for progress)");
                }
                return true;
            }

            case "sync" -> {
                return new StationCMD(plugin, stationDb).onCommand(sender, command, label, args);
            }

            case "tiers" -> {
                if (args.length < 2) {
                    sender.sendMessage(PFX_ERR + "Usage: §f/mi tiers <id>");
                    return true;
                }
                if (!(sender instanceof Player pTiers)) {
                    sender.sendMessage(PFX_ERR + "This command can only be used by players.");
                    return true;
                }
                ItemStack itemTier = pTiers.getInventory().getItemInMainHand();
                if (itemTier == null || itemTier.getType().isAir()) {
                    pTiers.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }
                org.ThienNguyen.Lore.TiersLore.applyTier(itemTier, args[1].toLowerCase());
                pTiers.sendMessage(PFX_OK + "Item tier updated successfully.");
                break;
            }

            case "checkitem" -> {
                if (!(sender instanceof Player player)) return true;

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                player.sendMessage("§8§m──────────§r §b§lItem Inspector §8§m──────────");
                player.sendMessage("§7Material      §8: §f" + item.getType().name());

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    // Kiểm tra màu nếu là trang bị da (Leather Armor)
                    if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
                        org.bukkit.Color color = leatherMeta.getColor();
                        String hexColor = String.format("#%06X", (0xFFFFFF & color.asRGB()));
                        player.sendMessage("§7Leather Color §8: §fRGB(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ") §8[" + hexColor + "]");
                    }

                    if (meta.hasCustomModelData()) {
                        player.sendMessage("§7Custom Model  §8: §e" + meta.getCustomModelData());
                    } else {
                        player.sendMessage("§7Custom Model  §8: §8none");
                    }

                    player.sendMessage("§7PDC Keys:");
                    var pdc = meta.getPersistentDataContainer();
                    var keys = pdc.getKeys();

                    if (keys.isEmpty()) {
                        player.sendMessage("  §8(no persistent data on this item)");
                    } else {
                        for (NamespacedKey key : keys) {
                            String valueStr = "§7[unknown type]";
                            if (pdc.has(key, PersistentDataType.STRING))
                                valueStr = "§6String §f\"" + pdc.get(key, PersistentDataType.STRING) + "\"";
                            else if (pdc.has(key, PersistentDataType.DOUBLE))
                                valueStr = "§aDouble §f" + pdc.get(key, PersistentDataType.DOUBLE);
                            else if (pdc.has(key, PersistentDataType.INTEGER))
                                valueStr = "§bInteger §f" + pdc.get(key, PersistentDataType.INTEGER);
                            else if (pdc.has(key, PersistentDataType.FLOAT))
                                valueStr = "§dFloat §f" + pdc.get(key, PersistentDataType.FLOAT);
                            else if (pdc.has(key, PersistentDataType.LONG))
                                valueStr = "§eLong §f" + pdc.get(key, PersistentDataType.LONG);
                            else if (pdc.has(key, PersistentDataType.BYTE))
                                valueStr = "§5Byte §f" + pdc.get(key, PersistentDataType.BYTE);
                            else if (pdc.has(key, PersistentDataType.SHORT))
                                valueStr = "§3Short §f" + pdc.get(key, PersistentDataType.SHORT);
                            player.sendMessage("  §8• §7" + key + " §8→ " + valueStr);
                        }
                    }
                } else {
                    player.sendMessage(PFX_ERR + "ItemMeta is invalid.");
                }
                player.sendMessage("§8§m────────────────────────────────────");
            }
            case "dye-color" -> {
                if (!(sender instanceof Player player)) return true;

                if (args.length < 4) {
                    player.sendMessage(PFX_ERR + "Usage: /myitem dye-color <red> <green> <blue>");
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                ItemMeta meta = item.getItemMeta();
                if (!(meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta)) {
                    player.sendMessage(PFX_ERR + "The item in your main hand must be leather armor.");
                    return true;
                }

                try {
                    int red = Integer.parseInt(args[1]);
                    int green = Integer.parseInt(args[2]);
                    int blue = Integer.parseInt(args[3]);

                    // Giới hạn giá trị màu từ 0 đến 255
                    red = Math.clamp(red, 0, 255);
                    green = Math.clamp(green, 0, 255);
                    blue = Math.clamp(blue, 0, 255);

                    org.bukkit.Color color = org.bukkit.Color.fromRGB(red, green, blue);
                    leatherMeta.setColor(color);
                    item.setItemMeta(leatherMeta);

                    String hexColor = String.format("#%06X", (0xFFFFFF & color.asRGB()));
                    player.sendMessage("§aSuccessfully updated leather color to RGB(" + red + ", " + green + ", " + blue + ") §8[" + hexColor + "]");
                } catch (NumberFormatException e) {
                    player.sendMessage(PFX_ERR + "RGB values must be valid integers between 0 and 255.");
                }
                return true;
            }
            case "passive" -> {
                if (!(sender instanceof Player player)) return true;
                if (!checkAdmin(player)) return true;

                if (args.length < 2) {
                    player.sendMessage(PFX + "Usage:");
                    player.sendMessage("  §f/mi passive bind <id> §7— Bind a passive to held item");
                    player.sendMessage("  §f/mi passive unbind <id> §7— Unbind a passive from held item");
                    return true;
                }

                String action = args[1].toLowerCase();
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                    return true;
                }

                ItemMeta meta = item.getItemMeta();
                if (meta == null) return true;

                NamespacedKey passiveKey = new NamespacedKey(plugin, "passive_ids");
                String oldData = meta.getPersistentDataContainer().get(passiveKey, PersistentDataType.STRING);
                if (oldData == null) oldData = "";

                List<String> currentIds = new ArrayList<>();
                if (!oldData.isEmpty()) {
                    for (String pid : oldData.split(",")) {
                        if (!pid.trim().isEmpty()) currentIds.add(pid.trim());
                    }
                }

                switch (action) {
                    case "bind" -> {
                        if (args.length < 3) {
                            player.sendMessage(PFX_ERR + "Usage: §f/mi passive bind <id>");
                            return true;
                        }
                        String passiveId = args[2].trim();
                        if (currentIds.contains(passiveId)) {
                            player.sendMessage(PFX_WRN + "Passive §f" + passiveId + " §7is already bound to this item.");
                            return true;
                        }
                        currentIds.add(passiveId);
                        meta.getPersistentDataContainer().set(passiveKey, PersistentDataType.STRING, String.join(",", currentIds));
                        item.setItemMeta(meta);
                        org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
                        org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                        player.sendMessage(PFX_OK + "Passive §e" + passiveId + " §7bound to item.");
                    }
                    case "unbind" -> {
                        if (args.length < 3) {
                            player.sendMessage(PFX_ERR + "Usage: §f/mi passive unbind <id>");
                            return true;
                        }
                        String passiveId = args[2].trim();
                        if (!currentIds.contains(passiveId)) {
                            player.sendMessage(PFX_ERR + "Passive §f" + passiveId + " §7is not on this item.");
                            return true;
                        }
                        currentIds.remove(passiveId);
                        if (currentIds.isEmpty()) {
                            meta.getPersistentDataContainer().remove(passiveKey);
                        } else {
                            meta.getPersistentDataContainer().set(passiveKey, PersistentDataType.STRING, String.join(",", currentIds));
                        }
                        item.setItemMeta(meta);
                        org.ThienNguyen.Lore.LoreGenerator.rebuild(item);
                        org.ThienNguyen.Listener.CacheListener.refreshCache(player);
                        player.sendMessage(PFX_OK + "Passive §e" + passiveId + " §7unbound from item.");
                    }
                    default -> player.sendMessage(PFX_ERR + "Invalid action. Use §fbind §7or §funbind§7.");
                }
                return true;
            }

            case "storage" -> {
                if (args.length < 2) {
                    sender.sendMessage(PFX + "§f/myitem storage create <type> §7— Create new yml file");
                    sender.sendMessage(PFX + "§f/myitem storage save <type> <id> §7— Save held item");
                    sender.sendMessage(PFX + "§f/myitem storage load <type> <id> §7— Retrieve item from cache");
                    sender.sendMessage(PFX + "§f/myitem storage reload §7— Reload all items from folder");
                    sender.sendMessage(PFX + "§f/myitem storage browse §7— Browse item GUI");
                    return true;
                }

                ItemStorageManager ism = plugin.getItemStorageManager();
                String action = args[1].toLowerCase();

                switch (action) {
                    case "browse" -> {
                        if (!(sender instanceof Player p)) {
                            sender.sendMessage(PFX_ERR + "This command can only be used by players.");
                            return true;
                        }
                        plugin.getMiBrowseGUI().openTypePage(p, 0);
                    }
                    case "create" -> {
                        if (args.length < 3) {
                            sender.sendMessage(PFX_ERR + "Example: §f/myitem storage create Sword");
                            return true;
                        }
                        String type = args[2];
                        if (ism.createTypeFile(type)) {
                            sender.sendMessage(PFX_OK + "File created: §fManagerItem/" + type + ".yml");
                        } else {
                            sender.sendMessage(PFX_ERR + "File already exists or could not be created.");
                        }
                    }
                    case "save" -> {
                        if (!(sender instanceof Player p)) return true;
                        if (args.length < 4) {
                            p.sendMessage(PFX_ERR + "Example: §f/myitem storage save <type> <id>");
                            return true;
                        }
                        String type = args[2];
                        String id = args[3];
                        ItemStack item = p.getInventory().getItemInMainHand();
                        if (item.getType().isAir()) {
                            p.sendMessage(PFX_ERR + "You must be holding an item in your main hand.");
                            return true;
                        }
                        if (ism.saveItemToType(type, id, item)) {
                            p.sendMessage(PFX_OK + "Item §f" + id + " §7saved to §e" + type + ".yml §7(cache updated).");
                        } else {
                            p.sendMessage(PFX_ERR + "File §f" + type + ".yml §7not found. Run §f'create' §7first.");
                        }
                    }
                    case "load" -> {
                        if (!(sender instanceof Player p)) return true;
                        if (args.length < 4) {
                            p.sendMessage(PFX_ERR + "Example: §f/myitem storage load <type> <id>");
                            return true;
                        }
                        String type = args[2];
                        String id = args[3];
                        ItemStack item = ism.getItem(id);
                        if (item != null) {
                            p.getInventory().addItem(item);
                            p.sendMessage(PFX_OK + "Retrieved §f" + id + " §7from §e" + type + ".yml §7(cache).");
                        } else {
                            p.sendMessage(PFX_ERR + "Item §f" + id + " §7not found in §e" + type + ".yml§7. Try §f/myitem storage reload§7.");
                        }
                    }
                    case "reload" -> {
                        ism.loadAllItems();
                        sender.sendMessage(PFX_OK + "All items reloaded from the ManagerItem folder.");
                    }
                    default -> sender.sendMessage(PFX_ERR + "Unknown action: §f" + action);
                }
            }

            default -> sendHelp(sender, 1);
        }
        return true;
    }

    // ── Permission helper ─────────────────────────────────────────────────────
    private boolean checkAdmin(Player p) {
        if (!p.hasPermission("myitem.admin")) {
            p.sendMessage(PFX_ERR + "You do not have permission to perform this action.");
            return false;
        }
        return true;
    }

    // ── Gem item builder (unchanged logic) ───────────────────────────────────
    private ItemStack createGemItem(String id, FileConfiguration config, String itemTag) {
        return createGemItem(id, id, config, itemTag);
    }

    /**
     * @param storedId  id lưu vào PDC (gem_item_id) — dùng để đối chiếu về sau (vd: GemDucLo tra cứu drill).
     * @param configPath đường dẫn dùng để ĐỌC material/display-name/lore/model-id trong config
     *                   (thường trùng với storedId, trừ trường hợp drill: config nằm lồng dưới
     *                   "<type>.drills.<storedId>" trong type.yml sau khi gộp DucLo.yml).
     */
    private ItemStack createGemItem(String storedId, String configPath, FileConfiguration config, String itemTag) {
        try {
            String matStr = config.getString(configPath + ".material", "STONE");
            org.bukkit.Material mat = org.bukkit.Material.valueOf(matStr.toUpperCase());
            ItemStack item = new ItemStack(mat);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        config.getString(configPath + ".display-name", storedId)));

                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList(configPath + ".lore"))
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                meta.setLore(lore);

                if (config.contains(configPath + ".model-id"))
                    meta.setCustomModelData(config.getInt(configPath + ".model-id"));

                NamespacedKey typeKey = new NamespacedKey(Main.getInstance(), "gem_item_type");
                NamespacedKey idKey   = new NamespacedKey(Main.getInstance(), "gem_item_id");
                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, itemTag);
                meta.getPersistentDataContainer().set(idKey,   PersistentDataType.STRING, storedId);
                item.setItemMeta(meta);
            }
            return item;
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Failed to create gem item: " + storedId);
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
        helpLines.add(miPrefix + "checkitem §7- Kiểm tra data vật phẩm");
        helpLines.add(miPrefix + "passive <bind/unbind> <id> §7- Thêm nội tại cho item");
        helpLines.add(miPrefix + "dye-color <r> <g> <b> §7- Đổi mã màu RGB cho áo da §c§lNEW");
        helpLines.add(miPrefix + "owner-tag <tên_người_chơi> §7- Gắn nhãn sở hữu item cho người chơi §c§lNEW");
        helpLines.add(miPrefix + "del-tag §7- Xóa nhãn sở hữu khỏi item trên tay §c§lNEW");
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
    }
}