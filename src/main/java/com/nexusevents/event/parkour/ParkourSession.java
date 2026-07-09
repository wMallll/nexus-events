package com.nexusevents.event.parkour;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.arena.Region;
import com.nexusevents.event.EventContext;
import com.nexusevents.event.EventEndReason;
import com.nexusevents.event.EventSession;
import com.nexusevents.event.EventState;
import com.nexusevents.event.GameEvent;
import com.nexusevents.util.TimeUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Partida de Parkour Colapsable.
 *
 * <p>Optimizado para evitar lag: al comenzar se capturan unicamente los
 * bloques solidos del recorrido y se ordenan una sola vez por distancia
 * al punto de inicio; el colapso avanza consumiendo esa lista en lotes
 * pequenos a intervalos regulares (nunca miles de bloques en un mismo
 * tick), con aceleracion progresiva y tope configurable. La onda sigue
 * la forma real del recorrido, aunque sea curvo. Al finalizar, todos
 * los bloques se restauran a su estado original.</p>
 */
public final class ParkourSession extends EventSession {

    private final ParkourConfig config;
    private final List<BlockState> blocks = new ArrayList<>();

    private Region course;
    private int cursor;
    private int blocksPerStep;
    private boolean collapsing;
    private int collapseElapsedSeconds;
    private BukkitTask collapseTask;

    public ParkourSession(EventContext context, GameEvent type, Arena arena, ParkourConfig config) {
        super(context, type, arena);
        this.config = config;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Override
    protected void onStart() {
        this.course = getArena().getRegion(ArenaKeys.REGION_PARKOUR).orElse(null);
        if (course == null || course.getWorld() == null) {
            context.getPlugin().getLogger().severe("parkour: el recorrido no esta disponible. Se cancela.");
            end(EventEndReason.CANCELLED);
            return;
        }
        this.blocksPerStep = config.getBlocksPerStepStart();
        this.cursor = 0;
        this.collapsing = false;
        this.collapseElapsedSeconds = 0;
        captureAndSortBlocks();
        if (context.getConfigManager().isDebug()) {
            context.getPlugin().getLogger().info("parkour: capturados " + blocks.size()
                    + " bloques solidos de " + course.getVolume() + " posibles.");
        }
    }

    /**
     * Captura solo los bloques solidos del recorrido (un parkour es
     * mayormente aire) y los ordena por distancia al punto de inicio,
     * definiendo el orden exacto del colapso de una sola vez.
     */
    private void captureAndSortBlocks() {
        blocks.clear();
        course.forEachBlock(block -> {
            if (block.getType() != Material.AIR) {
                blocks.add(block.getState());
            }
        });
        Location origin = resolvePoint(getStartPointKey());
        final double originX = origin != null ? origin.getX() : course.getMinX();
        final double originY = origin != null ? origin.getY() : course.getMinY();
        final double originZ = origin != null ? origin.getZ() : course.getMinZ();
        blocks.sort(Comparator.comparingDouble(state -> {
            double dx = state.getX() + 0.5 - originX;
            double dy = state.getY() + 0.5 - originY;
            double dz = state.getZ() + 0.5 - originZ;
            return dx * dx + dy * dy + dz * dz;
        }));
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (course == null) {
            return;
        }
        checkFalls();
        if (getState() != EventState.RUNNING) {
            return;
        }
        if (!collapsing) {
            tickWaiting(elapsed);
        } else {
            tickCollapsing();
        }
        sendCounter(elapsed, remaining);
    }

    private void tickWaiting(int elapsed) {
        int secondsToCollapse = config.getStartDelaySeconds() - elapsed;
        if (secondsToCollapse > 0 && secondsToCollapse <= 3) {
            TagResolver seconds = Placeholder.unparsed("seconds", String.valueOf(secondsToCollapse));
            forEachAliveOnline(player -> {
                context.getTitles().showTitle(player, config.getWarningTitle(), seconds);
                context.getSounds().play(player, "countdown-tick");
            });
        }
        if (elapsed >= config.getStartDelaySeconds()) {
            beginCollapse();
        }
    }

    private void beginCollapse() {
        collapsing = true;
        broadcast("event.parkour.collapse-start");
        forEachParticipantOnline(player -> {
            context.getTitles().showTitle(player, config.getStartTitle());
            context.getSounds().play(player, "event-start");
        });
        collapseTask = context.getScheduler().syncTimer(this::collapseStep,
                config.getIntervalTicks(), config.getIntervalTicks());
    }

    private void tickCollapsing() {
        collapseElapsedSeconds++;
        if (config.getSpeedUpEverySeconds() > 0 && config.getSpeedUpAmount() > 0
                && collapseElapsedSeconds % config.getSpeedUpEverySeconds() == 0) {
            blocksPerStep = Math.min(config.getMaxBlocksPerStep(), blocksPerStep + config.getSpeedUpAmount());
        }
    }

    /**
     * Un paso del colapso: elimina el siguiente lote de bloques de la
     * lista pre-ordenada y reproduce un unico sonido en el frente de
     * colapso para los jugadores cercanos.
     */
    private void collapseStep() {
        if (getState() != EventState.RUNNING) {
            return;
        }
        int removed = 0;
        Location front = null;
        while (removed < blocksPerStep && cursor < blocks.size()) {
            Block block = blocks.get(cursor).getBlock();
            cursor++;
            if (block.getType() != Material.AIR) {
                block.setType(Material.AIR);
                front = block.getLocation();
                removed++;
            }
        }
        if (front != null && config.isCollapseSoundEnabled()) {
            playCollapseSound(front);
        }
        if (cursor >= blocks.size()) {
            cancelCollapseTask();
        }
    }

    private void playCollapseSound(Location front) {
        double radiusSq = (double) config.getCollapseSoundRadius() * config.getCollapseSoundRadius();
        context.getSounds().get("parkour-collapse").ifPresent(sound ->
                forEachAliveOnline(player -> {
                    if (player.getWorld().equals(front.getWorld())
                            && player.getLocation().distanceSquared(front) <= radiusSq) {
                        sound.play(player, front);
                    }
                }));
    }

    private void cancelCollapseTask() {
        if (collapseTask != null) {
            collapseTask.cancel();
            collapseTask = null;
        }
    }

    @Override
    protected void onEnd(EventEndReason reason) {
        cancelCollapseTask();
        restoreBlocks();
        if (reason == EventEndReason.TIMEOUT) {
            announceSurvivors();
        }
    }

    private void restoreBlocks() {
        for (BlockState state : blocks) {
            state.update(true, false);
        }
        blocks.clear();
    }

    private void announceSurvivors() {
        StringJoiner winners = new StringJoiner(", ");
        for (UUID id : getAlive()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                winners.add(player.getName());
                context.getSounds().play(player, "event-win");
            }
        }
        if (winners.length() > 0) {
            context.getMessages().broadcast("event.parkour.winners",
                    Placeholder.unparsed("winners", winners.toString()));
        }
    }

