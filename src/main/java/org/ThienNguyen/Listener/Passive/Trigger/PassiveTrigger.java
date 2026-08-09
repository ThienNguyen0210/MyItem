package org.ThienNguyen.Listener.Passive.Trigger;

public enum PassiveTrigger {
    ON_HIT,           // đánh trúng enemy
    ON_KILL,          // hạ gục enemy
    ON_TAKE_DAMAGE,   // bị đánh (mọi nguồn: PvP, mob, fall, fire... miễn có damage)
    HP_THRESHOLD,     // HP victim xuống dưới ngưỡng %
    ON_DEATH,         // actor (chủ passive) chết
    ON_BLOCK_BREAK    // actor phá 1 block (dùng cho cuốc TNT, khoan vùng...)
}