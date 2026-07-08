package com.nexusevents.event;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import com.nexusevents.message.MessageService;
import com.nexusevents.message.TitleService;
import com.nexusevents.scheduler.TaskScheduler;
import com.nexusevents.scoreboard.ScoreboardTemplateRegistry;
import com.nexusevents.sound.SoundService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Nucleo del sistema de eventos.
 *
 * <p>Mantiene el registro de tipos de evento disponibles y las sesiones
 * activas (una por arena, con soporte de varias arenas en simultaneo).
 * Valida que la arena cumpla los requisitos del evento antes de iniciar
 * e informa exactamente que falta configurar.</p>
 */
public final class EventManager implements Manager, Reloadable {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final EventContext context;

    private final Map<String, GameEvent> types = new LinkedHashMap<>();
    private final Map<String, EventSession> sessions = new LinkedHashMap<>();
    private final Map<UUID, PlayerSnapshot> orphanRestores = new HashMap<>();

    private EventSettings settings;

    public EventManager(JavaPlugin plugin, ConfigManager configManager, TaskScheduler scheduler,
                        MessageService messages, TitleService titles, SoundService sounds,
                        ScoreboardTemplateRegistry scoreboards, ArenaManager arenas) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.context = new EventContext(plugin, scheduler, messages, titles, sounds,
                scoreboards, arenas, this);
    }

    @Override
    public String getName() {
        return "Eventos";
    }

    @Override
    public void enable() {
        this.settings = EventSettings.parse(configManager.getConfig(), plugin.getLogger());
    }

    @Override
    public void disable() {
        stopAll();
        types.clear();
    }

    @Override
    public void reload() {
        this.settings = EventSettings.parse(configManager.getConfig(), plugin.getLogger());
    }

    // ------------------------------------------------------------------
    // Registro de tipos
    // ------------------------------------------------------------------

    /**
     * Registra un tipo de evento disponible para iniciarse.
     *
     * @param event tipo de evento.
     */
    public void register(GameEvent event) {
        types.put(event.getId().toLowerCase(Locale.ROOT), event);
    }

    public Optional<GameEvent> getType(String id) {
        return Optional.ofNullable(types.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<GameEvent> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    // ------------------------------------------------------------------
    // Inicio y detencion
    // ------------------------------------------------------------------

    /**
     * Intenta iniciar un evento sobre una arena, validando existencia,
     * disponibilidad y completitud de la arena.
     *
     * @param eventId   id del tipo de evento.
     * @param arenaName nombre de la arena.
     * @return resultado del intento, con detalle si la arena esta incompleta.
     */
    public StartResult start(String eventId, String arenaName) {
        GameEvent type = types.get(eventId.toLowerCase(Locale.ROOT));
        if (type == null) {
            return StartResult.of(StartResult.Status.EVENT_NOT_FOUND);
        }
        Arena arena = context.getArenas().get(arenaName).orElse(null);
        if (arena == null) {
            return StartResult.of(StartResult.Status.ARENA_NOT_FOUND);
        }
        if (sessions.containsKey(keyOf(arena.getName()))) {
            return StartResult.of(StartResult.Status.ARENA_IN_USE);
        }
        String missing = describeMissing(type, arena);
        if (!missing.isEmpty()) {
            return StartResult.incomplete(missing);
        }
        EventSession session = type.createSession(context, arena);
        sessions.put(keyOf(arena.getName()), session);
        session.open();
        return StartResult.of(StartResult.Status.SUCCESS);
    }

    private String describeMissing(GameEvent type, Arena arena) {
        List<String> missing = new ArrayList<>();

        Set<String> requiredPoints = new LinkedHashSet<>();
        requiredPoints.add(ArenaKeys.SPAWN);
        requiredPoints.add(ArenaKeys.LOBBY);
        requiredPoints.addAll(type.getRequiredPoints());

        for (String key : requiredPoints) {
            ArenaLocation point = arena.getPoint(key).orElse(null);
            if (point == null) {
                missing.add(displayPoint(key));
            } else if (point.toBukkit() == null) {
                missing.add(displayPoint(key) + " (mundo no cargado)");
            }
        }
        for (String key : type.getRequiredRegions()) {
            com.nexusevents.arena.Region region = arena.getRegion(key).orElse(null);
            if (region == null) {
                missing.add(displayRegion(key));
            } else if (region.getWorld() == null) {
                missing.add(displayRegion(key) + " (mundo no cargado)");
            }
        }
        return String.join(", ", missing);
    }

    private String displayPoint(String key) {
        return context.getMessages().rawOr("arena.point-names." + key, key);
    }

    private String displayRegion(String key) {
        return context.getMessages().rawOr("arena.region-names." + key, key);
    }

    /**
     * Detiene el evento activo en una arena.
     *
     * @param arenaName nombre de la arena.
     * @return true si habia un evento y fue detenido.
     */
    public boolean stop(String arenaName) {
        EventSession session = sessions.get(keyOf(arenaName));
        if (session == null) {
            return false;
        }
        session.end(EventEndReason.CANCELLED);
        return true;
    }

    /**
     * Detiene todas las sesiones activas.
     *
     * @return cantidad de sesiones detenidas.
     */
    public int stopAll() {
        List<EventSession> active = new ArrayList<>(sessions.values());
        for (EventSession session : active) {
            session.end(EventEndReason.CANCELLED);
        }
        return active.size();
    }

    /**
     * Notificacion interna de que una sesion finalizo (la invoca la
     * propia sesion al cerrar).
     *
     * @param session sesion finalizada.
     */
    public void sessionEnded(EventSession session) {
        sessions.remove(keyOf(session.getArena().getName()));
    }

    // ------------------------------------------------------------------
    // Jugadores
    // ------------------------------------------------------------------

    /**
     * Une un jugador a un evento activo. Si hay uno solo, no hace falta
     * indicar la arena.
     *
     * @param player    jugador entrante.
     * @param arenaName arena objetivo, o null para autodetectar.
     * @return resultado del intento.
     */
    public JoinResult join(Player player, String arenaName) {
        if (getSessionByPlayer(player.getUniqueId()).isPresent()) {
            return JoinResult.ALREADY_IN;
        }
        EventSession target;
        if (arenaName != null) {
            target = sessions.get(keyOf(arenaName));
            if (target == null) {
                return JoinResult.NOT_FOUND;
            }
        } else {
            if (sessions.isEmpty()) {
                return JoinResult.NONE_ACTIVE;
            }
            if (sessions.size() > 1) {
                return JoinResult.AMBIGUOUS;
            }
            target = sessions.values().iterator().next();
        }
        return target.join(player);
    }

    /**
     * Registra el snapshot de un jugador que quedo offline al finalizar
     * su sesion, para restaurarlo en su proximo ingreso al servidor.
     *
     * @param playerId identificador del jugador.
     * @param snapshot estado a restaurar.
     */
    public void addOrphanRestore(UUID playerId, PlayerSnapshot snapshot) {
        if (snapshot != null) {
            orphanRestores.put(playerId, snapshot);
        }
    }

    /**
     * Procesa el ingreso de un jugador al servidor: primero restaura
     * cualquier estado pendiente de un evento anterior y, si no,
     * intenta reincorporarlo a una sesion con reconexion abierta.
     *
     * @param player jugador que acaba de entrar.
     */
    public void handleJoin(Player player) {
        PlayerSnapshot pending = orphanRestores.remove(player.getUniqueId());
        if (pending != null) {
            pending.restore(player);
            context.getMessages().send(player, "event.restored-late");
            return;
        }
        for (EventSession session : new ArrayList<>(sessions.values())) {
            if (session.tryReconnect(player)) {
                return;
            }
        }
    }

    /**
     * Saca a un jugador del evento en el que participa.
     *
     * @param player jugador saliente.
     * @return true si estaba participando de alguno.
     */
    public boolean leave(Player player) {
        EventSession session = getSessionByPlayer(player.getUniqueId()).orElse(null);
        if (session == null) {
            return false;
        }
        session.leave(player, false);
        return true;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Optional<EventSession> getSession(String arenaName) {
        return Optional.ofNullable(sessions.get(keyOf(arenaName)));
    }

    public Collection<EventSession> getSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    /**
     * Busca la sesion en la que participa un jugador.
     *
     * @param playerId identificador del jugador.
     * @return sesion, si participa de alguna.
     */
    public Optional<EventSession> getSessionByPlayer(UUID playerId) {
        for (EventSession session : sessions.values()) {
            if (session.isParticipant(playerId)) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    public EventSettings getSettings() {
        return settings;
    }

    public EventContext getContext() {
        return context;
    }

    private String keyOf(String arenaName) {
        return arenaName.toLowerCase(Locale.ROOT);
    }
}
