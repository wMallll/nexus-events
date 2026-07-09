package com.nexusevents.event.hideandseek;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.event.EventContext;
import com.nexusevents.event.EventEndReason;
import com.nexusevents.event.EventSession;
import com.nexusevents.event.EventState;
import com.nexusevents.event.GameEvent;
import com.nexusevents.event.PlayerSnapshot;
import com.nexusevents.util.TimeUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Partida de "Escondete si puedes".
 *
 * <p>Fases internas: {@code HIDING} (cuenta regresiva por titulo para
 * esconderse, con el cazador congelado y ciego en su spawn),
 * {@code RELEASING} (cuenta regresiva corta) y {@code HUNTING} (el
 * cazador recibe su arma y elimina de un golpe). Los eliminados quedan
 * invisibles, con vuelo y sin capacidad de afectar el evento. Incluye
 * reconexion con ventana configurable para desconexiones involuntarias
 * y promocion automatica de cazador si el actual abandona.</p>
 */
public final class HideAndSeekSession extends EventSession {

    private enum Phase {
        HIDING,
        RELEASING,
        HUNTING
    }

    private static final class Disconnected {
        private final boolean wasAlive;
        private final boolean wasHunter;
        private final long expiryMillis;

        private Disconnected(boolean wasAlive, boolean wasHunter, long expiryMillis) {
            this.wasAlive = wasAlive;
            this.wasHunter = wasHunter;
            this.expiryMillis = expiryMillis;
        }
    }

    private final HideAndSeekConfig config;
    private final Set<UUID> hunters = new LinkedHashSet<>();
    private final Map<UUID, Disconnected> disconnected = new HashMap<>();

    private Phase phase = Phase.HIDING;
    private int hideSecondsLeft;
    private int releaseSecondsLeft;
    private HideAndSeekListener listener;

