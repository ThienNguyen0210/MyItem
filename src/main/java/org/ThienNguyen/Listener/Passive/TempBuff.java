package org.ThienNguyen.Listener.Passive;

/**
 * 1 buff tạm thời lên 1 stat, có hạn dùng.
 * Lưu trong PlayerCombatCache.CombatStats.tempBuffs.
 *
 * LƯU Ý VỀ HIỆU LỰC: hiện tại BuffStatMechanic chỉ LƯU TempBuff vào cache, nhưng
 * EventDamage.onDamage() CHƯA đọc tempBuffs khi tính damage (attackerStats.totalCritDamage...
 * vẫn đọc trực tiếp field gốc, không cộng thêm buff tạm). Nếu cần buff thực sự ảnh hưởng
 * combat, cần sửa thêm EventDamage để cộng tempBuffs vào stat tương ứng trước khi dùng —
 * báo lại nếu cần, đây là việc còn để mở theo đúng phạm vi đã chốt ban đầu.
 */
public class TempBuff {

    public final String statKey;   // khớp tên field dùng trong yml (vd "critical_damage")
    public final double amount;    // giá trị cộng thêm (flat)
    public final long expireAtMillis;

    public TempBuff(String statKey, double amount, int durationSeconds) {
        this.statKey = statKey;
        this.amount = amount;
        this.expireAtMillis = System.currentTimeMillis() + (durationSeconds * 1000L);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expireAtMillis;
    }
}