    // ------------------------------------------------------------------
    // Caidas y eliminaciones
    // ------------------------------------------------------------------

    private void checkFalls() {
        int threshold = course.getMinY() - config.getFallDistance();
        List<Player> fallen = new ArrayList<>();
        forEachAliveOnline(player -> {
            Location location = player.getLocation();
            if (location.getWorld() != null
                    && location.getWorld().getName().equalsIgnoreCase(course.getWorldName())
                    && location.getY() < threshold) {
                fallen.add(player);
            }
        });
        for (Player player : fallen) {
            eliminate(player);
        }
    }

    @Override
    protected void onPlayerEliminated(Player player) {
        Location spawn = resolvePoint(getStartPointKey());
        if (spawn != null) {
            player.teleport(spawn);
        }
        applySpectatorState(player);
        context.getMessages().send(player, "event.parkour.eliminated-info");
    }

    private void sendCounter(int elapsed, int remaining) {
        String format;
        String time;
        if (!collapsing) {
            format = config.getActionbarWaiting();
            time = TimeUtil.formatSeconds(Math.max(0, config.getStartDelaySeconds() - elapsed));
        } else {
            format = config.getActionbarCollapsing();
            time = TimeUtil.formatSeconds(Math.max(0, remaining));
        }
        int progress = blocks.isEmpty() ? 0 : Math.min(100, cursor * 100 / blocks.size());
        TagResolver[] resolvers = {
                Placeholder.unparsed("time", time),
                Placeholder.unparsed("progress", String.valueOf(progress)),
                Placeholder.unparsed("alive", String.valueOf(getAliveCount()))
        };
        forEachParticipantOnline(player -> context.getTitles().sendActionBar(player, format, resolvers));
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
        return config.isWinWhenOneRemains();
    }

    @Override
    public boolean allowDamage(Player player, org.bukkit.event.entity.EntityDamageEvent event) {
        return false;
    }
}
