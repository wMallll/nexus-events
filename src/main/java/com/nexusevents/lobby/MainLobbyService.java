package com.nexusevents.lobby;

import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.Region;
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
    private static final String REGION_PATH = "main-lobby-region";
    private static final long SAFETY_PERIOD_TICKS = 20L;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final TaskScheduler scheduler;
    private final MessageService messages;

    private ArenaLocation lobby;
    private Integer minY;
    private Region region;
    private final LobbyProtectionSettings protection = new LobbyProtectionSettings();
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
            sendSafetyMessage(player);
        }
    }

    /**
     * Envia el aviso estandar de la red de seguridad.
     *
     * @param player jugador rescatado.
     */
    public void sendSafetyMessage(Player player) {
        messages.send(player, "event.safety-teleport");
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
        String rawRegion = configManager.getFile(FILE).get().getString(REGION_PATH, "");
        if (rawRegion == null || rawRegion.isEmpty()) {
            region = null;
        } else {
            try {
                region = Region.deserialize(rawRegion);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("lobby.yml contiene una region global invalida: se ignora.");
                region = null;
            }
        }
        protection.load(configManager.getFile(FILE).get().getConfigurationSection("protection"));
    }

    /**
     * Define y persiste la region protegida del lobby global.
     *
     * @param region region marcada con pos1/pos2.
     */
    public void setRegion(Region region) {
        this.region = region;
        ConfigurationFile file = configManager.getFile(FILE);
        file.get().set(REGION_PATH, region.serialize());
        file.save();
    }

    /**
     * Elimina la region protegida (las protecciones dejan de actuar).
     */
    public void removeRegion() {
        this.region = null;
        ConfigurationFile file = configManager.getFile(FILE);
        file.get().set(REGION_PATH, null);
        file.save();
    }

    /**
     * Region protegida del lobby global, si esta definida.
     *
     * @return region configurada.
     */
    public Optional<Region> getRegion() {
        return Optional.ofNullable(region);
    }

    /**
     * Altura minima del lobby global, si esta configurada.
     *
     * @return altura minima.
     */
    public Optional<Integer> getMinY() {
        return Optional.ofNullable(minY);
    }

    /**
     * Protecciones activas de la region del lobby.
     *
     * @return configuracion de protecciones.
     */
    public LobbyProtectionSettings getProtection() {
        return protection;
    }

    /**
     * Actualiza una proteccion, la aplica de inmediato y la persiste.
     *
     * @param key   proteccion.
     * @param value "true" o "false".
     * @return true si se actualizo.
     */
    public boolean setProtection(String key, String value) {
        if (!protection.applySetting(key, value)) {
            return false;
        }
        ConfigurationFile file = configManager.getFile(FILE);
        protection.save(file.get());
        file.save();
        return true;
    }

    /**
     * Indica si la posicion esta dentro de la region protegida.
     *
     * @param location posicion a evaluar.
     * @return true si hay region y la contiene.
     */
    public boolean isInsideRegion(Location location) {
        if (region == null || location.getWorld() == null
                || region.getWorld() == null
                || !region.getWorld().equals(location.getWorld())) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= region.getMinX() && x <= region.getMaxX()
                && y >= region.getMinY() && y <= region.getMaxY()
                && z >= region.getMinZ() && z <= region.getMaxZ();
    }

    /**
     * Indica si un jugador debe recibir las protecciones del lobby:
     * dentro de la region y sin estar participando en un evento (los
     * participantes quedan a cargo de las reglas de su evento).
     *
     * @param player jugador a evaluar.
     * @return true si las protecciones aplican.
     */
    public boolean shouldProtect(Player player) {
        return !participantCheck.test(player.getUniqueId()) && isInsideRegion(player.getLocation());
    }

    /**
     * Indica si el jugador esta participando en un evento (en cuyo
     * caso las redes del lobby global no intervienen).
     *
     * @param playerId identificador del jugador.
     * @return true si participa de una sesion activa.
     */
    public boolean isParticipant(java.util.UUID playerId) {
        return participantCheck.test(playerId);
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
