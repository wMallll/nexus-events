package com.nexusevents.event;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.arena.ArenaLocation;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Partida en curso de un evento (patron template method).
 *
 * <p>Esta clase base resuelve todo lo comun a cualquier evento: lobby
 * con timeout, cuenta regresiva con titulos y sonidos (que retrocede a
 * espera si faltan jugadores), entradas y salidas con snapshot del
 * jugador, eliminaciones, condicion de victoria, anuncios y restauracion
 * total al finalizar. Los eventos concretos implementan unicamente los
 * hooks {@link #onStart()}, {@link #onSecond(int, int)},
 * {@link #onEnd(EventEndReason)} y {@link #onPlayerEliminated(Player)}.</p>
 */
public abstract class EventSession {

    protected final EventContext context;
    private final GameEvent type;
    private final Arena arena;

    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Set<UUID> eliminated = new LinkedHashSet<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();

    private EventState state = EventState.WAITING;
    private BukkitTask ticker;
    private int lobbySecondsLeft;
    private int countdownSecondsLeft;
    private int elapsedSeconds;

    protected EventSession(EventContext context, GameEvent type, Arena arena) {
        this.context = context;
        this.type = type;
        this.arena = arena;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    /**
     * Abre la sesion: anuncia el evento globalmente y comienza la fase
     * de lobby con su unico temporizador de 1 segundo.
     */
    public final void open() {
        this.lobbySecondsLeft = settings().getLobbyTimeoutSeconds();
        this.ticker = context.getScheduler().syncTimer(this::tick, 20L, 20L);
        context.getMessages().broadcast("event.announce-open",
                Placeholder.parsed("event", displayName()),
                Placeholder.unparsed("arena", arena.getName()));
    }

    private void tick() {
        switch (state) {
            case WAITING:
                tickLobby();
                break;
            case COUNTDOWN:
                tickCountdown();
                break;
            case RUNNING:
                tickGame();
                break;
            default:
                break;
        }
    }

    private void tickLobby() {
        if (alive.size() >= settings().getMinPlayers()) {
            startCountdown();
            return;
        }
        lobbySecondsLeft--;
        if (lobbySecondsLeft <= 0) {
            end(EventEndReason.NOT_ENOUGH_PLAYERS);
        }
    }

    private void startCountdown() {
        state = EventState.COUNTDOWN;
        countdownSecondsLeft = settings().getCountdownSeconds();
        broadcast("event.countdown-start",
                Placeholder.unparsed("seconds", String.valueOf(countdownSecondsLeft)));
    }

    private void tickCountdown() {
        if (alive.size() < settings().getMinPlayers()) {
            state = EventState.WAITING;
            lobbySecondsLeft = settings().getLobbyTimeoutSeconds();
            broadcast("event.countdown-aborted");
            return;
        }
        if (countdownSecondsLeft <= 0) {
            begin();
            return;
        }
        if (countdownSecondsLeft <= 5) {
            TagResolver seconds = Placeholder.unparsed("seconds", String.valueOf(countdownSecondsLeft));
            String soundKey = countdownSecondsLeft == 1 ? "countdown-final" : "countdown-tick";
            forEachAliveOnline(player -> {
                context.getTitles().showTitle(player, settings().getCountdownTitle(), seconds);
                context.getSounds().play(player, soundKey);
            });
        }
        countdownSecondsLeft--;
    }

    private void begin() {
        state = EventState.RUNNING;
        elapsedSeconds = 0;
        Location start = resolvePoint(getStartPointKey());
        forEachAliveOnline(player -> {
            if (start != null) {
                player.teleport(start);
            }
            context.getSounds().play(player, "event-start");
        });
        broadcast("event.started");
        onStart();
    }

    private void tickGame() {
        elapsedSeconds++;
        int max = getMaxDurationSeconds();
        onSecond(elapsedSeconds, max > 0 ? max - elapsedSeconds : -1);
        if (max > 0 && elapsedSeconds >= max && state == EventState.RUNNING) {
            end(EventEndReason.TIMEOUT);
        }
    }

    /**
     * Finaliza la sesion: ejecuta el hook de limpieza del evento,
     * anuncia el resultado, restaura a todos los jugadores y se
     * desregistra del manager. Es idempotente.
     *
     * @param reason motivo de finalizacion.
     */
    public final void end(EventEndReason reason) {
        if (state == EventState.ENDING) {
            return;
        }
        state = EventState.ENDING;
        cancelTicker();
        onEnd(reason);
        announceEnd(reason);
        restoreAll();
        context.getEventManager().sessionEnded(this);
    }

    private void announceEnd(EventEndReason reason) {
        switch (reason) {
            case WINNER:
                announceWinner();
                break;
            case TIMEOUT:
                broadcast("event.ended-timeout");
                break;
            case CANCELLED:
                broadcast("event.cancelled");
                break;
            case NOT_ENOUGH_PLAYERS:
                broadcast("event.cancelled-not-enough",
                        Placeholder.unparsed("min", String.valueOf(settings().getMinPlayers())));
                break;
            case NO_PLAYERS:
                broadcast("event.ended-no-winner");
                break;
            default:
                break;
        }
    }

    private void announceWinner() {
        Player winner = firstAliveOnline();
        if (winner == null) {
            broadcast("event.ended-no-winner");
            return;
        }
        context.getMessages().broadcast("event.winner",
                Placeholder.unparsed("winner", winner.getName()),
                Placeholder.parsed("event", displayName()));
        context.getSounds().play(winner, "event-win");
    }

    private void restoreAll() {
        for (UUID id : new ArrayList<>(snapshots.keySet())) {
            Player player = Bukkit.getPlayer(id);
            PlayerSnapshot snapshot = snapshots.remove(id);
            if (snapshot == null) {
                continue;
            }
            if (player != null && player.isOnline()) {
                snapshot.restore(player);
            } else {
                // El jugador esta offline (desconexion involuntaria): su
                // estado se restaurara en su proximo ingreso al servidor.
                context.getEventManager().addOrphanRestore(id, snapshot);
            }
        }
        alive.clear();
        eliminated.clear();
    }

    private void cancelTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    // ------------------------------------------------------------------
    // Jugadores
    // ------------------------------------------------------------------

    /**
     * Intenta unir un jugador a la sesion. Captura su estado completo,
     * lo prepara y lo teletransporta al lobby de la arena.
     *
     * @param player jugador entrante.
     * @return resultado del intento.
     */
    public final JoinResult join(Player player) {
        if (!state.isJoinable()) {
            return JoinResult.ALREADY_STARTED;
        }
        if (alive.size() >= settings().getMaxPlayers()) {
            return JoinResult.FULL;
        }
        snapshots.put(player.getUniqueId(), PlayerSnapshot.capture(player));
        alive.add(player.getUniqueId());
        prepareForLobby(player);
        context.getMessages().send(player, "event.joined",
                Placeholder.parsed("event", displayName()));
        broadcast("event.player-joined",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("count", String.valueOf(alive.size())),
                Placeholder.unparsed("max", String.valueOf(settings().getMaxPlayers())));
        context.getSounds().play(player, "player-join-event");
        return JoinResult.SUCCESS;
    }

    private void prepareForLobby(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setAllowFlight(false);
        player.setFlying(false);
        Location lobby = resolvePoint(ArenaKeys.LOBBY);
        if (lobby != null) {
            player.teleport(lobby);
        }
    }

    /**
     * Saca un jugador de la sesion, restaurando su estado original.
     *
     * @param player       jugador saliente.
     * @param disconnected true si la salida fue por desconexion.
     */
    public final void leave(Player player, boolean disconnected) {
        UUID id = player.getUniqueId();
        boolean wasAlive = alive.remove(id);
        boolean wasEliminated = eliminated.remove(id);
        if (!wasAlive && !wasEliminated) {
            return;
        }
        restore(player);
        broadcast("event.player-left", Placeholder.unparsed("player", player.getName()));
        onPlayerLeft(player, disconnected);
        if (state == EventState.RUNNING) {
            checkVictory();
        }
    }

    private void restore(Player player) {
        PlayerSnapshot snapshot = snapshots.remove(player.getUniqueId());
        if (snapshot != null && player.isOnline()) {
            snapshot.restore(player);
        }
    }

    /**
     * Elimina un jugador de la partida (sigue presente como eliminado).
     *
     * @param player jugador eliminado.
     */
    public final void eliminate(Player player) {
        if (!alive.remove(player.getUniqueId())) {
            return;
        }
        eliminated.add(player.getUniqueId());
        broadcast("event.player-eliminated",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("count", String.valueOf(alive.size())));
        context.getSounds().play(player, "player-eliminated");
        onPlayerEliminated(player);
        if (state == EventState.RUNNING) {
            checkVictory();
        }
    }

    /**
     * Maneja la desconexion de un participante. Por defecto cualquier
     * desconexion equivale a abandonar; los eventos con sistema de
     * reconexion (como Escondete si puedes) lo sobreescriben.
     *
     * @param player jugador desconectado.
     * @param kicked true si la desconexion fue un kick del servidor
     *               (timeout, perdida de conexion), false si fue una
     *               desconexion limpia iniciada por el cliente.
     */
    public void playerDisconnected(Player player, boolean kicked) {
        leave(player, true);
    }

    /**
     * Intenta reincorporar a un jugador que se habia desconectado.
     * La base no soporta reconexion; los eventos que la implementan
     * sobreescriben este metodo.
     *
     * @param player jugador que acaba de entrar al servidor.
     * @return true si el jugador fue reincorporado a esta sesion.
     */
    public boolean tryReconnect(Player player) {
        return false;
    }

    /**
     * Evalua la condicion de victoria estandar: sin vivos termina sin
     * ganador; con un unico vivo termina con ganador si el evento lo
     * permite ({@link #endsWhenOneRemains()}).
     */
    protected final void checkVictory() {
        if (state != EventState.RUNNING) {
            return;
        }
        if (alive.isEmpty()) {
            end(EventEndReason.NO_PLAYERS);
            return;
        }
        if (alive.size() == 1 && endsWhenOneRemains()) {
            end(EventEndReason.WINNER);
        }
    }

    // ------------------------------------------------------------------
    // Hooks del evento concreto
    // ------------------------------------------------------------------

    /**
     * Se invoca cuando el evento pasa a RUNNING, con los jugadores ya
     * teletransportados al punto de inicio.
     */
    protected abstract void onStart();

    /**
     * Se invoca cada segundo mientras el evento esta en RUNNING.
     *
     * @param elapsed   segundos transcurridos.
     * @param remaining segundos restantes, o -1 si no hay limite.
     */
    protected abstract void onSecond(int elapsed, int remaining);

    /**
     * Se invoca al finalizar, antes de restaurar a los jugadores: es el
     * lugar para limpiar bloques, tareas y estado propio del evento.
     *
     * @param reason motivo de finalizacion.
     */
    protected abstract void onEnd(EventEndReason reason);

    /**
     * Se invoca cuando un jugador es eliminado de la partida.
     *
     * @param player jugador eliminado.
     */
    protected abstract void onPlayerEliminated(Player player);

    /**
     * Extension point: se invoca cuando un jugador deja la sesion
     * (voluntariamente o por desconexion definitiva), luego de haber
     * sido restaurado. Los eventos que necesitan reaccionar (por
     * ejemplo, reasignar al cazador) lo sobreescriben.
     *
     * @param player       jugador que salio.
     * @param disconnected true si la salida fue por desconexion.
     */
    protected void onPlayerLeft(Player player, boolean disconnected) {
        // Punto de extension deliberado: la base no necesita reaccionar.
    }

    // ------------------------------------------------------------------
    // Reglas sobreescribibles
    // ------------------------------------------------------------------

    /**
     * Clave del punto de arena al que se teletransporta a los jugadores
     * al comenzar. Por defecto el spawn general.
     *
     * @return clave del punto de inicio.
     */
    protected String getStartPointKey() {
        return ArenaKeys.SPAWN;
    }

    /**
     * Duracion maxima del evento en segundos (0 o negativo = sin limite).
     * Por defecto usa el valor global de configuracion.
     *
     * @return duracion maxima en segundos.
     */
    protected int getMaxDurationSeconds() {
        return settings().getMaxDurationSeconds();
    }

    /**
     * Indica si el evento termina automaticamente cuando queda un unico
     * jugador vivo. Pixel Party, por ejemplo, devuelve false.
     *
     * @return true para terminar con ultimo jugador en pie.
     */
    protected boolean endsWhenOneRemains() {
        return true;
    }

    /**
     * Indica si el jugador puede recibir el dano dado en el estado
     * actual. Por defecto solo durante RUNNING. Los eventos pueden
     * inspeccionar la causa (por ejemplo, El Circulo permite unicamente
     * su propio dano de zona y bloquea PvP y caidas).
     *
     * @param player jugador participante.
     * @param event  evento de dano original.
     * @return true si el dano esta permitido.
     */
    public boolean allowDamage(Player player, EntityDamageEvent event) {
        return state == EventState.RUNNING;
    }

    /**
     * Indica si el hambre del jugador puede variar durante la sesion.
     * Por defecto queda congelada todo el evento.
     *
     * @param player jugador participante.
     * @return true si el hambre puede variar.
     */
    public boolean allowHunger(Player player) {
        return false;
    }

    /**
     * Indica si el jugador puede romper o colocar bloques.
     * Por defecto los participantes no pueden modificar la arena.
     *
     * @param player jugador participante.
     * @return true si puede construir.
     */
    public boolean allowBuilding(Player player) {
        return false;
    }

    // ------------------------------------------------------------------
    // Utilidades para eventos concretos
    // ------------------------------------------------------------------

    protected final EventSettings settings() {
        return context.getEventManager().getSettings();
    }

    /**
     * Resuelve un punto de la arena como Location de Bukkit.
     *
     * @param key clave del punto.
     * @return posicion o null si no esta configurada o su mundo no cargo.
     */
    protected final Location resolvePoint(String key) {
        return arena.getPoint(key).map(ArenaLocation::toBukkit).orElse(null);
    }

    /**
     * Ejecuta una accion sobre cada jugador vivo online.
     *
     * @param action accion a ejecutar.
     */
    protected final void forEachAliveOnline(Consumer<Player> action) {
        forEachOnline(alive, action);
    }

    /**
     * Ejecuta una accion sobre cada participante online (vivos y
     * eliminados).
     *
     * @param action accion a ejecutar.
     */
    protected final void forEachParticipantOnline(Consumer<Player> action) {
        forEachOnline(alive, action);
        forEachOnline(eliminated, action);
    }

    /**
     * Quita a un participante de los conjuntos de la sesion sin
     * restaurarlo ni anunciar nada (usado por los sistemas de
     * reconexion, que conservan su snapshot).
     *
     * @param id identificador del jugador.
     * @return TRUE si estaba vivo, FALSE si estaba eliminado,
     *         null si no participaba.
     */
    protected final Boolean silentRemoveParticipant(UUID id) {
        if (alive.remove(id)) {
            return Boolean.TRUE;
        }
        if (eliminated.remove(id)) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * Reincorpora a un participante previamente removido con
     * {@link #silentRemoveParticipant(UUID)}.
     *
     * @param id      identificador del jugador.
     * @param asAlive true para reincorporarlo como vivo.
     */
    protected final void readmitParticipant(UUID id, boolean asAlive) {
        if (asAlive) {
            alive.add(id);
        } else {
            eliminated.add(id);
        }
    }

    /**
     * Extrae el snapshot almacenado de un jugador (por ejemplo, para
     * derivarlo al registro de restauraciones pendientes cuando expira
     * su ventana de reconexion).
     *
     * @param id identificador del jugador.
     * @return snapshot removido, o null si no habia.
     */
    protected final PlayerSnapshot removeStoredSnapshot(UUID id) {
        return snapshots.remove(id);
    }

    private void forEachOnline(Set<UUID> ids, Consumer<Player> action) {
        for (UUID id : new ArrayList<>(ids)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                action.accept(player);
            }
        }
    }

    private Player firstAliveOnline() {
        for (UUID id : alive) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                return player;
            }
        }
        return null;
    }

    /**
     * Envia un mensaje configurable a todos los participantes online.
     *
     * @param path      ruta del mensaje en messages.yml.
     * @param resolvers placeholders dinamicos.
     */
    public final void broadcast(String path, TagResolver... resolvers) {
        forEachParticipantOnline(player -> context.getMessages().send(player, path, resolvers));
    }

    /**
     * Nombre visible del evento, configurable en
     * {@code messages.yml -> event.names.<id>}.
     *
     * @return nombre visible del evento.
     */
    public final String displayName() {
        return context.getMessages().rawOr("event.names." + type.getId(), type.getId());
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public final EventState getState() {
        return state;
    }

    public final Arena getArena() {
        return arena;
    }

    public final GameEvent getType() {
        return type;
    }

    public final boolean isParticipant(UUID id) {
        return alive.contains(id) || eliminated.contains(id);
    }

    public final boolean isAlive(UUID id) {
        return alive.contains(id);
    }

    public final int getAliveCount() {
        return alive.size();
    }

    public final int getEliminatedCount() {
        return eliminated.size();
    }

    public final int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public final Set<UUID> getAlive() {
        return Collections.unmodifiableSet(alive);
    }

    public final Set<UUID> getEliminated() {
        return Collections.unmodifiableSet(eliminated);
    }
}
