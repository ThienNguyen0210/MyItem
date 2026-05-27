package org.ThienNguyen.Ability;

import org.ThienNguyen.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AbilityManager {
    private static final Map<String, IAbility> abilities = new HashMap<>();

    static {
        
        registerDefaultAbilities();
    }

    /**
     * Đăng ký các kỹ năng Java thuần (Hard-coded).
     * Tách riêng để có thể gọi lại khi Reload plugin.
     */
    public static void registerDefaultAbilities() {
        register(new Lightning());
        register(new Poison());
        register(new Wither());
        register(new Hunger());
        register(new Tired());
        register(new Slow());
        register(new Confuse());
        register(new Weak());
        register(new Blind());
        register(new AirShock());
        register(new Curse());
        register(new Bubble());
        register(new Bleed());
        register(new FireVortex());
        register(new Freeze());
        register(new Disarm());
        register(new Explode());
        register(new FlamePulse());
        register(new Angel());
        register(new ShadowDevour());
        register(new SonicWave());
        register(new FireRain());
        register(new StarRitual());
        register(new FireTripleShot());
        register(new StarFall());
        register(new SunStrikeAOE());
        register(new BlackHole());
        register(new ShadowWave());
        register(new FairyChain());
        register(new LilacBloomBomb());
        register(new LeafStorm());
        register(new SpiritWolf());
        register(new PlagueSpread());
        register(new WindTornado());
        register(new FireOrb());
        register(new PlasmaOrb());
        register(new ElectricBlade());
        register(new FirstStrike());
        register(new TNTStuck());
        register(new BadLuck());
        register(new BubbleDeflector());
        register(new DarkFlame());
        register(new DarkImpact());
        register(new Roots());
        register(new Vampirism());
        register(new VenomSpread());
    }

    /**
     * Hàm quét thư mục Skript/Ability và nạp code động từ file .yml
     */
    public static void loadExternalAbilities() {
        File folder = new File(Main.getInstance().getDataFolder(), "Skript/Ability");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        int count = 0;
        for (File file : files) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String abilityName = file.getName().replace(".yml", "").toUpperCase();
                String scriptCode = config.getString("code");

                if (scriptCode != null && !scriptCode.isEmpty()) {
                    register(new ScriptAbility(abilityName, scriptCode));
                    count++;
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("[MyItem] Khong the nap ability tu file: " + file.getName());
            }
        }
        if (count > 0) {
            Bukkit.getLogger().info("[MyItem] Da nap thanh cong " + count + " ky nang tu Skript/Ability!");
        }
    }

    public static void register(IAbility ability) {
        if (ability != null) {
            abilities.put(ability.getName().toUpperCase(), ability);
        }
    }

    public static IAbility getAbility(String name) {
        if (name == null) return null;
        return abilities.get(name.toUpperCase());
    }

    /**
     * Xóa sạch danh sách và nạp lại từ đầu (Dùng khi Reload plugin)
     */
    public static void clearAbilities() {
        abilities.clear();
        
        registerDefaultAbilities();
        
    }
}