package com.nexusevents.lobby;

import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.configuration.ConfigurationFile;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import com.nexusevents.message.MessageService;
import com.nexusevents.scheduler.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

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
    private static final String MIN_Y_PATH = "main-lobby-min-y";
    private static final long SAFETY_PERIOD_TICKS = 20L;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final TaskScheduler scheduler;
    private final MessageService messages;

    private ArenaLocation lobby;
    private Integer minY;
    private Predicate<UUID> participantCheck = playerId -> false;
    private BukkitTask safetyTask;

    public MainLobbyService(JavaPlugin plugin, ConfigManager configManager,
                            TaskScheduler scheduler, MessageService messages) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.messages = messages;
    }

    /**
     * Define el filtro de participantes de eventos: los jugadores en
     * una sesion quedan a cargo de la red de seguridad de su propio
     * evento, no de la del lobby global.
     *
     * @param participantCheck predicado por UUID.
     */
    public void setParticipantCheck(Predicate<UUID> participantCheck) {
        this.participantCheck = participantCheck;
    }

    @Override
    public String getName() {
        return "Lobby global";
    }

    @Override
    public void enable() {
        configManager.register(FILE);
        loadLobby();
        this.safetyTask = scheduler.syncTimer(this::enforceSafety, SAFETY_PERIOD_TICKS, SAFETY_PERIOD_TICKS);
    }

    @Override
    public void disable() {
        if (safetyTask != null) {
            safetyTask.cancel();
            safetyTask = null;
        }
        lobby = null;
        minY = null;
    }

    /**
     * Red de seguridad del lobby global: los jugadores que NO estan en
     * un evento y caen debajo de la altura minima (en el mundo del
     * lobby) vuelven al lobby.
     */
    private void enforceSafety() {
        if (lobby == null || minY == null) {
            return;
        }
        Location destination = lobby.toBukkit();
        World world = destination != null ? destination.getWorld() : null;
        if (world == null) {
            return;
        }
        List<Player> falling = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!participantCheck.test(player.getUniqueId())
                    && world.equals(player.getWorld())
                    && player.getLocation().getY() < minY) {
                falling.add(player);
            }
        }
        for (Player player : falling) {
            player.teleport(destination);
            messages.send(player, "event.safety-teleport");
        }
    }

    /**
     * Fija y persiste la altura minima del lobby global.
     *
     * @param minY altura minima (Y en bloques).
     */
    public void setMinY(int minY) {
        this.minY = minY;
        ConfigurationFile file = configManager.getFile(FILE);
        file.get().set(MIN_Y_PATH, minY);
        file.save();
    }

    @Override
    public void reload() {
        loadLobby();
    }

    private void loadLobby() {
        String raw = configManager.getFile(FILE).get().getString(PATH, "");
        if (raw == null || raw.isEmpty()) {
            lobby = null;
        } else {
            try {
                lobby = ArenaLocation.deserialize(raw);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("lobby.yml contiene un lobby global invalido: se ignora.");
                lobby = null;
            }
        }
        this.minY = configManager.getFile(FILE).get().contains(MIN_Y_PATH)
                ? configManager.getFile(FILE).get().getInt(MIN_Y_PATH)
                : null;
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
