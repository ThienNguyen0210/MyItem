package org.ThienNguyen.Listener.Passive;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;


public class PassiveContext {

    private final Player actor;
    private final LivingEntity victim;
    private final double damage;
    private final EntityDamageByEntityEvent event;
    private final Block brokenBlock;

    private final Location actorLocation;
    private final Location victimLocation;

    public PassiveContext(Player actor, LivingEntity victim, double damage, EntityDamageByEntityEvent event) {
        this(actor, victim, damage, event, null);
    }

    public PassiveContext(Player actor, LivingEntity victim, double damage,
                          EntityDamageByEntityEvent event, Block brokenBlock) {
        this.actor       = actor;
        this.victim      = victim;
        this.damage      = damage;
        this.event       = event;
        this.brokenBlock = brokenBlock;
        this.actorLocation  = (actor  != null) ? actor.getLocation().clone()  : null;
        this.victimLocation = (victim != null) ? victim.getLocation().clone() : null;
    }

    public Player        getActor()       { return actor; }
    public LivingEntity  getVictim()      { return victim; }
    public double        getDamage()      { return damage; }
    public EntityDamageByEntityEvent getEvent() { return event; }
    public Block         getBrokenBlock() { return brokenBlock; }

    
    public Location getActorLocation()  { return actorLocation; }

    
    public Location getVictimLocation() { return victimLocation; }
}