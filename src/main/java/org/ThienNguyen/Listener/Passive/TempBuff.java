package org.ThienNguyen.Listener.Passive;


public class TempBuff {

    public final String statKey;   
    public final double amount;    
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