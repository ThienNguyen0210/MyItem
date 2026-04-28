package org.ThienNguyen.Webapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ThienNguyen.Main;
import org.ThienNguyen.Stat.*;
import org.ThienNguyen.Ability.AbilityData;
import org.ThienNguyen.Effect.BuffData;
import org.ThienNguyen.Lore.AbilityLore;
import org.ThienNguyen.Lore.EffectLore;
import org.ThienNguyen.Lore.StatsLore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Web {

    private static final String API_URL = "http://103.188.83.137/api/get-item/";

    public static void connectItem(Player player, String code) {
        player.sendMessage("§e[MyItem] Đang kết nối tới máy chủ thiết kế...");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + code))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            giveItem(player, json);
                        });
                    } else {
                        player.sendMessage("§c[MyItem] Mã kết nối không tồn tại!");
                    }
                })
                .exceptionally(ex -> {
                    player.sendMessage("§c[MyItem] Lỗi kết nối API!");
                    ex.printStackTrace();
                    return null;
                });
    }

    private static void giveItem(Player player, JsonObject json) {
        try {
            // 1. Khởi tạo vật phẩm cơ bản
            String matName = json.has("material") ? json.get("material").getAsString().toUpperCase() : "DIAMOND_SWORD";
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.STONE;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            // Thiết lập tên và CustomModelData
            if (json.has("name")) meta.setDisplayName(formatColor(json.get("name").getAsString()));
            if (json.has("modelId") && !json.get("modelId").getAsString().isEmpty()) {
                try {
                    meta.setCustomModelData(json.get("modelId").getAsInt());
                } catch (Exception ignored) {}
            }
            item.setItemMeta(meta);

            // 2. GÁN DỮ LIỆU VÀO PERSISTENT DATA CONTAINER (PDC)
            // Lưu dữ liệu thô vào item trước khi render lore
            if (json.has("stats")) {
                JsonObject statsJson = json.get("stats").getAsJsonObject();
                for (String type : statsJson.keySet()) {
                    com.google.gson.JsonElement el = statsJson.get(type);
                    if (el != null && !el.isJsonNull() && !el.getAsString().isEmpty()) {
                        try {
                            applyStatByCase(item, type, el.getAsDouble());
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (json.has("abilities")) {
                JsonObject abJson = json.get("abilities").getAsJsonObject();
                for (String abName : abJson.keySet()) {
                    JsonObject info = abJson.get(abName).getAsJsonObject();
                    if (info.has("lv") && !info.get("lv").getAsString().isEmpty()) {
                        int lv = info.get("lv").getAsInt();
                        double chance = (info.has("chance") && !info.get("chance").getAsString().isEmpty())
                                ? info.get("chance").getAsDouble() : 100.0;
                        AbilityData.setAbility(item, abName, lv, chance);
                    }
                }
            }

            if (json.has("effects")) {
                JsonObject effJson = json.get("effects").getAsJsonObject();
                for (String effName : effJson.keySet()) {
                    com.google.gson.JsonElement el = effJson.get(effName);
                    if (el != null && !el.getAsString().isEmpty()) {
                        BuffData.setEffect(item, effName, el.getAsInt());
                    }
                }
            }

            if (json.has("elements")) {
                JsonObject eleJson = json.get("elements").getAsJsonObject();
                for (String eleId : eleJson.keySet()) {
                    com.google.gson.JsonElement el = eleJson.get(eleId);
                    if (el != null && !el.getAsString().isEmpty()) {
                        int level = el.getAsInt();
                        if (level > 0) org.ThienNguyen.Element.ElementCore.addElement(item, eleId, level);
                    }
                }
            }

            // 3. XỬ LÝ LORE TEMPLATE (Quan trọng: Biến template phải nằm trong block này)
            if (json.has("lore_template")) {
                String template = json.get("lore_template").getAsString();

                // 3.1 Render Stats
                if (json.has("stats")) {
                    JsonObject statsJson = json.get("stats").getAsJsonObject();
                    for (String type : statsJson.keySet()) {
                        String tag = "{" + type + "}";
                        if (template.contains(tag)) {
                            com.google.gson.JsonElement el = statsJson.get(type);
                            String replacement = (el != null && !el.isJsonNull() && !el.getAsString().isEmpty())
                                    ? StatsLore.getFormattedLore(item, type, el.getAsDouble()) : "";
                            template = template.replace(tag, replacement);
                        }
                    }
                }

                // 3.2 Render Abilities
                if (json.has("abilities")) {
                    JsonObject abJson = json.get("abilities").getAsJsonObject();
                    for (String abName : abJson.keySet()) {
                        String tag = "{" + abName + "}";
                        if (template.contains(tag)) {
                            JsonObject info = abJson.get(abName).getAsJsonObject();
                            int lv = info.has("lv") ? info.get("lv").getAsInt() : 1;
                            double chance = info.has("chance") ? info.get("chance").getAsDouble() : 100.0;
                            template = template.replace(tag, getFormattedAbility(abName, lv, chance));
                        }
                    }
                }

                // 3.3 Render Elements
                if (json.has("elements")) {
                    JsonObject eleJson = json.get("elements").getAsJsonObject();
                    for (String eleId : eleJson.keySet()) {
                        String tag = "{" + eleId + "}";
                        if (template.contains(tag)) {
                            com.google.gson.JsonElement el = eleJson.get(eleId);
                            String replacement = "";
                            if (el != null && !el.getAsString().isEmpty()) {
                                int level = el.getAsInt();
                                if (level > 0) {
                                    // GỌI ĐÚNG HÀM TỪ CLASS ElementLore
                                    replacement = org.ThienNguyen.Lore.ElementLore.getFormattedElement(eleId, level);
                                }
                            }
                            template = template.replace(tag, replacement);
                        }
                    }
                }

                // 3.4 Render Effects
                if (json.has("effects")) {
                    JsonObject effJson = json.get("effects").getAsJsonObject();
                    for (String effName : effJson.keySet()) {
                        String tag = "{" + effName + "}";
                        if (template.contains(tag)) {
                            int level = effJson.get(effName).getAsInt();
                            template = template.replace(tag, getFormattedEffect(effName, level));
                        }
                    }
                }

                // 4. CẬP NHẬT LORE CUỐI CÙNG
                List<String> finalLore = new ArrayList<>();
                for (String line : template.split("\n")) {
                    String formatted = formatColor(line);
                    if (!ChatColor.stripColor(formatted).trim().isEmpty() || formatted.contains("§")) {
                        finalLore.add(formatted);
                    }
                }

                ItemMeta finalMeta = item.getItemMeta();
                if (finalMeta != null) {
                    finalMeta.setLore(finalLore);
                    item.setItemMeta(finalMeta);
                }
            }

            // Trao vật phẩm cho người chơi
            player.getInventory().addItem(item);
            player.sendMessage("§a[MyItem] §fKết nối thành công: " + item.getItemMeta().getDisplayName());

        } catch (Exception e) {
            player.sendMessage("§c[MyItem] Lỗi cấu trúc dữ liệu");
            e.printStackTrace();
        }
    }

    // Helper xử lý màu Hex + Color code truyền thống
    private static String formatColor(String text) {
        if (text == null || text.isEmpty()) return text;
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = hexPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static String getFormattedAbility(String key, int level, double chance) {
        FileConfiguration config = Main.getInstance().getAbilityConfig();
        List<String> template = config.getStringList("abilities." + key + ".lore");
        boolean useRoman = config.getBoolean("settings.use-roman", false);

        if (template.isEmpty()) return "§7- " + key + " Lv." + level;

        String levelToDisplay = useRoman ? toRoman(level) : String.valueOf(level);

        List<String> lines = new ArrayList<>();
        for (String line : template) {
            String formatted = line
                    .replace("{level}", levelToDisplay)
                    .replace("{chance}", String.valueOf(chance));
            lines.add(formatColor(formatted)); // Format màu HEX trong template
        }

        return String.join("\n", lines);
    }

    private static String getFormattedElement(String eleId, int level) {
        FileConfiguration config = Main.getInstance().getElementLoreConfig();
        String format = config.getString(eleId, "&7" + eleId + ": {value}");
        boolean useRoman = config.getBoolean("settings.use-roman", false);
        String val = useRoman ? toRoman(level) : String.valueOf(level);
        return formatColor(format.replace("{value}", val));
    }

    public static String getFormattedEffect(String key, int level) {
        FileConfiguration config = Main.getInstance().getEffectConfig();

        String displayName = config.getString("display-names." + key, key);
        boolean useRoman = config.getBoolean("use-roman", true);
        String levelStr = useRoman ? toRoman(level) : String.valueOf(level);

        // Hard-code format sạch, không dấu -, có thể tùy chỉnh màu/icon
        String result = "§7" + displayName + " §f" + levelStr;
        // Hoặc đẹp hơn tí:
        // String result = "§b✦ " + displayName + " §f" + levelStr;

        return formatColor(result);
    }

    private static final java.util.TreeMap<Integer, String> romanMap = new java.util.TreeMap<>();
    static {
        romanMap.put(1000, "M"); romanMap.put(900, "CM"); romanMap.put(500, "D");
        romanMap.put(400, "CD"); romanMap.put(100, "C"); romanMap.put(90, "XC");
        romanMap.put(50, "L"); romanMap.put(40, "XL"); romanMap.put(10, "X");
        romanMap.put(9, "IX"); romanMap.put(5, "V"); romanMap.put(4, "IV");
        romanMap.put(1, "I");
    }

    private static String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        Integer l = romanMap.floorKey(number);
        if (l == null) return String.valueOf(number);
        if (number == l) return romanMap.get(number);
        return romanMap.get(l) + toRoman(number - l);
    }

    private static void applyStatByCase(ItemStack item, String type, double value) {
        if (value <= 0) return;
        switch (type.toLowerCase()) {
            case "damage" -> Damage.setDamage(item, value);
            case "health" -> Health.setHealth(item, value);
            case "armor" -> Armor.setArmor(item, value);
            case "pve_damage" -> PveDamage.set(item, value);
            case "pvp_damage" -> PvpDamage.set(item, value);
            case "pve_defense" -> PveDefense.set(item, value);
            case "pvp_defense" -> PvpDefense.set(item, value);
            case "critical_chance" -> CriticalChance.set(item, value);
            case "critical_damage" -> CriticalDamage.set(item, value);
            case "lifesteal" -> Lifesteal.set(item, value);
            case "dodge_rate" -> DodgeRate.set(item, value);
            case "block_rate" -> BlockRate.set(item, value);
            case "penetration" -> Penetration.set(item, value);
            case "level_require" -> LevelRequire.set(item, (int) value);
            case "true_damage" -> TrueDamage.set(item, value);
            case "thorns" -> Thorns.set(item, value);
            case "max_mana" -> MaxMana.set(item, value);
            case "mana_regen" -> ManaRegen.set(item, value);
            case "exp_bonus" -> ExpBonus.set(item, value);
            case "attack_speed" -> AttackSpeed.set(item, value);
            case "movement_speed" -> MovementSpeed.set(item, value);
            case "health_regen" -> HealthRegen.set(item, value);
            case "armor_pen" -> ArmorPen.set(item, value);
            case "all_damage" -> AllDamage.set(item, value);           // Sát thương toàn phần (%)
            case "all_defense" -> AllDefense.set(item, value);         // Giảm damage toàn phần (%)
            case "bow_damage" -> BowDamage.set(item, value);           // Sát thương cung tên
            case "knockback_resistance" -> KnockbackResistance.set(item, value);
            case "durability" -> {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                    // 1. Lưu vào PDC (Số hiển thị trong Lore)
                    var pdc = meta.getPersistentDataContainer();
                    pdc.set(new NamespacedKey(Main.getInstance(), "durability"), PersistentDataType.DOUBLE, value);
                    pdc.set(new NamespacedKey(Main.getInstance(), "max_durability"), PersistentDataType.DOUBLE, value);

                    // 2. Set độ bền Vanilla (Minecraft mặc định)
                    damageable.setDamage(0); // 0 damage = Thanh độ bền đầy 100%
                    item.setItemMeta(damageable);
                }
            }
        }
    }
}