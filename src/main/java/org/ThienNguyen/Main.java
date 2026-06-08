package org.ThienNguyen;

import org.ThienNguyen.AI.utils.AIListener;
import org.ThienNguyen.Command.ItemStorageManager;
import org.ThienNguyen.Command.MyItemCommand;
import org.ThienNguyen.Command.Tab;
import net.milkbowl.vault.economy.Economy;
import org.ThienNguyen.Database.ItemDatabase;
import org.ThienNguyen.EditItem.AttributeCommand;
import org.ThienNguyen.EditItem.ItemFlagCommand;
import org.ThienNguyen.EditItem.ModelAndMaterial;
import java.util.Optional;
import java.util.Arrays;
import net.milkbowl.vault.economy.Economy;
import org.ThienNguyen.GemSocket.GemRemover;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.ThienNguyen.Hook.MyItemExpansion;
import org.ThienNguyen.Language.LanguageManager;
import org.ThienNguyen.Webapi.UpdateListener;
import org.ThienNguyen.Listener.*;
import org.ThienNguyen.Listener.Station.StationDatabase;
import org.ThienNguyen.Listener.Station.Stations;
import org.ThienNguyen.Skill.SkillManager;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {
    private static Economy econ = null; // Thêm dòng này
    private FileConfiguration evolutionConfig;
    private static Main instance;
    private FileConfiguration aiConfig;
    private FileConfiguration tooltipConfig;
    // --- Các biến FileConfiguration ---
    private FileConfiguration skillConfig;
    private FileConfiguration customListenerConfig;
    private StationDatabase stationDatabase;
    private FileConfiguration gemConfig;
    private org.ThienNguyen.AI.AIProcessor aiProcessor;
    private FileConfiguration consumeConfig;
    private FileConfiguration gemTypeConfig;
    private FileConfiguration chuyenHoaConfig;
    private FileConfiguration particleConfig;
    private FileConfiguration ducLoConfig;
    private FileConfiguration upgradeGemConfig;
    private FileConfiguration loreFormatConfig;
    private FileConfiguration tiersConfig;
    private FileConfiguration enchantConfig;
    private FileConfiguration mobDropConfig;
    private FileConfiguration UpgradeGem;
    private FileConfiguration skillWeaponConfig; // Thêm dòng này
    private File abilityTargetFile;
    private FileConfiguration abilityTargetConfig;
    private FileConfiguration statsConfig, customConfig, statsSettingsConfig, effectConfig, abilityConfig;
    private FileConfiguration elementConfig, elementLoreConfig;
    private FileConfiguration skillCommandConfig;
    private FileConfiguration skillMythicLibConfig;
    private FileConfiguration protectionConfig;
    private LanguageManager langManager;
    private FileConfiguration comboConfig;
    // --- Database ---
    private ItemDatabase itemDatabase;
    private FileConfiguration scriptSkillConfig;
    private FileConfiguration expireConfig; // <-- THÊM DÒNG NÀY ĐỂ QUẢN LÝ EXPIRE.YML
    private ItemStorageManager itemStorageManager;
    private org.ThienNguyen.Command.MiBrowseGUI miBrowseGUI;
    @Override
    public void onEnable() {
        instance = this;
        if (setupEconomy()) {
            getLogger().info("&aHook Vault Success!");
        } else {
            getLogger().warning("&cQuên cài vault kìa bro,:)");
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MyItemExpansion().register();
            getLogger().info("Đã đăng ký Placeholder thành công: %windycore_totaldamage%");
        } else {
            getLogger().warning("Không tìm thấy PlaceholderAPI! Placeholder của WindyCore sẽ không hoạt động.");
        }
        this.itemStorageManager = new ItemStorageManager(this);
        this.miBrowseGUI = new org.ThienNguyen.Command.MiBrowseGUI(this);
        getServer().getPluginManager().registerEvents(this.miBrowseGUI, this);
        Bukkit.getPluginManager().registerEvents(new AIExperienceGUI(), this);
        getServer().getPluginManager().registerEvents(new JewelryManager(null), this);
        getServer().getPluginManager().registerEvents(new AIListener(), this);
        this.aiProcessor = new org.ThienNguyen.AI.AIProcessor();
        org.ThienNguyen.Ability.AbilityManager.loadExternalAbilities();
        this.stationDatabase = new StationDatabase(this);
        getServer().getPluginManager().registerEvents(new Stations(this, this.stationDatabase), this);
// Trong onEnable()
        // Đăng ký Command Executor cho rpginv (Bản Full tối ưu cho ThienNguyen)
        Optional.ofNullable(getCommand("rpginv")).ifPresent(command -> {

            // 1. Xử lý thực thi lệnh
            command.setExecutor((sender, cmd, label, args) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c[!] Lệnh này chỉ dành cho người chơi!");
                    return true;
                }

                // Mở GUI chính
                if (args.length == 0) {
                    player.openInventory(new JewelryManager(player).getInventory());
                    return true;
                }

                String subCommand = args[0].toLowerCase();
                switch (subCommand) {
                    case "type":
                        if (args.length < 2) {
                            player.sendMessage("§6§l[!] §7Cách dùng: §e/rpginv type <id>");
                            return true;
                        }

                        String typeId = args[1].toLowerCase();
                        // Thực hiện gán loại trang sức
                        if (JewelryManager.setJewelryType(player, typeId)) {
                            player.sendMessage("§a§l✔ §7Đã gán thành công loại: §e" + typeId);
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                        } else {
                            player.sendMessage("§c§l✘ §7Thất bại! Bạn phải cầm vật phẩm trên tay.");
                        }
                        break;

                    case "untype":
                        if (JewelryManager.setJewelryType(player, null)) {
                            player.sendMessage("§a§l✔ §7Đã xóa định dạng trang sức khỏi vật phẩm.");
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
                        } else {
                            player.sendMessage("§c§l✘ §7Thất bại! Hãy cầm vật phẩm cần xóa.");
                        }
                        break;

                    case "reload": // Thêm sub-command reload cho máu lửa
                        if (!player.hasPermission("myitem.admin")) {
                            player.sendMessage("§cBạn không có quyền!");
                            return true;
                        }
                        reloadPluginConfigs();
                        player.sendMessage("§a§l✔ §7Đã nạp lại cấu hình trang sức!");
                        break;

                    default:
                        player.sendMessage("§8§m-------§r §6§lRPG INVENTORY §8§m-------");
                        player.sendMessage("§7» §e/rpginv §f: Mở kho đồ trang sức");
                        player.sendMessage("§7» §e/rpginv type <id> §f: Gán ID trang sức");
                        player.sendMessage("§7» §e/rpginv untype §f: Xóa ID trang sức");
                        player.sendMessage("§8§m---------------------------");
                        break;
                }
                return true;
            });
            getServer().getPluginManager().registerEvents(new org.ThienNguyen.Evolution.EvolutionListener(), this);
            getServer().getPluginManager().registerEvents(new org.ThienNguyen.Listener.DurabilityListener(), this);
            // 2. Xử lý Tab Completion (Gợi ý lệnh cực mượt)
            command.setTabCompleter((sender, cmd, alias, args) -> {
                List<String> suggestions = new ArrayList<>();

                if (args.length == 1) {
                    List<String> subs = Arrays.asList("type", "untype", "reload");
                    for (String s : subs) {
                        if (s.startsWith(args[0].toLowerCase())) suggestions.add(s);
                    }
                }
                else if (args.length == 2 && args[0].equalsIgnoreCase("type")) {
                    // Lấy danh sách ID từ jewelry.slots trong config.yml
                    ConfigurationSection section = getConfig().getConfigurationSection("jewelry.slots");
                    if (section != null) {
                        for (String key : section.getKeys(false)) {
                            if (key.toLowerCase().startsWith(args[1].toLowerCase())) {
                                suggestions.add(key);
                            }
                        }
                    }
                }
                return suggestions;
            });
        });

        // Đăng ký Tab Completer (Sửa lỗi biến completions ở đây)
        getCommand("rpginv").setTabCompleter((s, cmd, alias, args) -> {
            List<String> completions = new ArrayList<>(); // Khởi tạo biến

            if (args.length == 1) {
                String input = args[0].toLowerCase();
                if ("type".startsWith(input)) completions.add("type");
                if ("untype".startsWith(input)) completions.add("untype");
            }
            else if (args.length == 2 && args[0].equalsIgnoreCase("type")) {
                // Đọc từ config.yml (như bạn nói jewelry xài config.yml)
                ConfigurationSection section = getConfig().getConfigurationSection("jewelry.slots");
                if (section != null) {
                    String input = args[1].toLowerCase();
                    for (String key : section.getKeys(false)) {
                        if (key.toLowerCase().startsWith(input)) {
                            completions.add(key);
                        }
                    }
                }
            }
            return completions; // Dòng 76 của bạn phải nằm trong dấu ngoặc nhọn của Lambda này
        });
        // 1. Khởi tạo file config.yml mặc định
        saveDefaultConfig();

        // 2. Khởi tạo thư mục plugin
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // 3. Nạp toàn bộ cấu hình vào RAM (Phải gọi cái này trước khi đăng ký Listener)
        reloadPluginConfigs();
        getAIConfig();
        // 4. Khởi tạo Database
        itemDatabase = new ItemDatabase(getDataFolder().getAbsolutePath() + "/items.db");

        // 5. Đăng ký các Listener cần Cache
        org.ThienNguyen.Listener.CacheListener cacheListener = new org.ThienNguyen.Listener.CacheListener();
        getServer().getPluginManager().registerEvents(cacheListener, this);
        getServer().getPluginManager().registerEvents(new StatsListener(), this);
        // Refresh cache cho người chơi đang online
        for (Player p : Bukkit.getOnlinePlayers()) {
            cacheListener.refreshCache(p);
        }

        // 6. Đăng ký toàn bộ Events
        // passive
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.Listener.Passive.Blood(), this);
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.Listener.Passive.Longshot(), this);
        new org.ThienNguyen.Listener.Expire().runTaskLater(this, 100L);
        //
        langManager = new LanguageManager(this);
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.Listener.AbilityBlockListener(), this);
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.Consume.ConsumeManager(), this);
        getServer().getPluginManager().registerEvents(new UpdateListener(this), this);
        getServer().getPluginManager().registerEvents(new MobDrop(), this);
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.Utils.ChuyenHoa(), this);
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.Utils.GUI(), this);
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.GemSocket.GemDucLo(), this);
        getServer().getPluginManager().registerEvents(new GemRemover(), this);
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.GemSocket.GemKham(), this);
        getServer().getPluginManager().registerEvents(new org.ThienNguyen.Skill.SkillTriggerListener(), this);
        ExpLogic expLogic = new ExpLogic();
        getServer().getPluginManager().registerEvents(expLogic, this);
        getServer().getPluginManager().registerEvents(new EventDamage(), this);