    public HideAndSeekSession(EventContext context, GameEvent type, Arena arena, HideAndSeekConfig config) {
        super(context, type, arena);
        this.config = config;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Override
    protected void onStart() {
        phase = Phase.HIDING;
        hideSecondsLeft = config.getHideSeconds();
        pickHunters();
        this.listener = new HideAndSeekListener(this);
        Bukkit.getPluginManager().registerEvents(listener, context.getPlugin());
    }

    private void pickHunters() {
        List<UUID> pool = new ArrayList<>(getAlive());
        Collections.shuffle(pool);
        int count = Math.min(config.getHunterCount(), Math.max(1, pool.size() - 1));
        for (int i = 0; i < count; i++) {
            hunters.add(pool.get(i));
        }
        for (UUID hunterId : hunters) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null) {
                setupWaitingHunter(hunter);
                broadcast("event.hide-and-seek.hunter-is",
                        Placeholder.unparsed("player", hunter.getName()));
            }
        }
    }

    private void setupWaitingHunter(Player hunter) {
        Location hunterSpawn = resolvePoint(ArenaKeys.HUNTER_SPAWN);
        if (hunterSpawn != null) {
            hunter.teleport(hunterSpawn);
        }
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        context.getTitles().showTitle(hunter, config.getHunterAssignedTitle());
        context.getMessages().send(hunter, "event.hide-and-seek.you-are-hunter");
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        expireDisconnected();
        switch (phase) {
            case HIDING:
                tickHiding();
                break;
            case RELEASING:
                tickReleasing();
                break;
            case HUNTING:
                break;
            default:
                break;
        }
        sendCounter(remaining);
    }

    private void tickHiding() {
        freezeHunters();
        TagResolver seconds = Placeholder.unparsed("seconds", String.valueOf(hideSecondsLeft));
        forEachAliveOnline(player -> context.getTitles().showTitle(player, config.getHidingTitle(), seconds));
        hideSecondsLeft--;
        if (hideSecondsLeft < 0) {
            phase = Phase.RELEASING;
            releaseSecondsLeft = config.getReleaseSeconds();
        }
    }

    private void tickReleasing() {
        freezeHunters();
        if (releaseSecondsLeft <= 0) {
            startHunt();
            return;
        }
        TagResolver seconds = Placeholder.unparsed("seconds", String.valueOf(releaseSecondsLeft));
        forEachAliveOnline(player -> {
            context.getTitles().showTitle(player, config.getReleaseTitle(), seconds);
            context.getSounds().play(player, "countdown-tick");
        });
        releaseSecondsLeft--;
    }

    private void freezeHunters() {
        Location hunterSpawn = resolvePoint(ArenaKeys.HUNTER_SPAWN);
        if (hunterSpawn == null) {
            return;
        }
        for (UUID hunterId : hunters) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null && isAlive(hunterId)) {
                hunter.teleport(hunterSpawn);
            }
        }
    }

    private void startHunt() {
        phase = Phase.HUNTING;
        for (UUID hunterId : hunters) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null && isAlive(hunterId)) {
                releaseHunter(hunter);
            }
        }
        broadcast("event.hide-and-seek.hunt-started");
        forEachParticipantOnline(player -> {
            context.getTitles().showTitle(player, config.getHuntStartedTitle());
            context.getSounds().play(player, "event-start");
        });
    }

    private void releaseHunter(Player hunter) {
        hunter.removePotionEffect(PotionEffectType.BLINDNESS);
        hunter.getInventory().setItem(0, config.buildWeapon(context.getPlugin().getLogger()));
        hunter.getInventory().setHeldItemSlot(0);
    }

    private void sendCounter(int remaining) {
        String format;
        String time;
        switch (phase) {
            case HIDING:
                format = config.getActionbarHiding();
                time = TimeUtil.formatSeconds(Math.max(0, hideSecondsLeft));
                break;
            case RELEASING:
                format = config.getActionbarRelease();
                time = TimeUtil.formatSeconds(Math.max(0, releaseSecondsLeft));
                break;
            default:
                format = config.getActionbarHunting();
                time = TimeUtil.formatSeconds(Math.max(0, remaining));
                break;
        }
        TagResolver[] resolvers = {
                Placeholder.unparsed("time", time),
                Placeholder.unparsed("alive", String.valueOf(hidersAlive())),
                Placeholder.unparsed("hunters", String.valueOf(huntersAlive()))
        };
        forEachParticipantOnline(player -> context.getTitles().sendActionBar(player, format, resolvers));
    }

    @Override
    protected void onEnd(EventEndReason reason) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (reason == EventEndReason.TIMEOUT) {
            announceSurvivors();
        }
        disconnected.clear();
        hunters.clear();
    }

    private void announceSurvivors() {
        StringJoiner winners = new StringJoiner(", ");
        for (UUID id : getAlive()) {
            if (!hunters.contains(id)) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    winners.add(player.getName());
                    context.getSounds().play(player, "event-win");
                }
            }
        }
        if (winners.length() > 0) {
            context.getMessages().broadcast("event.hide-and-seek.survivors-win",
                    Placeholder.unparsed("winners", winners.toString()));
        }
    }

    // ------------------------------------------------------------------
    // Golpes y eliminaciones
    // ------------------------------------------------------------------

    /**
     * Resuelve un golpe cuerpo a cuerpo entre dos jugadores: elimina al
     * escondido golpeado por el cazador y bloquea cualquier interferencia
     * de participantes que no sean el cazador.
     *
     * @param damager atacante.
     * @param victim  victima.
     * @param event   evento de dano original.
     */
    void handleHit(Player damager, Player victim, EntityDamageByEntityEvent event) {
        boolean damagerInSession = isParticipant(damager.getUniqueId());
        boolean victimInSession = isParticipant(victim.getUniqueId());
        if (!damagerInSession && !victimInSession) {
            return;
        }
        event.setCancelled(true);
        if (getState() != EventState.RUNNING || phase != Phase.HUNTING) {
            return;
        }
        if (!hunters.contains(damager.getUniqueId()) || !isAlive(damager.getUniqueId())) {
            return;
        }
        if (!isAlive(victim.getUniqueId()) || hunters.contains(victim.getUniqueId())) {
            return;
        }
        if (config.isRequireWeapon() && !config.isWeapon(damager.getInventory().getItemInMainHand())) {
            return;
        }
        eliminate(victim);
    }

    @Override
    protected void onPlayerEliminated(Player player) {
        applySpectatorState(player);
        context.getMessages().send(player, "event.hide-and-seek.eliminated-info");
        checkHuntEnd();
    }

    private void checkHuntEnd() {
        if (getState() == EventState.RUNNING && hidersAlive() == 0 && getAliveCount() > 0) {
            end(EventEndReason.WINNER);
        }
    }

    private int hidersAlive() {
        int count = 0;
        for (UUID id : getAlive()) {
            if (!hunters.contains(id)) {
                count++;
            }
        }
        return count;
    }

    private int huntersAlive() {
        int count = 0;
        for (UUID id : getAlive()) {
            if (hunters.contains(id)) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Desconexiones y reconexion
    // ------------------------------------------------------------------

    @Override
    public void playerDisconnected(Player player, boolean kicked) {
        boolean involuntary = kicked || !config.isTreatQuitAsVoluntary();
        if (getState() != EventState.RUNNING || !involuntary) {
            leave(player, true);
            return;
        }
        UUID id = player.getUniqueId();
        Boolean wasAlive = silentRemoveParticipant(id);
        if (wasAlive == null) {
            return;
        }
        boolean wasHunter = hunters.remove(id);
        long expiry = System.currentTimeMillis() + config.getReconnectWindowSeconds() * 1000L;
        disconnected.put(id, new Disconnected(wasAlive, wasHunter, expiry));
        broadcast("event.hide-and-seek.disconnect-wait",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("time", TimeUtil.formatSeconds(config.getReconnectWindowSeconds())));
    }

    @Override
    public boolean tryReconnect(Player player) {
        Disconnected info = disconnected.remove(player.getUniqueId());
        if (info == null) {
            return false;
        }
        readmitParticipant(player.getUniqueId(), info.wasAlive);
        if (info.wasHunter) {
            hunters.add(player.getUniqueId());
        }
        restoreParticipantState(player, info);
        attachHud(player);
        broadcast("event.hide-and-seek.reconnected",
                Placeholder.unparsed("player", player.getName()));
        return true;
    }

    private void restoreParticipantState(Player player, Disconnected info) {
        Location spawn = resolvePoint(getStartPointKey());
        if (!info.wasAlive) {
            if (spawn != null) {
                player.teleport(spawn);
            }
            onPlayerEliminatedState(player);
            return;
        }
        if (info.wasHunter) {
            setupWaitingHunter(player);
            if (phase == Phase.HUNTING) {
                releaseHunter(player);
            }
            return;
        }
        if (spawn != null) {
            player.teleport(spawn);
        }
    }

    private void onPlayerEliminatedState(Player player) {
        applySpectatorState(player);
    }

    private void expireDisconnected() {
        if (disconnected.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Disconnected>> iterator = disconnected.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Disconnected> entry = iterator.next();
            if (now < entry.getValue().expiryMillis) {
                continue;
            }
            iterator.remove();
            UUID id = entry.getKey();
            PlayerSnapshot snapshot = removeStoredSnapshot(id);
            context.getEventManager().addOrphanRestore(id, snapshot);
            String name = Bukkit.getOfflinePlayer(id).getName();
            broadcast("event.hide-and-seek.reconnect-expired",
                    Placeholder.unparsed("player", name != null ? name : "???"));
            if (entry.getValue().wasHunter) {
                ensureHunterExists();
            }
        }
        checkHuntEnd();
        checkVictory();
    }

    @Override
    protected void onPlayerLeft(Player player, boolean disconnectedLeave) {
        if (hunters.remove(player.getUniqueId())) {
            ensureHunterExists();
        }
        checkHuntEnd();
    }

    private void ensureHunterExists() {
        if (getState() != EventState.RUNNING || huntersAlive() > 0) {
            return;
        }
        List<UUID> candidates = new ArrayList<>();
        for (UUID id : getAlive()) {
            if (!hunters.contains(id)) {
                candidates.add(id);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        Collections.shuffle(candidates);
        UUID promoted = candidates.get(0);
        hunters.add(promoted);
        Player hunter = Bukkit.getPlayer(promoted);
        if (hunter == null) {
            return;
        }
        broadcast("event.hide-and-seek.new-hunter",
                Placeholder.unparsed("player", hunter.getName()));
        if (phase == Phase.HUNTING) {
            Location hunterSpawn = resolvePoint(ArenaKeys.HUNTER_SPAWN);
            if (hunterSpawn != null) {
                hunter.teleport(hunterSpawn);
            }
            releaseHunter(hunter);
        } else {
            setupWaitingHunter(hunter);
        }
        checkHuntEnd();
    }

    // ------------------------------------------------------------------
    // Reglas
    // ------------------------------------------------------------------

    @Override
    protected int getMaxDurationSeconds() {
        return config.getMaxDurationSeconds();
    }

    @Override
    protected boolean endsWhenOneRemains() {
        return false;
    }

    @Override
    public boolean allowDamage(Player player, org.bukkit.event.entity.EntityDamageEvent event) {
        return false;
    }
}
