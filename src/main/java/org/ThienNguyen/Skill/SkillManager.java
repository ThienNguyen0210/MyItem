package org.ThienNguyen.Skill;

import org.ThienNguyen.Main;
import org.ThienNguyen.Skill.TypeSkill.MythicMobsSkill;
import org.ThienNguyen.Skill.TypeSkill.SkillCommand;
import org.ThienNguyen.Skill.TypeSkill.ScriptSkill; 
import org.ThienNguyen.Skill.TypeSkill.Weapon.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SkillManager {
    private static final Map<String, ISkill> skills = new HashMap<>();
    private static final Map<String, Integer> manaCosts = new HashMap<>();

    public static void loadSkills() {
        skills.clear();
        manaCosts.clear();

        
        register(new SongAm());
        register(new LightningStrike());
        register(new FireballExplosionSkill());
        register(new TeleportSkill());
        register(new DestructiveLaserSkill());
        register(new AmaterasuSkill());
        register(new HakiBaVuongSkill());
        register(new DashSkill());
        register(new SoulReleaseSkill());
        register(new HellfireBreathSkill());
        register(new FireBlossomSkill());
        register(new DarknessDevourSkill());
        register(new DiaBocThienTinhSkill());
        register(new TiaChopLienHoanSkill());
        register(new ElectricFieldSkill());
        register(new EndCrystalSkill());
        register(new WindBladeSkill());
        register(new ThousandSwordsSkill());
        register(new DeathMarkSkill());
        register(new OmniSwordRainSkill());

        
        loadWeaponSkills();
        loadCommandSkills();
        loadMythicLibSkills();

        
        loadScriptSkills();

        Main.getInstance().getLogger().info("§a[Skill] Đã nạp tổng cộng " + skills.size() + " kỹ năng và đồng bộ Mana!");
    }

    private static void loadScriptSkills() {
        File folder = new File(Main.getInstance().getDataFolder(), "Skript/Skill");
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = file.getName().replace(".yml", "");
                String trigger = config.getString("trigger", "RIGHT_CLICK");
                String code = config.getString("code", "");
                int mana = config.getInt("mana", 0);

                if (!code.isEmpty()) {
                    register(new ScriptSkill(id, trigger, code));
                    manaCosts.put(id.toUpperCase(), mana);
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("§c[Skill] Lỗi khi nạp file script: " + file.getName());
            }
        }
    }

    private static void loadWeaponSkills() {
        File file = getConfigFile("SkillWeapon.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (ISkill skill : skills.values()) {
            if (skill != null && "Weapon".equalsIgnoreCase(skill.getType())) {
                String name = skill.getName();
                int mana = config.getInt(name + ".mana", 0);
                manaCosts.put(name.toUpperCase(), mana);
            }
        }
    }

    private static void loadCommandSkills() {
        File file = getConfigFile("SkillCommand.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            register(new SkillCommand(key));
            int mana = config.getInt(key + ".mana", 0);
            manaCosts.put(key.toUpperCase(), mana);
        }
    }

    private static void loadMythicLibSkills() {
        File file = getConfigFile("SkillMythicMob.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            register(new MythicMobsSkill(Main.getInstance(), key));
            int mana = config.getInt(key + ".mana", 0);
            manaCosts.put(key.toUpperCase(), mana);
        }
    }

    private static File getConfigFile(String fileName) {
        File folder = new File(Main.getInstance().getDataFolder(), "Listener");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, fileName);
        if (!file.exists()) {
            try {
                Main.getInstance().saveResource("Listener/" + fileName, false);
            } catch (Exception e) {
                try { file.createNewFile(); } catch (Exception ignored) {}
            }
        }
        return file;
    }

    private static void register(ISkill skill) {
        if (skill == null || skill.getName() == null) return;
        skills.put(skill.getName().toUpperCase(), skill);
    }

    public static int getManaCost(String skillName) {
        if (skillName == null) return 0;
        return manaCosts.getOrDefault(skillName.toUpperCase(), 0);
    }

    public static ISkill getSkill(String name) {
        if (name == null) return null;
        return skills.get(name.toUpperCase());
    }

    public static List<ISkill> getSkillsByType(String type) {
        return skills.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public static List<String> getSkillNamesByType(String type) {
        FileConfiguration config = null;
        if (type.equalsIgnoreCase("Mythicmob")) {
            config = YamlConfiguration.loadConfiguration(getConfigFile("SkillMythicMob.yml"));
        } else if (type.equalsIgnoreCase("Command")) {
            config = YamlConfiguration.loadConfiguration(getConfigFile("SkillCommand.yml"));
        } else if (type.equalsIgnoreCase("Script")) {
            
            File folder = new File(Main.getInstance().getDataFolder(), "Skript/Skill");
            if (folder.exists()) {
                File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
                if (files != null) {
                    List<String> names = new ArrayList<>();
                    for (File f : files) names.add(f.getName().replace(".yml", ""));
                    return names;
                }
            }
        }

        if (config != null) return new ArrayList<>(config.getKeys(false));

        return skills.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .map(ISkill::getName)
                .collect(Collectors.toList());
    }

    public static List<String> getSkillNames() {
        return skills.values().stream()
                .map(ISkill::getName)
                .collect(Collectors.toList());
    }
}