//
//        // 7. Hooks
//        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
//            new MyItemExpansion().register();
//        }
        checkHooks();

        // 8. Đăng ký Lệnh
        registerAllCommands();

        // 9. Task ngầm
        startRegenTask();
        startEffectTask();
        sendStartupMessage();
        org.ThienNguyen.Listener.Particle.ParticleManager.startTask();
        getLogger().info("§a[Myitem] Plugin đã kích hoạt thành công!");
    }
    /**
     * Load AIConfig.yml từ thư mục AI/
     * - Tự động tạo thư mục AI nếu chưa có
     * - Tự động extract từ resource nếu file chưa tồn tại
     */
    private FileConfiguration loadAIConfig() {
        File aiFolder = new File(getDataFolder(), "AI");
        if (!aiFolder.exists()) {
            aiFolder.mkdirs();
        }

        File aiFile = new File(aiFolder, "AIConfig.yml");

        // Nếu file chưa tồn tại → extract từ resources (jar)
        if (!aiFile.exists()) {
            if (getResource("AI/AIConfig.yml") != null) {
                saveResource("AI/AIConfig.yml", false);
                getLogger().info("§a[MyItem] Đã tạo file AI/AIConfig.yml từ resource mặc định.");
            } else {
                // Nếu không có trong resource, tạo file rỗng + nội dung mặc định
                try {
                    aiFile.createNewFile();
                    YamlConfiguration defaultConfig = new YamlConfiguration();

                    defaultConfig.set("api-key", "");
                    defaultConfig.set("ai.tiny_font_enabled", true);
                    defaultConfig.set("ai.level_require", true);
                    defaultConfig.set("ai.unbreaking", false);

                    defaultConfig.save(aiFile);
                    getLogger().info("§e[MyItem] Đã tạo AI/AIConfig.yml với cấu hình mặc định.");
                } catch (IOException e) {
                    getLogger().severe("§c[MyItem] Không thể tạo AI/AIConfig.yml: " + e.getMessage());
                }
            }
        }

        return YamlConfiguration.loadConfiguration(aiFile);
    }
    public FileConfiguration getAiEulaConfig() {
        File file = new File(getDataFolder() + "/AI", "eula.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set("accepted", false);
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void setAiEulaAccepted(boolean value) {
        File file = new File(getDataFolder() + "/AI", "eula.yml");
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("accepted", value);
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Quét toàn bộ file .yml trong một thư mục cụ thể và nạp vào bộ nhớ
     * @param subFolderPath Đường dẫn thư mục con tính từ thư mục gốc của plugin (Ví dụ: "Skript/Skill")
     */
    private void reloadSkriptFolder(String subFolderPath) {
        File folder = new File(getDataFolder(), subFolderPath);

        // Nếu thư mục chưa tồn tại, tạo mới nó
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) return;

        int loadedCount = 0;
        for (File file : files) {
            try {
                // Nạp nội dung file cấu hình .yml vào bộ nhớ RAM
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                // TODO: Bạn có thể lưu trữ các đối tượng 'config' này vào một Map<String, FileConfiguration>
                // nếu cần truy xuất dữ liệu từ các file Skript này ở các class xử lý tính năng khác.

                loadedCount++;
            } catch (Exception e) {
            }
        }

        if (loadedCount > 0) {
        }
    }
    public void reloadPluginConfigs() {
        // 1. Nạp lại file config.yml chính của Bukkit
        reloadConfig();

        // ==================== NẠP AIConfig.yml (TỪ RESOURCE + THƯ MỤC AI) ====================
        this.aiConfig = loadAIConfig();
        if (this.itemStorageManager != null) {
            // Gọi hàm này để quét sạch cache cũ và đọc lại TOÀN BỘ file .yml trong thư mục ManagerItem
            this.itemStorageManager.loadAllItems();
        } else {
            // Trường hợp chưa khởi tạo thì khởi tạo mới (đề phòng)
            this.itemStorageManager = new org.ThienNguyen.Command.ItemStorageManager(this);
        }
        // Log thông tin AI Config
        if (aiConfig != null) {
            String apiKeyStatus = aiConfig.getString("api-key", "").trim().isEmpty()
                    ? "§7Sử dụng hard-coded key"
                    : "§aĐang dùng key tùy chỉnh";

            getLogger().info("§a[MyItem] Đã reload AI/AIConfig.yml thành công!");
            getLogger().info("§7   → tiny_font_enabled: " + aiConfig.getBoolean("ai.tiny_font_enabled", false));
            getLogger().info("§7   → level_require: " + aiConfig.getBoolean("ai.level_require", false));
            getLogger().info("§7   → unbreaking: " + aiConfig.getBoolean("ai.unbreaking", false));
            getLogger().info("§7   → api-key: " + apiKeyStatus);
        }

        // ==================== Các config khác giữ nguyên ====================
        this.evolutionConfig = setupConfig("Evolution.yml");

        syncStatsWithWeb();
        File readmeFile = new File(getDataFolder(), "README.yml");
        if (!readmeFile.exists()) {
            saveResource("README.yml", false);
        }
        if (this.aiProcessor != null) {
            this.aiProcessor = new org.ThienNguyen.AI.AIProcessor();
        }

        if (langManager != null) {
            langManager.loadLanguage();
        }
        this.customListenerConfig = setupConfig("Listener/Custom.yml");
        this.comboConfig = setupConfig("itemcombo.yml");
        this.customConfig = setupConfig("config.yml");
        this.tooltipConfig = setupConfig("Tooltip.yml");
        this.mobDropConfig = setupConfig("MobDrop.yml");
        this.particleConfig = setupConfig("Particle.yml");
        this.expireConfig = setupConfig("Listener/Expire.yml"); // <-- THÊM DÒNG NÀY ĐỂ TỰ ĐỘNG GIẢI NÉN VÀ NẠP CONFIG
        this.tiersConfig = setupConfig("LoreFormat/Tiers.yml");
        this.upgradeGemConfig = setupConfig("Upgrade/upgrade.yml");
        this.chuyenHoaConfig = setupConfig("Upgrade/chuyenhoa.yml");
        this.protectionConfig = setupConfig("Upgrade/protection.yml");
        this.gemConfig = setupConfig("GemStone/Gem.yml");
        this.gemTypeConfig = setupConfig("GemStone/type.yml");
        this.ducLoConfig = setupConfig("GemStone/DucLo.yml");
        this.UpgradeGem = setupConfig("Upgrade/Gem.yml");

        this.enchantConfig = setupConfig("Enchant.yml");
        this.skillMythicLibConfig = setupConfig("Listener/SkillMythicMob.yml");
        this.skillCommandConfig = setupConfig("Listener/SkillCommand.yml");
        this.elementConfig = setupConfig("Listener/Element.yml");
        this.statsSettingsConfig = setupConfig("Listener/StatsSettings.yml");
        this.skillWeaponConfig = setupConfig("Listener/SkillWeapon.yml");
        loadAbilityTargetConfig();

        checkAndSaveExample("Skript/Skill/EXAMPLE_SKILL.yml");
        checkAndSaveExample("Skript/Ability/EXAMPLE_SKILL.yml");
        reloadSkriptFolder("Skript/Skill");
        reloadSkriptFolder("Skript/Ability");
        this.loreFormatConfig = setupConfig("LoreFormat/Format.yml");
        this.statsConfig = setupConfig("LoreFormat/Stats.yml");
        this.effectConfig = setupConfig("LoreFormat/Effect.yml");
        this.abilityConfig = setupConfig("LoreFormat/Ability.yml");
        this.elementLoreConfig = setupConfig("LoreFormat/Element.yml");
        this.skillConfig = setupConfig("LoreFormat/Skill.yml");

        // Consume.yml
        File consumeFile = new File(getDataFolder(), "Consume.yml");
        if (!consumeFile.exists()) {
            saveResource("Consume.yml", false);
        }
        this.consumeConfig = YamlConfiguration.loadConfiguration(consumeFile);

        // Reload kỹ năng
        SkillManager.loadSkills();
        org.ThienNguyen.Ability.AbilityManager.clearAbilities();
        org.ThienNguyen.Ability.AbilityManager.loadExternalAbilities();
        EventDamage.resetFormulaCache();
        EventDamage.reloadAbilityTriggerCache();
        getLogger().info("§e[MyItem] Hệ thống cấu hình, Kỹ năng và Nội tại mẫu đã được nạp thành công!");
    }

    /**
     * Kiểm tra và giải nén file từ JAR nếu chưa tồn tại
     * @param path Đường dẫn tính từ root của JAR (VD: Skript/Skill/EXAMPLE_SKILL.yml)
     */
    private void checkAndSaveExample(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();

            // Luôn ưu tiên saveResource từ JAR trước
            if (getResource(path) != null) {
                saveResource(path, false);
                getLogger().info("§b[MyItem] Đã xuất file mẫu từ JAR: " + path);
            }
        }
    }
    // --- Các hàm phụ trợ (Giữ nguyên logic của bạn) ---

    private void sendStartupMessage() {
        String logo =
                "\n§3■■      ■■  ■■■■■■■■" +
                        "\n§3■■■    ■■■      ■■" +
                        "\n§3■■ ■  ■ ■■      ■■" +
                        "\n§3■■  ■■  ■■      ■■" +
                        "\n§3■■      ■■  ■■■■■■■■";

        String description =
                "\n§f> Tên plugin: §6Myitem" +
                        "\n§f> Phiên bản: §e" + "2.1" +
                        "\n§f> AI: §e" + "1.0" +
                        "\n§f> Tác giả: §dThiện Dev" +
                        "\n§f> Dành cho: §a1.14.x -> 1.21.x" +
                        "\n§f> Trạng thái: §c§lBản Chính Thức";

        Bukkit.getConsoleSender().sendMessage(logo + "\n" + description + "\n");
    }
    public FileConfiguration getMobDropConfig() {
        return mobDropConfig;
    }
    private void startRegenTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("regeneration.enabled", false)) return;

            // Đọc % hồi máu từ config (áp dụng cho tất cả player)
            double globalRegenPercent = getConfig().getDouble("regeneration.percent-per-second", 0.0);

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.isOnline() || p.isDead()) continue;

                double maxHp = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                double currentHp = p.getHealth();
                if (currentHp >= maxHp) continue;

                double finalRegen = 0;

                // 1. Flat regen từ trang bị
                if (p.hasMetadata("windy_health_regen")) {
                    finalRegen += p.getMetadata("windy_health_regen").get(0).asDouble();
                }

                // 2. % regen từ trang bị
                if (p.hasMetadata("windy_health_regen_percent")) {
                    double percent = p.getMetadata("windy_health_regen_percent").get(0).asDouble();
                    if (percent > 0) {
                        finalRegen += (maxHp * (percent / 100.0));
                    }
                }

                // 3. % regen từ config.yml (global cho tất cả player)
                if (globalRegenPercent > 0) {
                    finalRegen += (maxHp * (globalRegenPercent / 100.0));
                }

                if (finalRegen > 0) {
                    p.setHealth(Math.min(maxHp, currentHp + finalRegen));
                }

                StatsListener.getInstance().updateGemBuffsOnly(p);
            }
        }, 0L, 20L);
    }

    @Override
    public void onDisable() {
        if (stationDatabase != null) {
            stationDatabase.closeConnection();
        }
        if (itemDatabase != null) itemDatabase.closeConnection();
        getLogger().info("§c[Myitem] Plugin đang tắt...");
    }

    private void registerAllCommands() {
        org.ThienNguyen.EditItem.Essentials editItemHandler = new org.ThienNguyen.EditItem.Essentials();
        registerCommand("setname", editItemHandler);
        registerCommand("setlore", editItemHandler);
        registerCommand("unbreaking", editItemHandler);

        ModelAndMaterial mmHandler = new ModelAndMaterial();
        registerCommand("setmodel", mmHandler);
        registerCommand("material", mmHandler);

        registerCommand("attribute", new AttributeCommand());
        registerCommand("itemflag", new ItemFlagCommand());

        if (getCommand("myitem") != null) {
            MyItemCommand cmdExecutor = new MyItemCommand(this, this.stationDatabase);
            Tab tabCompleter = new Tab();

            getCommand("myitem").setExecutor(cmdExecutor);
            getCommand("myitem").setTabCompleter(tabCompleter);

            // Thêm dòng này để đảm bảo aliases "mi" cũng nhận được TabCompleter và Executor
            if (getCommand("mi") != null) {
                getCommand("mi").setExecutor(cmdExecutor);
                getCommand("mi").setTabCompleter(tabCompleter);
            }
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter) {
                getCommand(name).setTabCompleter((org.bukkit.command.TabCompleter) executor);
            }
        }
    }

    public FileConfiguration setupConfig(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            saveResource(path, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void loadAbilityTargetConfig() {
        File folder = new File(getDataFolder(), "Listener");
        if (!folder.exists()) folder.mkdirs();
        abilityTargetFile = new File(folder, "AbilityTarget.yml");
        if (!abilityTargetFile.exists()) {
            if (getResource("Listener/AbilityTarget.yml") != null) {
                saveResource("Listener/AbilityTarget.yml", false);
            } else {
                try { abilityTargetFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
        abilityTargetConfig = YamlConfiguration.loadConfiguration(abilityTargetFile);
    }

    private void checkHooks() {
        if (Bukkit.getPluginManager().getPlugin("MMOCore") != null) getLogger().info("§b[MyItem] Đã kết nối với MMOCore!");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) getLogger().info("§b[MyItem] Đã kết nối với PlaceholderAPI!");

        // Thêm kiểm tra MythicMobs
        if (org.ThienNguyen.Hook.MythicMobHook.isMythicMobsEnabled()) {
            getLogger().info("§b[MyItem] Đã kết nối với MythicMobs!");
        } else {
            getLogger().info("§e[MyItem] Không tìm thấy MythicMobs, tính năng rơi đồ quái custom sẽ bị tắt.");
        }
    }

    private void startEffectTask() {
        Effect effectHandler = new Effect();
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                effectHandler.updatePlayerEffects(player);
            }
        }, 20L, 40L);
    }
    // --- Các Getters bổ sung để sửa lỗi Build ---
    public FileConfiguration getExpireConfig() {
        if (this.expireConfig == null) {
            this.expireConfig = setupConfig("Listener/Expire.yml");
        }
        return this.expireConfig;
    }
    public FileConfiguration getDucLoConfig() {
        return ducLoConfig;
    }

    public FileConfiguration getGemConfig() {
        return gemConfig;
    }
    public FileConfiguration upgradeGemConfig() {
        return UpgradeGem;
    }

    public FileConfiguration getUpgradeGemConfig() {
        return UpgradeGem; // Trỏ về biến UpgradeGem (chứa upgrade/Gem.yml)
    }
    // Đảm bảo các hàm này cũng tồn tại nếu các class khác có gọi:
    public FileConfiguration getChuyenHoaConfig() {
        return chuyenHoaConfig;
    }
    public FileConfiguration getJewelryConfig() {
        // Vì bạn nói dùng config.yml, và trong onEnable bạn gán config.yml vào customConfig
        return customConfig;
    }
    public FileConfiguration getProtectionConfig() {
        return protectionConfig;
    }
    // --- Getters ---

    public FileConfiguration getSkillWeaponConfig() {
        return skillWeaponConfig;
    }
    public static Main getInstance() { return instance; }
    public FileConfiguration getCustomConfig() { return customConfig; }
    public FileConfiguration getGemTypeConfig() { return gemTypeConfig; }
    public FileConfiguration getEnchantConfig() { return enchantConfig; }
    public ItemDatabase getItemDatabase() { return itemDatabase; }
    public FileConfiguration getStatsConfig() { return statsConfig; }
    public FileConfiguration getEffectConfig() { return effectConfig; }
    public FileConfiguration getAbilityConfig() { return abilityConfig; }
    public FileConfiguration getStatsSettingsConfig() { return statsSettingsConfig; }
    public FileConfiguration getAbilityTargetConfig() { return abilityTargetConfig; }
    public FileConfiguration getElementConfig() { return elementConfig; }
    public FileConfiguration getElementLoreConfig() { return elementLoreConfig; }
    public FileConfiguration getSkillConfig() { return skillConfig; }
    public FileConfiguration getSkillCommandConfig() { return skillCommandConfig; }
    public FileConfiguration getSkillMythicLibConfig() { return skillMythicLibConfig; }
    public LanguageManager getLangManager() {
        return langManager;
    }
    public FileConfiguration getCustomListenerConfig() {
        if (this.customListenerConfig == null) {
            this.customListenerConfig = setupConfig("Listener/Custom.yml");
        }
        return this.customListenerConfig;
    }
    // web
    public FileConfiguration getTooltipConfig() {
        if (this.tooltipConfig == null) {
            this.tooltipConfig = setupConfig("Tooltip.yml");
        }
        return this.tooltipConfig;
    }
    public void syncStatsWithWeb() {
        // Chạy Async để không gây lag khi load server
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // 1. Đọc nội dung file Stats.yml
                File file = new File(getDataFolder(), "LoreFormat/Stats.yml");
                if (!file.exists()) return;
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                // 2. Chuyển đổi thành JSON
                com.google.gson.JsonObject statsJson = new com.google.gson.JsonObject();
                for (String key : config.getKeys(false)) {
                    statsJson.addProperty(key, config.getString(key));
                }

                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.addProperty("serverId", "server1"); // Có thể thay bằng tên server trong config
                payload.add("stats", statsJson);

                // 3. Gửi lên Web API (Dùng HttpClient của Java 21 bạn đang xài)
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://103.188.83.137/api/sync-stats")) // Thay bằng IP web của bạn
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                getLogger().info("§a[MyItemAPI] Đã đồng bộ với web Windycraft.com của Author");
            } catch (Exception e) {
                getLogger().warning("§c[WebAPI] Không thể kết nối tới Web để đồng bộ Stats!");
            }
        });
    }
    // Thêm vào class Main
    public void updatePlugin(String version, String downloadUrl) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                getLogger().info("§e[MyItem] Đang tải phiên bản " + version + "...");

                java.net.URL url = new java.net.URL(downloadUrl);
                File currentFile = getFile(); // Lấy file .jar hiện tại
                File updateFolder = new File(getDataFolder().getParentFile(), "update");

                if (!updateFolder.exists()) updateFolder.mkdirs();

                // Tải file vào thư mục update - Bukkit sẽ tự động thay thế khi Restart
                File destination = new File(updateFolder, currentFile.getName());

                try (java.io.InputStream in = url.openStream()) {
                    java.nio.file.Files.copy(in, destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                getLogger().info("§a[MyItem] Đã tải xong! Phiên bản " + version + " sẽ được áp dụng sau khi Restart server.");
                Bukkit.getConsoleSender().sendMessage("§e[MyItem] Khuyên dùng: Sử dụng /reload hoặc khởi động lại để cập nhật.");

            } catch (Exception e) {
                getLogger().severe("§c[MyItem] Lỗi khi cập nhật: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    public FileConfiguration getParticleConfig() {
        return this.particleConfig;
    }
    public FileConfiguration getTiersConfig() {
        if (this.tiersConfig == null) {
            reloadPluginConfigs();
        }
        return this.tiersConfig;
    }
    public FileConfiguration getConsumeConfig() {
        if (this.consumeConfig == null) {
            reloadPluginConfigs();
        }
        return this.consumeConfig;
    }
    public FileConfiguration getComboConfig() {
        if (this.comboConfig == null) {
            // Tên file cấu hình của bạn
            File file = new File(getDataFolder(), "itemcombo.yml");
            if (!file.exists()) {
                saveResource("itemcombo.yml", false);
            }
            this.comboConfig = YamlConfiguration.loadConfiguration(file);
        }
        return this.comboConfig;
    }

    // --- Thêm hàm lưu để dùng khi tạo combo mới (nếu cần) ---
    public void saveComboConfig() {
        try {
            if (comboConfig != null) {
                comboConfig.save(new File(getDataFolder(), "itemcombo.yml"));
            }
        } catch (IOException e) {
            getLogger().severe("§cKhông thể lưu itemcombo.yml!");
        }
    }
    public FileConfiguration getLoreFormatConfig() {
        // Nếu chưa load thì load luôn cho chắc
        if (this.loreFormatConfig == null) {
            this.loreFormatConfig = setupConfig("LoreFormat/Format.yml");
        }
        return this.loreFormatConfig;
    }
    public org.ThienNguyen.AI.AIProcessor getAiProcessor() {
        return aiProcessor;
    }
    public FileConfiguration getAIConfig() {
        if (aiConfig == null) {
            File aiFolder = new File(getDataFolder(), "AI");
            if (!aiFolder.exists()) aiFolder.mkdirs();

            File file = new File(aiFolder, "AIConfig.yml");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    YamlConfiguration defaultCfg = new YamlConfiguration();
                    defaultCfg.set("ai.tiny_font_enabled", true);
                    defaultCfg.save(file);
                    getLogger().info("§a[MyItem] Đã tạo file mặc định AI/AIConfig.yml");
                } catch (IOException e) {
                    getLogger().warning("§c[MyItem] Không thể tạo AIConfig.yml: " + e.getMessage());
                }
            }
            aiConfig = YamlConfiguration.loadConfiguration(file);
        }
        return aiConfig;
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
    public ItemStorageManager getItemStorageManager() {
        return itemStorageManager;
    }
    public org.ThienNguyen.Command.MiBrowseGUI getMiBrowseGUI() {
        return miBrowseGUI;
    }
    public static Economy getEconomy() {
        return econ;
    }
    public FileConfiguration getEvolutionConfig() {
        return this.evolutionConfig;
    }
    public StationDatabase getStationDatabase() {
        return stationDatabase;
    }
}