package org.ThienNguyen.Listener.Passive;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Context truyền vào PassiveMechanic.execute() mỗi lần trigger.
 */
public class PassiveContext {

    private final Player actor;          // người kích hoạt passive (chủ item)
    private final LivingEntity victim;   // mục tiêu (có thể null nếu ON_KILL sau khi chết,
    // hoặc chết do môi trường, hoặc ON_DEATH không có killer)
    private final double damage;         // damage tại thời điểm trigger
    private final EntityDamageByEntityEvent event; // null nếu trigger không phải từ combat damage

    /** Block vừa bị actor phá — CHỈ có giá trị khi trigger = ON_BLOCK_BREAK, null mọi trường
     *  hợp khác. Dùng cho mechanic BREAK_AREA (đào N×N quanh block này). */
    private final Block brokenBlock;

    public PassiveContext(Player actor, LivingEntity victim, double damage, EntityDamageByEntityEvent event) {
        this(actor, victim, damage, event, null);
    }

    public PassiveContext(Player actor, LivingEntity victim, double damage,
                          EntityDamageByEntityEvent event, Block brokenBlock) {
        this.actor       = actor;
        this.victim      = victim;
        this.damage       = damage;
        this.event        = event;
        this.brokenBlock  = brokenBlock;
    }

    public Player getActor()  { return actor; }
    public LivingEntity getVictim() { return victim; }
    public double getDamage() { return damage; }
    public EntityDamageByEntityEvent getEvent() { return event; }
    public Block getBrokenBlock() { return brokenBlock; }
}