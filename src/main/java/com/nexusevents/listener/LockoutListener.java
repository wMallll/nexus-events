package com.nexusevents.listener;

import com.nexusevents.lockout.LockoutService;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Aplica los efectos del modo torneo sobre los jugadores bloqueados:
 * no pueden ejecutar ningun comando y, si salen del servidor, no pueden
 * volver a entrar. Los administradores con el permiso de bypass quedan
 * exentos. Sin efecto con el modo desactivado.
 */
public final class LockoutListener implements Listener {

    private final LockoutService lockouts;
    private final MessageService messages;

    public LockoutListener(LockoutService lockouts, MessageService messages) {
        this.lockouts = lockouts;
        this.messages = messages;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!lockouts.isEnabled() || !lockouts.isLocked(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getPlayer().hasPermission(Permissions.LOCKOUT_BYPASS)) {
            return;
        }
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                messages.legacy(messages.raw("lockout.kick")));
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!lockouts.isEnabled() || !lockouts.isLocked(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getPlayer().hasPermission(Permissions.LOCKOUT_BYPASS)) {
            return;
        }
        event.setCancelled(true);
        messages.send(event.getPlayer(), "lockout.commands-blocked");
    }
}
