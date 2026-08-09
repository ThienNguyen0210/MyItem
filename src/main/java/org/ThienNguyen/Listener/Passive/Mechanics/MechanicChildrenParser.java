package org.ThienNguyen.Listener.Passive.Mechanics;

import org.ThienNguyen.Listener.Passive.MechanicRegistry;
import org.ThienNguyen.Listener.Passive.PassiveMechanic;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MechanicChildrenParser {

    private MechanicChildrenParser() {}

    public static List<PassiveMechanic> parse(ConfigurationSection cfg, String key) {
        List<PassiveMechanic> result = new ArrayList<>();
        if (cfg == null) return result;

        List<?> rawList = cfg.getMapList(key);
        for (Object obj : rawList) {
            if (obj instanceof Map<?, ?> map) {
                ConfigurationSection childSection = toSection(map);
                PassiveMechanic m = MechanicRegistry.create(childSection);
                if (m != null) result.add(m);
            }
        }
        return result;
    }

    private static ConfigurationSection toSection(Map<?, ?> map) {
        MemoryConfiguration memCfg = new MemoryConfiguration();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            memCfg.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        return memCfg;
    }
}