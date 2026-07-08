package com.nexusevents.arena;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sesiones de setup por administrador.
 *
 * <p>Guarda que arena esta editando cada admin (fijada con
 * {@code /evento select}) y las esquinas de region marcadas a medias,
 * para que los comandos de setup no necesiten repetir el nombre de la
 * arena en cada uso.</p>
 */
public final class SetupSessionService {

    private final Map<UUID, String> selectedArenas = new HashMap<>();
    private final Map<UUID, Map<String, ArenaLocation>> pendingCorners = new HashMap<>();

    /**
     * Fija la arena en edicion para un administrador.
     *
     * @param playerId  identificador del admin.
     * @param arenaName nombre de la arena.
     */
    public void select(UUID playerId, String arenaName) {
        selectedArenas.put(playerId, arenaName);
    }

    /**
     * Obtiene el nombre de la arena en edicion de un administrador.
     *
     * @param playerId identificador del admin.
     * @return nombre de la arena seleccionada, si hay alguna.
     */
    public Optional<String> getSelected(UUID playerId) {
        return Optional.ofNullable(selectedArenas.get(playerId));
    }

    /**
     * Resuelve directamente la arena en edicion de un jugador.
     *
     * @param player       administrador.
     * @param arenaManager manager de arenas.
     * @return arena seleccionada, si existe y sigue registrada.
     */
    public Optional<Arena> resolveSelected(Player player, ArenaManager arenaManager) {
        return getSelected(player.getUniqueId()).flatMap(arenaManager::get);
    }

    /**
     * Limpia por completo la sesion de un administrador.
     *
     * @param playerId identificador del admin.
     */
    public void clearSession(UUID playerId) {
        selectedArenas.remove(playerId);
        pendingCorners.remove(playerId);
    }

    /**
     * Elimina de todas las sesiones las referencias a una arena
     * (se invoca cuando la arena se borra).
     *
     * @param arenaName nombre de la arena eliminada.
     */
    public void clearForArena(String arenaName) {
        Iterator<Map.Entry<UUID, String>> iterator = selectedArenas.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().equalsIgnoreCase(arenaName)) {
                iterator.remove();
            }
        }
    }

    /**
     * Guarda una esquina de region marcada por el administrador.
     *
     * @param playerId  identificador del admin.
     * @param regionKey clave de la region en edicion.
     * @param corner    numero de esquina (1 o 2).
     * @param location  posicion marcada.
     */
    public void setPendingCorner(UUID playerId, String regionKey, int corner, ArenaLocation location) {
        pendingCorners.computeIfAbsent(playerId, id -> new HashMap<>())
                .put(cornerKey(regionKey, corner), location);
    }

    /**
     * Obtiene una esquina previamente marcada.
     *
     * @param playerId  identificador del admin.
     * @param regionKey clave de la region.
     * @param corner    numero de esquina (1 o 2).
     * @return esquina marcada, si existe.
     */
    public Optional<ArenaLocation> getPendingCorner(UUID playerId, String regionKey, int corner) {
        Map<String, ArenaLocation> corners = pendingCorners.get(playerId);
        if (corners == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(corners.get(cornerKey(regionKey, corner)));
    }

    /**
     * Limpia las esquinas pendientes de una region tras completarla.
     *
     * @param playerId  identificador del admin.
     * @param regionKey clave de la region.
     */
    public void clearPendingCorners(UUID playerId, String regionKey) {
        Map<String, ArenaLocation> corners = pendingCorners.get(playerId);
        if (corners != null) {
            corners.remove(cornerKey(regionKey, 1));
            corners.remove(cornerKey(regionKey, 2));
        }
    }

    private String cornerKey(String regionKey, int corner) {
        return regionKey + ":" + corner;
    }
}
