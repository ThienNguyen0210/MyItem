package org.ThienNguyen.Listener.Station;

import java.util.Map;

public class StationData {
    private Map<String, Double> stats;
    private String rawAbilities; // Lưu chuỗi kỹ năng gốc
    private Map<String, Integer> effects;
    private Map<String, Integer> elements; // Thêm dòng này
    public Map<String, Integer> getElements() {
        return elements;
    }
    // Getter & Setter
    public Map<String, Double> getStats() { return stats; }
    public void setStats(Map<String, Double> stats) { this.stats = stats; }

    public String getRawAbilities() { return rawAbilities; }
    public void setRawAbilities(String rawAbilities) { this.rawAbilities = rawAbilities; }

    public Map<String, Integer> getEffects() { return effects; }
    public void setEffects(Map<String, Integer> effects) { this.effects = effects; }
}