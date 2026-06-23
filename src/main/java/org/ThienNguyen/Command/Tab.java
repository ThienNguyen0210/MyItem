package org.ThienNguyen.Command;

import org.ThienNguyen.Main;
import org.ThienNguyen.Skill.SkillManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class Tab implements TabCompleter {
    private static final List<String> ELEMENT_TYPES = Arrays.asList("attack", "defense");
    private static final List<String> COMMON_SLOTS = Arrays.asList(
            "mainhand", "offhand", "head", "chest", "legs", "feet", "any", "offhand,mainhand"
    );

    private static final List<String> MAIN_SUBCOMMANDS = Arrays.asList(
            "sync", "ability", "buff", "debuff", "element",
            "save", "load", "delete", "reload", "skill", "help",
            "gemstone", "enchant", "unskill", "upgrade", "givegem", "giveamulet", "trans",
            "connect", "editor", "stats", "update", "version", "particle", "unparticle",
            "tiers", "consume", "tooltip", "loreformat",
            "ic", "evo", "ai", "getai", "expire", "storage", "checkitem"
    );

    private static final List<String> IC_SUBCOMMANDS = Arrays.asList("add", "unadd");

    private static final List<String> MI_SUBCOMMANDS = Arrays.asList("create", "save", "load", "reload", "browse");

    private static final List<String> SKILL_TYPES = Arrays.asList("Weapon", "Command", "Mythicmob");

    private static final List<String> TRIGGERS = Arrays.asList(
            "HIT", "SNEAK", "RIGHT_CLICK", "LEFT_CLICK", "SHIFT_LEFT", "SHIFT_RIGHT", "DOUBLE_SNEAK"
    );

    private static final List<String> COMMON_COOLDOWN = Arrays.asList("5", "10", "15", "20", "30", "45", "60", "90", "120", "300");
    private static final List<String> COMMON_LEVEL = Arrays.asList("1", "2", "3", "4", "5", "10", "20", "50", "100");
    private static final List<String> COMMON_AMPLIFIER = Arrays.asList("0", "1", "2", "3", "4", "5", "10");
    private static final List<String> COMMON_DURATION_TICKS = Arrays.asList("20", "40", "60", "100", "200", "400", "600");
    private static final List<String> COMMON_CENT = Arrays.asList("5", "10", "20", "30", "50", "70", "100");
    private static final List<String> COMMON_AMOUNTS = Arrays.asList("1", "16", "32", "64");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.addAll(MAIN_SUBCOMMANDS);
        }




        else if (args.length == 2 && args[0].equalsIgnoreCase("ic")) {
            suggestions.addAll(IC_SUBCOMMANDS);
        }

        else if (args.length >= 3 && args[0].equalsIgnoreCase("ic")) {
            String icSub = args[1].toLowerCase();

            if (icSub.equals("add") || icSub.equals("unadd")) {
                FileConfiguration comboConfig = Main.getInstance().getComboConfig();
                if (comboConfig != null) {

                    suggestions.addAll(comboConfig.getKeys(false));
                }
            }
        }

        // ── mi: args[1] = subcommand ──────────────────────────────────────────
        else if (args.length == 2 && args[0].equalsIgnoreCase("storage")) {
            suggestions.addAll(MI_SUBCOMMANDS);
        }

        // ── mi: args[2] = type (tên file .yml trong ManagerItem) ──────────────
        else if (args.length == 3 && args[0].equalsIgnoreCase("storage")) {
            String miSub = args[1].toLowerCase();
            if (miSub.equals("create") || miSub.equals("save") || miSub.equals("load")) {
                ItemStorageManager ism = Main.getInstance().getItemStorageManager();
                if (ism != null) suggestions.addAll(ism.getTypeNames());
            }
        }

        // ── mi: args[3] = id (lọc theo type đã chọn ở args[2]) ───────────────
        else if (args.length == 4 && args[0].equalsIgnoreCase("storage")) {
            String miSub = args[1].toLowerCase();
            if (miSub.equals("save") || miSub.equals("load")) {
                ItemStorageManager ism = Main.getInstance().getItemStorageManager();
                if (ism != null) suggestions.addAll(ism.getIdsByType(args[2]));
            }
        }




        else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "evo" -> {
                    suggestions.add("ALL");
                    suggestions.add("mm_");
                    suggestions.add("mm_<id Mythicmob>");

                    suggestions.addAll(Arrays.asList("ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "ENDERMAN"));
                }
                case "consume" -> suggestions.add("give");
                case "skill" -> suggestions.addAll(SKILL_TYPES);
                case "unskill" -> suggestions.addAll(getAllSkillNames());
                case "save" -> suggestions.add("<ID_Vat_Pham>");
                case "load", "delete" -> suggestions.addAll(getItemDatabaseIds());
                case "tooltip" -> suggestions.addAll(Arrays.asList("add", "undo"));
                case "particle" -> {
                    FileConfiguration pConfig = Main.getInstance().getParticleConfig();
                    if (pConfig != null && pConfig.contains("effects")) {
                        suggestions.addAll(pConfig.getConfigurationSection("effects").getKeys(false));
                    }
                }
                case "tiers" -> {
                    FileConfiguration tConfig = Main.getInstance().getTiersConfig();
                    if (tConfig != null && tConfig.contains("tiers")) {
                        suggestions.addAll(tConfig.getConfigurationSection("tiers").getKeys(false));
                    }
                }
                case "element" -> suggestions.addAll(ELEMENT_TYPES);
                case "material" -> {
                    String input = args[1].toLowerCase();
                    suggestions.addAll(Arrays.stream(Material.values())
                            .filter(m -> m.isItem() && m.name().toLowerCase().startsWith(input))
                            .map(Material::name).map(String::toLowerCase).limit(50).collect(Collectors.toList()));
                }
                case "enchant" -> suggestions.addAll(Arrays.stream(org.bukkit.enchantments.Enchantment.values())
                        .map(e -> e.getKey().getKey().toLowerCase()).collect(Collectors.toList()));
                case "stats" -> suggestions.addAll(Arrays.asList(
                        "damage", "health", "armor", "pve_damage", "pvp_damage", "pve_defense", "pvp_defense",
                        "critical_chance", "critical_damage", "lifesteal", "dodge_rate", "block_rate", "penetration",
                        "level_require", "true_damage", "thorns", "class_require", "max_mana", "mana_regen",
                        "exp_bonus", "attack_speed", "movement_speed", "health_regen", "armor_pen", "all_damage", "all_defense", "bow_damage", "knockback_resistance", "death_damage", "durability",
                        "magic_damage", "magic_defense", "Accuracy", "critical_damage_reduction", "damage_reduction"
                ));
                case "sync" -> suggestions.addAll(Arrays.asList("clear", "addcode", "update", "check"));
                case "ability" -> suggestions.addAll(Arrays.asList(
                        "LIGHTNING", "POISON", "WEAK", "HUNGER", "TIRED", "CONFUSE", "WITHER", "BLIND", "SLOW",
                        "AIR_SHOCK", "CURSE", "BUBBLE", "BLEED", "FIRE_VORTEX", "FREEZE", "DISARM", "EXPLODE",
                        "FLAME_PULSE", "ANGEL", "SHADOW_DEVOUR", "SONIC_WAVE", "FIRE_RAIN", "STAR_RITUAL",
                        "FIRE_TRIPLE_SHOT", "STAR_FALL", "SUN_STRIKE_AOE", "BLACK_HOLE", "SHADOW_WAVE", "FAIRY_CHAIN", "LILAC_BLOOM_BOMB", "LEAF_STORM",
                        "SPIRIT_WOLF", "PLAGUE_SPREAD", "WIND_TORNADO", "FIRE_ORB", "PLASMA_ORB", "ELECTRIC_BLADE", "FIRST_STRIKE", "TNT_STUCK", "BadLuck",
                        "Bubble_Deflector", "DARK_FLAME", "DARK_IMPACT", "ROOTS", "VAMPIRISM", "VENOM_SPREAD"
                ));
                case "buff" -> suggestions.addAll(Arrays.asList(
                        "SPEED", "FAST_DIGGING", "INCREASE_DAMAGE", "JUMP", "REGENERATION", "DAMAGE_RESISTANCE",
                        "FIRE_RESISTANCE", "WATER_BREATHING", "HEALTH_BOOST", "ABSORPTION", "NIGHT_VISION", "LUCK"
                ));
                case "debuff" -> suggestions.addAll(Arrays.asList(
                        "SLOW", "SLOW_DIGGING", "CONFUSION", "BLINDNESS", "HUNGER", "WEAKNESS", "POISON",
                        "WITHER", "GLOWING", "LEVITATION", "UNLUCK", "DARKNESS"
                ));
                case "gemstone" -> suggestions.add("give");
                case "givegem" -> {
                    FileConfiguration gemConfig = Main.getInstance().upgradeGemConfig();
                    if (gemConfig != null) suggestions.addAll(gemConfig.getKeys(false));
                }
                case "giveamulet", "trans" -> suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
        }

        else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "consume" -> {
                    FileConfiguration consumeConfig = Main.getInstance().getConsumeConfig();
                    if (consumeConfig != null) suggestions.addAll(consumeConfig.getKeys(false));
                }
                case "evo" -> suggestions.addAll(Arrays.asList("10", "50", "100", "500", "1000"));
                case "tooltip" -> {
                    if (args[1].equalsIgnoreCase("add")) {
                        FileConfiguration tConfig = Main.getInstance().getTooltipConfig();
                        if (tConfig != null && tConfig.contains("types")) {
                            suggestions.addAll(tConfig.getConfigurationSection("types").getKeys(false));
                        }
                    }
                }
                case "gemstone" -> suggestions.addAll(Arrays.asList("Gem", "Drill", "Remover"));
                case "element" -> {

                    FileConfiguration eConfig = Main.getInstance().getElementConfig();
                    if (eConfig != null) suggestions.addAll(eConfig.getKeys(false));
                }
                case "enchant" -> suggestions.addAll(COMMON_LEVEL);
                case "skill" -> suggestions.addAll(SkillManager.getSkillNamesByType(args[1]));
                case "ability", "stats" -> suggestions.addAll(COMMON_LEVEL);
                case "buff", "debuff" -> suggestions.addAll(COMMON_AMPLIFIER);
                case "givegem" -> suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                case "giveamulet" -> suggestions.addAll(COMMON_AMOUNTS);

            }
        }

        else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "evo" -> suggestions.addAll(getItemDatabaseIds());
                case "consume" -> suggestions.addAll(COMMON_AMOUNTS);
                case "stats" -> suggestions.addAll(COMMON_SLOTS);
                case "gemstone" -> {
                    String type = args[2].toLowerCase();
                    if (type.equals("gem")) {
                        FileConfiguration config = Main.getInstance().getGemConfig();
                        if (config != null) suggestions.addAll(config.getKeys(false));
                    } else if (type.equals("drill")) {
                        FileConfiguration config = Main.getInstance().getDucLoConfig();
                        if (config != null) suggestions.addAll(config.getKeys(false));
                    }
                    else if (type.equals("remover")) {
                        FileConfiguration config = Main.getInstance().getGemConfig();

                        if (config != null) suggestions.addAll(config.getKeys(false));
                    }
                }
                case "element" -> suggestions.addAll(COMMON_LEVEL);
                case "skill" -> suggestions.addAll(TRIGGERS);
                case "ability" -> suggestions.addAll(COMMON_CENT);
                case "buff", "debuff" -> suggestions.addAll(COMMON_DURATION_TICKS);
                case "givegem" -> suggestions.addAll(COMMON_AMOUNTS);
            }
        }

        else if (args.length == 5) {
            String sub = args[0].toLowerCase();
            if (sub.equals("consume")) {
                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
            else if (sub.equals("skill")) suggestions.addAll(COMMON_COOLDOWN);
            else if (sub.equals("gemstone")) suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
        }

        else if (args.length == 6) {
            String sub = args[0].toLowerCase();
            if (sub.equals("skill")) suggestions.addAll(COMMON_LEVEL);
            else if (sub.equals("gemstone")) suggestions.addAll(COMMON_AMOUNTS);
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private List<String> getAllSkillNames() {
        List<String> allSkills = new ArrayList<>();
        allSkills.addAll(SkillManager.getSkillNamesByType("Weapon"));
        allSkills.addAll(SkillManager.getSkillNamesByType("Command"));
        allSkills.addAll(SkillManager.getSkillNamesByType("Mythicmob"));
        return allSkills;
    }

    private List<String> getItemDatabaseIds() {
        if (Main.getInstance().getItemDatabase() != null) {
            return Main.getInstance().getItemDatabase().getAllIds();
        }
        return Collections.emptyList();
    }
}