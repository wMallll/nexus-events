package com.nexusevents.lobby;

import com.nexusevents.arena.Region;
import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.configuration.ConfigurationFile;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Zonas de lobby por ARENA y por EVENTO, con alcance {@code general}
 * como fallback (el mismo patron que los puntos y la altura minima del
 * plugin). Cada zona es una region con las protecciones del lobby
 * global, persistida en {@code lobbyzones.yml}.
 *
 * <p>Resolucion en tiempo de juego: si una posicion cae dentro de una
 * zona especifica de un evento, mandan sus protecciones; si no, la
 * zona {@code general} de la arena. Las protecciones de jugador nunca
 * actuan sobre participantes de eventos.</p>
 */
public final class LobbyZoneService implements Manager, Reloadable {

    public static final String FILE = "lobbyzones.yml";
    public static final String GENERAL_SCOPE = "general";
    private static final String ROOT = "zones";

    /** Zona de lobby: region + protecciones. */
    public static final class LobbyZone {

        private final Region region;
        private final LobbyProtectionSettings settings;

        LobbyZone(Region region, LobbyProtectionSettings settings) {
            this.region = region;
            this.settings = settings;
        }

        public Region getRegion() {
            return region;
        }

        public LobbyProtectionSettings getSettings() {
            return settings;
        }
    }

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, Map<String, LobbyZone>> zonesByArena = new LinkedHashMap<>();

    private Predicate<UUID> participantCheck = playerId -> false;

    public LobbyZoneService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Define el filtro de participantes de eventos (exentos de las
     * protecciones de jugador de las zonas).
     *
     * @param participantCheck predicado por UUID.
     */
    public void setParticipantCheck(Predicate<UUID> participantCheck) {
        this.participantCheck = participantCheck;
    }

    @Override
    public String getName() {
        return "Zonas de lobby";
    }

    @Override
    public void enable() {
        configManager.register(FILE);
        load();
    }

    @Override
    public void disable() {
        zonesByArena.clear();
    }

    @Override
    public void reload() {
        load();
    }

    private void load() {
        zonesByArena.clear();
        ConfigurationSection root = configManager.getFile(FILE).get().getConfigurationSection(ROOT);
        if (root == null) {
            return;
        }
        boolean migrated = false;
        for (String arena : root.getKeys(false)) {
            ConfigurationSection arenaSection = root.getConfigurationSection(arena);
            if (arenaSection == null) {
                continue;
            }
            // Formato anterior (zona unica por arena): migra a "general".
            if (arenaSection.isString("region")) {
                loadZone(arena, GENERAL_SCOPE, arenaSection);
                migrated = true;
                continue;
            }
            for (String scope : arenaSection.getKeys(false)) {
                ConfigurationSection zoneSection = arenaSection.getConfigurationSection(scope);
                if (zoneSection != null) {
                    loadZone(arena, scope, zoneSection);
                }
            }
        }
        if (migrated) {
            saveAll();
            plugin.getLogger().info("lobbyzones.yml: zonas del formato anterior migradas al alcance 'general'.");
        }
    }

    private void loadZone(String arena, String scope, ConfigurationSection section) {
        String raw = section.getString("region", "");
        if (raw == null || raw.isEmpty()) {
            return;
        }
        try {
            Region region = Region.deserialize(raw);
            LobbyProtectionSettings settings = new LobbyProtectionSettings();
            settings.load(section.getConfigurationSection("protection"));
            zonesFor(arena).put(scope.toLowerCase(Locale.ROOT), new LobbyZone(region, settings));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("lobbyzones.yml: zona invalida '" + arena + "." + scope
                    + "': se ignora.");
        }
    }

    private Map<String, LobbyZone> zonesFor(String arena) {
        return zonesByArena.computeIfAbsent(arena.toLowerCase(Locale.ROOT),
                key -> new LinkedHashMap<>());
    }

