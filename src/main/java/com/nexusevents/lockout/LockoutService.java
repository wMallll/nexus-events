package com.nexusevents.lockout;

import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.configuration.ConfigurationFile;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modo torneo: registro persistente de jugadores eliminados.
 *
 * <p>Con el bloqueo activado ({@code /evento lockout on}), cada jugador
 * eliminado en un evento queda registrado en {@code lockouts.yml} y, aun
 * despues de finalizado el evento, no puede unirse a otros, no puede
 * ejecutar comandos y, si sale del servidor, no puede volver a entrar,
 * hasta que un administrador limpie la lista. Desactivado (por defecto,
 * ideal para pruebas), no tiene ningun efecto.</p>
 */
public final class LockoutService implements Manager, Reloadable {

    public static final String FILE = "lockouts.yml";
    private static final String ENABLED_PATH = "enabled";
    private static final String PLAYERS_PATH = "players";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private boolean enabled;
    private final Map<UUID, String> locked = new LinkedHashMap<>();

    public LockoutService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "Modo torneo";
    }

    @Override
    public void enable() {
        configManager.register(FILE);
        load();
    }

    @Override
    public void disable() {
        locked.clear();
    }

    @Override
    public void reload() {
        load();
    }

    private void load() {
        locked.clear();
        ConfigurationFile file = configManager.getFile(FILE);
        this.enabled = file.get().getBoolean(ENABLED_PATH, false);
        for (String entry : file.get().getStringList(PLAYERS_PATH)) {
            String[] parts = entry.split(";", 2);
            try {
                locked.put(UUID.fromString(parts[0].trim()), parts.length > 1 ? parts[1] : "?");
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("lockouts.yml contiene una entrada invalida: '" + entry + "'.");
            }
        }
    }

    private void save() {
        ConfigurationFile file = configManager.getFile(FILE);
        file.get().set(ENABLED_PATH, enabled);
        List<String> entries = new ArrayList<>(locked.size());
        for (Map.Entry<UUID, String> entry : locked.entrySet()) {
            entries.add(entry.getKey() + ";" + entry.getValue());
        }
        file.get().set(PLAYERS_PATH, entries);
        file.save();
    }

    /**
     * Activa o desactiva el modo torneo (persistido).
     *
     * @param enabled true para activar el bloqueo de eliminados.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Registra a un jugador eliminado (solo con el modo activado).
     *
     * @param player jugador eliminado.
     */
    public void lock(Player player) {
        locked.put(player.getUniqueId(), player.getName());
        save();
    }

    /**
     * Indica si el jugador esta bloqueado por eliminacion.
     *
     * @param playerId identificador del jugador.
     * @return true si figura en el registro.
     */
    public boolean isLocked(UUID playerId) {
        return locked.containsKey(playerId);
    }

    /**
     * Limpia el registro completo (nuevo evento oficial).
     *
     * @return cantidad de jugadores liberados.
     */
    public int clear() {
        int count = locked.size();
        locked.clear();
        save();
        return count;
    }

    public int getLockedCount() {
        return locked.size();
    }
}
