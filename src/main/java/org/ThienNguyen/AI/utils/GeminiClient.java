package org.ThienNguyen.AI.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.ThienNguyen.Main;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiClient {

    
    private final String HARDCODED_API_KEY = "AIzaSyAOwahO33Ue7n1Ao1gCAJdCSY_CQ1S_j_Y";

    
    private String getApiKey() {
        FileConfiguration aiCfg = Main.getInstance().getAIConfig();

        if (aiCfg != null) {
            String userKey = aiCfg.getString("api-key", "").trim();

            
            if (!userKey.isEmpty() && !userKey.equals("YOUR_API_KEY_HERE") && !userKey.equals("AIzaSy...")) {
                Bukkit.getLogger().info("[WindyAI] Đang sử dụng API Key tùy chỉnh từ AIConfig.yml");
                return userKey;
            }
        }

        
        Bukkit.getLogger().info("[WindyAI] Không tìm thấy API Key từ config → Sử dụng hard-coded key");
        return HARDCODED_API_KEY;
    }

    
    private String getGeminiUrl() {
        String apiKey = getApiKey();
        
        return "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey;
    }

    public String callGemini(String prompt) throws Exception {
        String url = getGeminiUrl();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        
        StringBuilder styleGuide = new StringBuilder(
                "QUY TẮC TRANG TRÍ LORE (BẮT BUỘC):\n" +
                        "- Sử dụng kí tự đặc biệt đẹp: ✦ ✧ ✪ ❖ ❈ ❂ ❁ ⫷ ⫸ ▣ ◈ ✹ ✵ ☠ ⚔\n" +
                        "- Dùng line phân cách nghệ thuật (divider)\n" +
                        "- Spacing hợp lý, không spam ký tự\n" +
                        "- Mỗi section phải có icon riêng\n" +
                        "- Tạo cảm giác 'RPG cao cấp / fantasy'\n" +
                        "- Dùng mã hex &#RRGGBB, không ghi hoa toàn bộ\n" +
                        "- Lore không quá dài, 1 dòng tối đa ~32 ký tự\n" +
                        "- Tránh màu quá tối độ tối được phép &8 (tooltip dễ mờ)\n" +
                        "- Stats, element, ability, effect phải tách riêng, không gộp chung\n" +
                        "- các line như chỉ số, hiệu ứng,  ability , element v.v chỉ ghi đơn giản thôi không màu mè nhé\n" +
                        "- Không dùng text thô đơn giản\n\n"
        );

        FileConfiguration aiCfg = Main.getInstance().getAIConfig();

        
        if (aiCfg != null && aiCfg.getBoolean("ai.tiny_font_enabled", false)) {
            styleGuide.append(
                    "- Nếu lore có flavor text, quote cuối, hoặc tiêu đề phụ nghệ thuật, bạn CÓ THỂ dùng small caps unicode (hybrid) để tăng tính thẩm mỹ.\n" +
                            "- Ví dụ: \"Sức mạnh hủy diệt từ bóng tối\" → \"sứᴄ ᴍạɴʜ ʜủʏ ᴅɪệᴛ ᴛừ ʙóɴɢ ᴛốɪ\"\n" +
                            "- CHỈ dùng ở phần văn bản nghệ thuật / quote / mô tả, TUYỆT ĐỐI KHÔNG dùng cho stats, effect, ability, element.\n\n"
            );
        }

        boolean isLevelRequireEnabled = aiCfg != null && aiCfg.getBoolean("ai.level_require", false);
        boolean isUnbreaking = aiCfg != null && aiCfg.getBoolean("ai.unbreaking", false);

        String validStats = "[damage, health, armor, pve_damage, pvp_damage, pve_defense, pvp_defense, critical_chance, critical_damage, lifesteal, dodge_rate, block_rate, penetration, level_require, true_damage, thorns, max_mana, mana_regen, exp_bonus, attack_speed, movement_speed, health_regen, armor_pen, all_damage, all_defense, bow_damage, knockback_resistance, death_damage, durability, magic_damage, magic_defense]";
        String finalStats = isLevelRequireEnabled
                ? validStats.replace("]", ", level_require]")
                : validStats;

        String validAbilities = "[LIGHTNING, POISON, WEAK, HUNGER, TIRED, CONFUSE, WITHER, BLIND, SLOW, AIR_SHOCK, CURSE, BUBBLE, BLEED, FIRE_VORTEX, FREEZE, DISARM, EXPLODE, FLAME_PULSE, ANGEL, SHADOW_DEVOUR, SONIC_WAVE, FIRE_RAIN, STAR_RITUAL, FIRE_TRIPLE_SHOT, STAR_FALL, SUN_STRIKE_AOE, BLACK_HOLE, SHADOW_WAVE, FAIRY_CHAIN, LILAC_BLOOM_BOMB, LEAF_STORM, SPIRIT_WOLF, PLAGUE_SPREAD, WIND_TORNADO, FIRE_ORB, PLASMA_ORB, ELECTRIC_BLADE, FIRST_STRIKE, TNT_STUCK, BadLuck, Bubble_Deflector, DARK_FLAME, DARK_IMPACT, ROOTS, VAMPIRISM, VENOM_SPREAD]";
        String validElements = getDynamicElements();
        String validEffect = "[SPEED, FAST_DIGGING, INCREASE_DAMAGE, JUMP, REGENERATION, DAMAGE_RESISTANCE, FIRE_RESISTANCE, WATER_BREATHING, HEALTH_BOOST, ABSORPTION, NIGHT_VISION, LUCK]";

        
        String systemInstruction = styleGuide.toString() +
                "Bạn là chuyên gia thiết kế Template Item RPG cho Minecraft Plugin.\n" +
                "NHIỆM VỤ: Trả về DUY NHẤT mã YAML thô, KHÔNG giải thích, KHÔNG thêm text ngoài YAML.\n\n" +
                "QUY TẮC DỮ LIỆU (BẮT BUỘC):\n" +
                "1. Material: Dùng Material Minecraft (Ví dụ: NETHERITE_SWORD).\n" +
                "2. Display Name: Dùng mã HEX &#RRGGBB có thể kết hợp &l cho gradient.\n" +
                "3. Stats: Giá trị là số thực (Double).\n" +
                "4. Ability: Định dạng 'LEVEL:CHANCE'. Ví dụ: LIGHTNING: '1:50.0'\n" +
                "5. Elements/Effects: Giá trị là số nguyên (Integer) đại diện Level.\n" +
                (isUnbreaking ? "6. Unbreaking: Luôn thêm dòng 'unbreaking: true' vào root của YAML.\n" : "") +
                "\n" +
                "QUY TẮC LORE (PLACEHOLDERS):\n" +
                "- Sử dụng đúng tag để plugin tự render: {tier:ID} | {stats:KEY} | {ability:ID} | {element:ID} | {effect:ID}\n" +
                "- Dòng nào có placeholder thì chỉ ghi đúng placeholder đó, không thêm chữ trước/sau.\n" +
                "- DANH MỤC KEY HỢP LỆ:\n" +
                "- STATS: " + finalStats + "\n" +
                "- ABILITIES: " + validAbilities + "\n" +
                "- ELEMENTS: " + validElements + "\n" +
                "- EFFECT: " + validEffect + "\n\n" +
                "Cac lore đẹp bạn có thể dùng format giống: &6&l⫷&e&l⟦&#FBDA61&l❖&e&l⟧&6&l⫸&4&m---&r&6&l⫷&e&l⟦&#FBDA61&l❖&e&l⟧&6&l⫸ &f&lChỉ Số &6&l⫷&e&l⟦&#FBDA61&l❖&e&l⟧&6&l⫸&4&m---&r&6&l⫷&e&l⟦&#FBDA61&l❖&e&l⟧&6&l⫸\n" +
                "CẤU TRÚC YAML MẪU:\n" +
                "material: DIAMOND_SWORD\n" +
                "display_name: '&#FBDA61&lT&#F7B733&lh&#F7B733&li&#F7B733&lê&#F7B733&ln &#F7B733&lK&#F7B733&li&#F7B733&lế&#F7B733&lm'\n" +
                "tier: MYTHIC\n" +
                "stats:\n  damage: 150.0\n  critical_chance: 25.0\n" +
                (isLevelRequireEnabled ? "  level_require: 10.0\n" : "") +
                "ability:\n  LIGHTNING: '3:10.0'\n" +
                "elements:\n  FIRE: 5\n" +
                "effects:\n  SPEED: 2\n" +
                "lore:\n" +
                " - '&6&l|⟦&e&l亗&6&l⟧|&8◥◣︿◢◤&6&l|⟦&e&l亗&6&l⟧|&f&l Thông Tin &6&l|⟦&e&l亗&6&l⟧|&8◥◣︿◢◤&6&l|⟦&e&l亗&6&l⟧|'\n" +
                " - '&7&o ức mạnh từ bóng tối....'\n" +
                " - '&f'\n" +
                " - ' &7Phẩm Chất: {tier:LEGENDARY}'\n" +
                " - ' &7Thuộc Loại: &c&lSWORD'\n" +
                " - '&f'\n" +
                " - '&f &f &f &f&6&l|⟦&e&l亗&6&l⟧|&f&l Chỉ Số'\n" +
                " - ' {stats:damage}'\n" +
                " - ' {stats:critical_chance}'\n" +
                " - ' {stats:armor_pen}'\n" +
                " - '&f'\n" +
                " - '&f &f &f &f&6&l|⟦&e&l亗&6&l⟧|&f&l Hiệu Ứng'\n" +
                " - ' {effect:INCREASE_DAMAGE}'\n" +
                " - '&f &f &f &f&6&l|⟦&e&l亗&6&l⟧|&f&l Nguyên Tố'\n" +
                " - ' {element:FIRE}'\n" +
                " - '&f'\n" +
                " - '&f &f &f &f&6&l|⟦&e&l亗&6&l⟧|&f&l Kĩ Năng'\n" +
                " - '{ability:LIGHTNING}'\n" +
                " - '&6&l|⟦&e&l亗&6&l⟧|&8◥◣︿◢◤&6&l|⟦&e&l亗&6&l⟧|&f&l Kết Thúc &6&l|⟦&e&l亗&6&l⟧|&8◥◣︿◢◤&6&l|⟦&e&l亗&6&l⟧|'\n";

        
        JSONObject body = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", systemInstruction));
        parts.put(new JSONObject().put("text", "Yêu cầu từ người chơi: " + prompt));

        JSONObject content = new JSONObject().put("parts", parts);
        JSONArray contents = new JSONArray().put(content);
        body.put("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject resJson = new JSONObject(response.body());

        if (resJson.has("error")) {
            String errorMsg = resJson.getJSONObject("error").getString("message");
            Bukkit.getLogger().warning("[WindyAI] Gemini API Error: " + errorMsg);
            throw new Exception("Google API Error: " + errorMsg);
        }

        
        String rawText = resJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        String cleanText = rawText.replaceAll("(?i)```yaml", "")
                .replaceAll("(?i)```", "")
                .trim();

        
        if (!cleanText.contains("display_name:") && cleanText.contains(":")) {
            int startIndex = cleanText.lastIndexOf("\n", cleanText.indexOf(":"));
            if (startIndex != -1) {
                cleanText = cleanText.substring(startIndex).trim();
            }
        }

        if (!cleanText.toLowerCase().contains("display_name")) {
            Bukkit.getLogger().warning("[WindyAI] AI trả về nội dung không đúng định dạng:\n" + rawText);
            throw new Exception("AI không tạo đúng cấu trúc MyItem. Hãy thử mô tả kỹ hơn.");
        }

        return cleanText;
    }

    private String getDynamicElements() {
        try {
            FileConfiguration elementConfig = Main.getInstance().getElementConfig();
            if (elementConfig == null) {
                return "[FIRE, WATER, ICE, LIGHTNING, DARK, HOLY, WIND, EARTH]";
            }
            java.util.Set<String> keys = elementConfig.getKeys(false);
            if (keys.isEmpty()) return "[]";
            return "[" + String.join(", ", keys).toUpperCase() + "]";
        } catch (Exception e) {
            return "[FIRE, WATER, ICE, LIGHTNING, DARK, HOLY, WIND, EARTH]";
        }
    }
}