    private void saveAll() {
        ConfigurationFile file = configManager.getFile(FILE);
        file.get().set(ROOT, null);
        ConfigurationSection root = file.get().createSection(ROOT);
        for (Map.Entry<String, Map<String, LobbyZone>> arenaEntry : zonesByArena.entrySet()) {
            for (Map.Entry<String, LobbyZone> zoneEntry : arenaEntry.getValue().entrySet()) {
                ConfigurationSection section = root.createSection(
                        arenaEntry.getKey() + "." + zoneEntry.getKey());
                section.set("region", zoneEntry.getValue().getRegion().serialize());
                zoneEntry.getValue().getSettings().save(section);
            }
        }
        file.save();
    }

    // ------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------

    /**
     * Define (o reemplaza) la zona del alcance dado, conservando las
     * protecciones existentes si ya tenia.
     *
     * @param arenaName nombre de la arena.
     * @param scope     id de evento o {@code general}.
     * @param region    region marcada.
     */
    public void setRegion(String arenaName, String scope, Region region) {
        Map<String, LobbyZone> zones = zonesFor(arenaName);
        String key = scope.toLowerCase(Locale.ROOT);
        LobbyZone previous = zones.get(key);
        LobbyProtectionSettings settings = previous != null
                ? previous.getSettings() : new LobbyProtectionSettings();
        zones.put(key, new LobbyZone(region, settings));
        saveAll();
    }

    /**
     * Elimina la zona del alcance dado.
     *
     * @param arenaName nombre de la arena.
     * @param scope     id de evento o {@code general}.
     * @return true si existia.
     */
    public boolean removeZone(String arenaName, String scope) {
        Map<String, LobbyZone> zones = zonesByArena.get(arenaName.toLowerCase(Locale.ROOT));
        if (zones == null || zones.remove(scope.toLowerCase(Locale.ROOT)) == null) {
            return false;
        }
        if (zones.isEmpty()) {
            zonesByArena.remove(arenaName.toLowerCase(Locale.ROOT));
        }
        saveAll();
        return true;
    }

    /**
     * Zona del alcance dado, si esta definida.
     *
     * @param arenaName nombre de la arena.
     * @param scope     id de evento o {@code general}.
     * @return zona configurada.
     */
    public Optional<LobbyZone> getZone(String arenaName, String scope) {
        Map<String, LobbyZone> zones = zonesByArena.get(arenaName.toLowerCase(Locale.ROOT));
        if (zones == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(zones.get(scope.toLowerCase(Locale.ROOT)));
    }

    /**
     * Actualiza una proteccion de la zona del alcance dado y la
     * persiste.
     *
     * @param arenaName nombre de la arena.
     * @param scope     id de evento o {@code general}.
     * @param key       proteccion.
     * @param value     "true" o "false".
     * @return true si se actualizo.
     */
    public boolean setProtection(String arenaName, String scope, String key, String value) {
        LobbyZone zone = getZone(arenaName, scope).orElse(null);
        if (zone == null || !zone.getSettings().applySetting(key, value)) {
            return false;
        }
        saveAll();
        return true;
    }

    /**
     * Zona que contiene la posicion: primero las especificas de
     * eventos, luego las generales.
     *
     * @param location posicion a evaluar.
     * @return zona contenedora con mayor prioridad.
     */
    public Optional<LobbyZone> zoneAt(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        LobbyZone general = null;
        for (Map<String, LobbyZone> zones : zonesByArena.values()) {
            for (Map.Entry<String, LobbyZone> entry : zones.entrySet()) {
                if (!contains(entry.getValue().getRegion(), location)) {
                    continue;
                }
                if (!GENERAL_SCOPE.equals(entry.getKey())) {
                    return Optional.of(entry.getValue());
                }
                if (general == null) {
                    general = entry.getValue();
                }
            }
        }
        return Optional.ofNullable(general);
    }

    private boolean contains(Region region, Location location) {
        if (region.getWorld() == null || !region.getWorld().equals(location.getWorld())) {
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
     * Indica si el jugador esta participando en un evento.
     *
     * @param playerId identificador del jugador.
     * @return true si participa.
     */
    public boolean isParticipant(UUID playerId) {
        return participantCheck.test(playerId);
    }
}
