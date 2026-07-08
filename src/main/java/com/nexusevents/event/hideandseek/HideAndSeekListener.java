package com.nexusevents.event.hideandseek;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener con alcance de sesion para "Escondete si puedes".
 *
 * <p>Se registra al comenzar la partida y se desregistra al terminar.
 * Deriva a la sesion los golpes cuerpo a cuerpo entre jugadores para
 * resolver eliminaciones e impedir que los eliminados afecten el
 * evento.</p>
 */
final class HideAndSeekListener implements Listener {

    private final HideAndSeekSession session;

    HideAndSeekListener(HideAndSeekSession session) {
        this.session = session;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        session.handleHit((Player) event.getDamager(), (Player) event.getEntity(), event);
    }
}
