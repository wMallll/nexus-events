package com.nexusevents.lobby;

import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.configuration.ConfigurationFile;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Lobby global del servidor, persistido en {@code lobby.yml}.
 *
 * <p>Los jugadores que entran por <b>primera vez</b> al servidor
 * aparecen en este punto (si esta configurado con
 * {@code /evento setmainlobby}). Las reconexiones mantienen el
 * comportamiento vanilla: el jugador aparece donde se desconecto.</p>
 */
public final class MainLobbyService implements Manager, Reloadable {

    public static final String FILE = "lobby.yml";
    private static final String PATH = "main-lobby";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private ArenaLocation lobby;

    public MainLobbyService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "Lobby global";
    }

    @Override
    public void enable() {
        configManager.register(FILE);
        loadLobby();
    }

    @Override
    public void disable() {
        lobby = null;
    }

    @Override
    public void reload() {
        loadLobby();
    }

    private void loadLobby() {
        String raw = configManager.getFile(FILE).get().getString(PATH, "");
        if (raw == null || raw.isEmpty()) {
            lobby = null;
            return;
        }
        try {
            lobby = ArenaLocation.deserialize(raw);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("lobby.yml contiene un lobby global invalido: se ignora.");
            lobby = null;
        }
    }

    /**
     * Define y persiste el lobby global.
     *
     * @param location posicion del lobby.
     */
    public void setLobby(Location location) {
        this.lobby = ArenaLocation.from(location);
        ConfigurationFile file = configManager.getFile(FILE);
        file.get().set(PATH, lobby.serialize());
        file.save();
    }

    /**
     * Devuelve el lobby global resuelto, si esta configurado y su
     * mundo esta cargado.
     *
     * @return posicion del lobby global.
     */
    public Optional<Location> getLobby() {
        if (lobby == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lobby.toBukkit());
    }
}
