package com.nexusevents.listener;

import com.nexusevents.event.EventManager;
import com.nexusevents.lobby.MainLobbyService;
import com.nexusevents.scheduler.TaskScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Clasifica y deriva las conexiones de jugadores a sus sesiones.
 *
 * <p>Distingue desconexiones <b>involuntarias</b> de <b>voluntarias</b>
 * con la unica senal confiable que expone el servidor: una perdida de
 * conexion, timeout o crash del cliente produce un {@link PlayerKickEvent}
 * (el socket muere sin despedida), mientras que una desconexion iniciada
 * por el jugador envia el paquete de cierre y genera solo el
 * {@link PlayerQuitEvent}. Cada sesion decide que hacer con esa
 * clasificacion (ver el sistema de reconexion de Escondete si puedes).</p>
 */
public final class PlayerConnectionListener implements Listener {

    private final EventManager eventManager;
    private final MainLobbyService mainLobby;
    private final TaskScheduler scheduler;
    private final Set<UUID> kickedRecently = new HashSet<>();

    public PlayerConnectionListener(EventManager eventManager, MainLobbyService mainLobby,
                                    TaskScheduler scheduler) {
        this.eventManager = eventManager;
        this.mainLobby = mainLobby;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        kickedRecently.add(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boolean kicked = kickedRecently.remove(event.getPlayer().getUniqueId());
        eventManager.getSessionByPlayer(event.getPlayer().getUniqueId())
                .ifPresent(session -> session.playerDisconnected(event.getPlayer(), kicked));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            // Primer ingreso al servidor: se lo lleva al lobby global.
            // Las reconexiones mantienen el comportamiento vanilla
            // (aparecer donde se desconecto).
            mainLobby.getLobby().ifPresent(lobby ->
                    scheduler.syncLater(() -> {
                        if (event.getPlayer().isOnline()) {
                            event.getPlayer().teleport(lobby);
                        }
                    }, 1L));
        }
        eventManager.handleJoin(event.getPlayer());
    }
}
