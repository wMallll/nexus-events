package com.nexusevents.listener;

import com.nexusevents.event.EventManager;
import com.nexusevents.event.EventSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.Optional;

/**
 * Protecciones automaticas para participantes de eventos.
 *
 * <p>Cancela dano y hambre cuando la sesion no lo permite (por defecto,
 * fuera del estado RUNNING) e impide romper o colocar bloques durante
 * el evento. Las reglas concretas las decide cada sesion mediante sus
 * hooks {@code allowDamage}, {@code allowHunger} y {@code allowBuilding}.</p>
 */
public final class EventProtectionListener implements Listener {

    private final EventManager eventManager;

    public EventProtectionListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        sessionOf(player).ifPresent(session -> {
            if (!session.allowDamage(player, event)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        sessionOf(player).ifPresent(session -> {
            if (!session.allowHunger(player)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        sessionOf(event.getPlayer()).ifPresent(session -> {
            if (!session.allowBuilding(event.getPlayer())) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        sessionOf(event.getPlayer()).ifPresent(session -> {
            if (!session.allowBuilding(event.getPlayer())) {
                event.setCancelled(true);
            }
        });
    }

    private Optional<EventSession> sessionOf(Player player) {
        return eventManager.getSessionByPlayer(player.getUniqueId());
    }
}
