package org.ThienNguyen.Listener.Passive;

import java.util.UUID;


public interface PlayerAware {
    void onPlayerQuit(UUID playerId);
}