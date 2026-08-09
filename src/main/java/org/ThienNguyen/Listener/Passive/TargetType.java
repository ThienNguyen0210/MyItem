package org.ThienNguyen.Listener.Passive;

/**
 * Bộ lọc loại victim mà passive áp dụng — dùng trong condition.target-type của <id>.yml.
 *
 * LƯU Ý: nếu bạn đã có file TargetType.java khác trong project, GIỮ NGUYÊN bản của bạn —
 * file này chỉ là suy luận lại từ cách PassiveDef.java gọi TargetType.PLAYER/MOB/BOTH,
 * vì bản gốc chưa được paste vào cuộc trò chuyện.
 */
public enum TargetType {
    PLAYER, // chỉ áp dụng khi victim là Player (PvP)
    MOB,    // chỉ áp dụng khi victim là mob/quái (không phải Player)
    SELF,   // chỉ áp dụng khi victim CHÍNH LÀ actor (tự gây damage lên bản thân, vd fall/fire tự gây)
    BOTH    // áp dụng cho cả PLAYER và MOB (mặc định) — không bao gồm điều kiện SELF
}