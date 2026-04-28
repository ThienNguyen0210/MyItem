package org.ThienNguyen.GemSocket;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

public class GemStone {
    private final String id;
    private final String displayName;
    private final List<String> lore;
    private final int modelId;
    private final ConfigurationSection applySection;

    public GemStone(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display-name");
        this.lore = config.getStringList("lore");
        this.modelId = config.getInt("model-id", 0);
        this.applySection = config.getConfigurationSection("apply");
    }

    // Getters...
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ConfigurationSection getApplySection() { return applySection; }
}