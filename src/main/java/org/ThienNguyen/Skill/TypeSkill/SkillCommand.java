package org.ThienNguyen.Skill.TypeSkill;

import me.clip.placeholderapi.PlaceholderAPI;
import org.ThienNguyen.Main;
import org.ThienNguyen.Skill.ISkill;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.io.File;

public class SkillCommand implements ISkill {

    private final String id;
    private String command;
    private String executeAs;

    public SkillCommand(String id) {
        this.id = id;
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(Main.getInstance().getDataFolder(), "Listener/SkillCommand.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.command = config.getString(id + ".command", "");
        this.executeAs = config.getString(id + ".execute_as", "console").toLowerCase();
    }

    @Override public String getName() { return id; }
    @Override public String getType() { return "Command"; }
    @Override public String getTrigger() { return "RIGHT_CLICK"; }

    @Override
    public void execute(Player player, LivingEntity target, int level, double baseDamage) {
        if (command == null || command.isEmpty()) return;

        
        String finalCommand = command.replace("%player%", player.getName())
                .replace("%level%", String.valueOf(level))
                .replace("%basedamage%", String.valueOf(baseDamage));

        if (target != null) {
            finalCommand = finalCommand.replace("%target%", target.getName());
        }

        
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            finalCommand = PlaceholderAPI.setPlaceholders(player, finalCommand);
        }

        
        final String cmdToRun = finalCommand; 

        switch (executeAs) {
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdToRun);

            case "op" -> {
                boolean wasOp = player.isOp();
                try {
                    if (!wasOp) player.setOp(true);
                    player.performCommand(cmdToRun);
                } finally {
                    if (!wasOp) player.setOp(false);
                }
            }

            default -> player.performCommand(cmdToRun);
        }
    }
}