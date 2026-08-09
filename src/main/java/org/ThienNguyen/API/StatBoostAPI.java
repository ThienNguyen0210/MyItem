package org.ThienNguyen.API;

import org.ThienNguyen.Listener.CacheListener;
import org.ThienNguyen.Listener.PlayerCombatCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API cho phép plugin khác cộng/nhân chỉ số cho người chơi (flat hoặc percent),
 * áp dụng LẶP LẠI mỗi lần stats được refresh (đổi đồ, respawn, join...) — khác với
 * PlayerRefreshStatsEvent#addStat() vốn chỉ cộng 1 lần cho đúng chu kỳ refresh đó rồi mất.
 *
 * Boost đăng ký ở đây tồn tại độc lập với CombatStats, tương tự cơ chế TempBuff nhưng
 * KHÔNG tự hết hạn — chỉ mất khi plugin gọi removeBoost()/clearAll(), hoặc server restart
 * (dữ liệu chỉ nằm trong RAM, không tự lưu xuống đĩa).
 *
 * Ví dụ dùng từ plugin khác:
 * <pre>
 *   // Cộng thẳng 5 damage, key duy nhất để sau này gỡ đúng effect này
 *   StatBoostAPI.addFlatBoost(uuid, "myplugin:quest_reward", "damage", 5.0);
 *
 *   // Cộng 10% toàn bộ damage (nhân sau khi đã cộng flat)
 *   StatBoostAPI.addPercentBoost(uuid, "myplugin:vip_rank", "all_damage", 10.0);
 *
 *   // Gỡ bỏ khi hết hiệu lực (hết VIP, hoàn thành nhiệm vụ khác, v.v.)
 *   StatBoostAPI.removeBoost(uuid, "myplugin:vip_rank");
 * </pre>
 *
 * sourceId nên đặt dạng "tenplugin:ten_hieu_ung" để tránh đụng độ giữa các plugin khác nhau,
 * và để đăng ký lại cùng sourceId chỉ ghi đè giá trị cũ thay vì cộng dồn vô hạn.
 *
 * statKey phải là 1 trong các tên hợp lệ ở PlayerCombatCache.getKnownStatKeys() (vd "damage",
 * "critical_chance", "armor", "health", "max_mana"...). Sai tên sẽ bị log warning và bỏ qua,
 * KHÔNG throw exception, để 1 plugin gõ sai không kéo sập plugin khác.
 */
public final class StatBoostAPI {

    public enum BoostType { FLAT, PERCENT }

    public record StatBoost(String statKey, BoostType type, double value) {}

    // uuid -> sourceId -> boost đang active
    private static final Map<UUID, Map<String, StatBoost>> BOOSTS = new ConcurrentHashMap<>();

    private StatBoostAPI() {}

    /** Cộng thẳng {@code amount} vào stat gốc, giữ nguyên mỗi lần refreshCache() chạy. */
    public static void addFlatBoost(UUID uuid, String sourceId, String statKey, double amount) {
        register(uuid, sourceId, new StatBoost(normalize(statKey), BoostType.FLAT, amount));
    }

    /**
     * Nhân thêm {@code percent}% vào stat (vd 10.0 nghĩa là +10%), áp dụng SAU tất cả các
     * boost FLAT (kể cả flat của chính StatBoostAPI lẫn flat từ equipment/gem/combo), giống
     * cách applyPercentStats()/AttributeAPI percent đang hoạt động trong CacheListener.
     */
    public static void addPercentBoost(UUID uuid, String sourceId, String statKey, double percent) {
        register(uuid, sourceId, new StatBoost(normalize(statKey), BoostType.PERCENT, percent));
    }

    private static void register(UUID uuid, String sourceId, StatBoost boost) {
        if (uuid == null || sourceId == null || sourceId.isBlank()) return;
        if (!PlayerCombatCache.isKnownStat(boost.statKey())) {
            Bukkit.getLogger().warning("[StatBoostAPI] Unknown stat key '" + boost.statKey()
                    + "' from source '" + sourceId + "'. Valid keys: " + PlayerCombatCache.getKnownStatKeys());
            return;
        }
        BOOSTS.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(sourceId, boost);
        refresh(uuid);
    }

    /** Gỡ 1 boost cụ thể theo sourceId. Trả về true nếu có boost bị gỡ. */
    public static boolean removeBoost(UUID uuid, String sourceId) {
        Map<String, StatBoost> map = BOOSTS.get(uuid);
        if (map == null) return false;
        boolean removed = map.remove(sourceId) != null;
        if (removed) refresh(uuid);
        return removed;
    }

    /** Gỡ toàn bộ boost của 1 người chơi (vd gọi khi cần reset sạch). */
    public static void clearAll(UUID uuid) {
        if (BOOSTS.remove(uuid) != null) refresh(uuid);
    }

    /** Xem danh sách boost đang active của 1 người chơi (chỉ đọc, dùng để debug/hiển thị). */
    public static Map<String, StatBoost> getBoosts(UUID uuid) {
        return Map.copyOf(BOOSTS.getOrDefault(uuid, Map.of()));
    }

    private static void refresh(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            CacheListener.refreshCache(p);
        }
    }

    /**
     * Gọi NỘI BỘ từ CacheListener#refreshCache(), sau khi mọi field gốc (equipment, gem,
     * combo, MyAttribute...) đã được tính xong. Cộng flat trước, nhân percent sau — áp dụng
     * trực tiếp lên field CombatStats qua reflection (dùng chung registry tên stat của
     * PlayerCombatCache), nên bất kỳ stat nào đã có trong STAT_NAME_TO_FIELD đều tự động
     * được hỗ trợ ở đây, không cần thêm case nào cả khi có stat mới.
     */
    public static void apply(UUID uuid, PlayerCombatCache.CombatStats stats) {
        Map<String, StatBoost> map = BOOSTS.get(uuid);
        if (map == null || map.isEmpty()) return;

        for (StatBoost b : map.values()) {
            if (b.type() == BoostType.FLAT) {
                PlayerCombatCache.addToField(stats, b.statKey(), b.value());
            }
        }
        for (StatBoost b : map.values()) {
            if (b.type() == BoostType.PERCENT) {
                PlayerCombatCache.multiplyField(stats, b.statKey(), b.value());
            }
        }
    }

    private static String normalize(String statKey) {
        return statKey == null ? "" : statKey.trim().toLowerCase();
    }
}