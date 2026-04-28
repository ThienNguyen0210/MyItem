package org.ThienNguyen.Listener.Station;

public class StationFullData {
    private final String dataJson;
    private final String loreJson;
    private final String displayName;
    private final Integer customModelData;
    private final String pdcJson;        // ← MỚI
    private final int version;

    public StationFullData(String dataJson, String loreJson, String displayName,
                           Integer customModelData, String pdcJson, int version) {
        this.dataJson = dataJson;
        this.loreJson = loreJson;
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.pdcJson = pdcJson;
        this.version = version;
    }

    public String getDataJson() { return dataJson; }
    public String getLoreJson() { return loreJson; }
    public String getDisplayName() { return displayName; }
    public Integer getCustomModelData() { return customModelData; }
    public String getPdcJson() { return pdcJson; }          // ← MỚI
    public int getVersion() { return version; }
}