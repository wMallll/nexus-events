package com.nexusevents.event.parkour;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Partida del Parkour por islas.
 *
 * <p>El recorrido se define como fragmentos (islas) ordenados con
 * {@code /evento parkour}. Tras la ventaja inicial, las islas se
 * desintegran una por una EN ESE ORDEN, y cada una de forma progresiva:
 * punados aleatorios de bloques cada pocos ticks, con pausa entre islas
 * y aceleracion opcional por isla. Caer bajo el recorrido elimina; el
 * ultimo en pie gana (o los sobrevivientes por tiempo). Todo se
 * restaura al finalizar.</p>
 */
public final class ParkourSession extends EventSession {

    private final ParkourConfig config;
    private final Random random = new Random();
    private final List<List<BlockState>> islands = new ArrayList<>();

    private World courseWorld;
    private int lowestY;
    private boolean collapsing;
    private int currentIsland;
    private int islandCursor;
    private int totalBlocks;
    private int brokenBlocks;
    private boolean pausedBetweenIslands;
    private boolean longPause;
    private int pauseSecondsLeft;
    private int nextIslandNumber;
    private long currentStepInterval;
    private BukkitTask stepTask;

    public ParkourSession(EventContext context, GameEvent type, Arena arena, ParkourConfig config) {
        super(context, type, arena);
        this.config = config;
    }

    // ------------------------------------------------------------------
    // Preparacion
    // ------------------------------------------------------------------

    @Override
    protected void onPrepare() {
        islands.clear();
        this.collapsing = false;
        this.currentIsland = 0;
        this.islandCursor = 0;
        this.currentStepInterval = config.getStepIntervalTicks();
        this.totalBlocks = 0;
        this.brokenBlocks = 0;
        this.pausedBetweenIslands = false;
        this.longPause = false;
        this.pauseSecondsLeft = 0;
        this.nextIslandNumber = 0;
        this.lowestY = Integer.MAX_VALUE;
        this.courseWorld = null;

        Set<Long> claimed = new HashSet<>();
        int[] overlapped = {0};
        int index = 1;
        Region fragment;
        int total = 0;
        while ((fragment = getArena().getRegion(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + index).orElse(null)) != null) {
            if (fragment.getWorld() == null) {
                context.getPlugin().getLogger().severe("parkour: el mundo del fragmento #" + index
                        + " no esta cargado. Se cancela.");
                end(EventEndReason.CANCELLED);
                return;
            }
            if (courseWorld == null) {
                courseWorld = fragment.getWorld();
            }
            List<BlockState> blocks = captureIsland(fragment, claimed, overlapped);
            Collections.shuffle(blocks, random);
            islands.add(blocks);
            lowestY = Math.min(lowestY, fragment.getMinY());
            total += blocks.size();
            index++;
        }
        this.totalBlocks = total;
        if (overlapped[0] > 0) {
            context.getPlugin().getLogger().info("parkour: " + overlapped[0]
                    + " bloques compartidos entre fragmentos superpuestos se asignaron al primero que los contiene.");
        }
        if (islands.isEmpty()) {
            context.getPlugin().getLogger().severe(
                    "parkour: la arena no tiene fragmentos (/evento parkour). Se cancela.");
            end(EventEndReason.CANCELLED);
            return;
        }
        if (context.getConfigManager().isDebug()) {
            context.getPlugin().getLogger().info("parkour: " + islands.size() + " islas capturadas ("
                    + total + " bloques solidos).");
        }
    }

    /**
     * Captura los bloques solidos del fragmento, saltando los que ya
     * fueron reclamados por un fragmento anterior: si dos cajas se
     * superponen, cada bloque pertenece solo a la primera isla que lo
     * contiene. Asi el total es exacto y el progreso llega al 100%.
     */
    private List<BlockState> captureIsland(Region fragment, Set<Long> claimed, int[] overlapped) {
        List<BlockState> blocks = new ArrayList<>();
        fragment.forEachBlock(block -> {
            if (block.getType() == Material.AIR) {
                return;
            }
            if (claimed.add(pack(block.getX(), block.getY(), block.getZ()))) {
                blocks.add(block.getState());
            } else {
                overlapped[0]++;
            }
        });
        return blocks;
    }

    private static long pack(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
    }

    @Override
    protected void onStart() {
        if (islands.isEmpty()) {
            return;
        }
        broadcast("event.parkour.islands-info",
                Placeholder.unparsed("count", String.valueOf(islands.size())));
    }

    // ------------------------------------------------------------------
    // Tick, colapso por islas y desintegracion progresiva
    // ------------------------------------------------------------------

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (islands.isEmpty()) {
            return;
        }
        checkFalls();
        if (getState() != EventState.RUNNING) {
            return;
        }
        if (!collapsing) {
            tickWaiting(elapsed);
        } else if (pausedBetweenIslands && pauseSecondsLeft > 0) {
            pauseSecondsLeft--;
        }
        sendCounter(remaining);
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
        forEachAliveOnline(player -> context.getTitles().showTitle(player, config.getStartTitle()));
        startIsland(0);
    }

    /**
     * Comienza la desintegracion progresiva de la isla indicada, con el
     * intervalo acelerado segun el multiplicador por isla.
     */
    private void startIsland(int index) {
        this.currentIsland = index;
        this.islandCursor = 0;
        this.currentStepInterval = Math.max(1L, Math.round(
                config.getStepIntervalTicks() * Math.pow(config.getIslandSpeedMultiplier(), index)));
        this.stepTask = context.getScheduler().syncTimer(this::collapseStep,
                currentStepInterval, currentStepInterval);
    }

    private void collapseStep() {
        if (getState() != EventState.RUNNING || currentIsland >= islands.size()) {
            return;
        }
        List<BlockState> island = islands.get(currentIsland);
        Location last = null;
        int removed = 0;
        while (removed < config.getBlocksPerStep() && islandCursor < island.size()) {
            Block block = island.get(islandCursor).getBlock();
            islandCursor++;
            // Cada bloque capturado que sale de la lista cuenta para el
            // progreso, aunque la fisica vanilla (arena que cae, hojas
            // que se descomponen, pastos que se sueltan) lo haya vaciado
            // antes de su turno: asi el porcentaje siempre llega a 100.
            brokenBlocks++;
            Material previousType = block.getType();
            if (previousType != Material.AIR) {
                // Sin fisica: los bloques vecinos no reaccionan (la
                // arena no cae, nada se suelta ni deja drops).
                XBlock.setType(block, XMaterial.AIR, false);
                if (config.isBreakEffect()) {
                    playBlockBreakEffect(block, previousType);
                }
                last = block.getLocation();
                removed++;
            }
        }
        if (last != null) {
            playCollapseSound(last);
        }
        if (islandCursor >= island.size()) {
            finishIsland();
        }
    }

    private void finishIsland() {
        if (stepTask != null) {
            stepTask.cancel();
            stepTask = null;
        }
        int finished = currentIsland + 1;
        if (config.isAnnounceIslands()) {
            broadcast("event.parkour.island-collapsed",
                    Placeholder.unparsed("island", String.valueOf(finished)));
        }
        int next = currentIsland + 1;
        if (next >= islands.size()) {
            return;
        }
        long delayTicks = Math.max(1L, config.delayAfterIsland(finished));
        this.nextIslandNumber = next + 1;
        this.pausedBetweenIslands = true;
        this.longPause = config.hasDelayOverride(finished);
        this.pauseSecondsLeft = (int) Math.ceil(delayTicks / 20.0);
        if (config.hasDelayOverride(finished)) {
            // Pausa especial configurada: se anuncia para que los
            // jugadores sepan cuanto falta para la proxima isla.
            broadcast("event.parkour.long-pause",
                    Placeholder.unparsed("island", String.valueOf(nextIslandNumber)),
                    Placeholder.unparsed("time", TimeUtil.formatSeconds(Math.max(1, pauseSecondsLeft))));
        }
        context.getScheduler().syncLater(() -> {
            pausedBetweenIslands = false;
            longPause = false;
            pauseSecondsLeft = 0;
            if (getState() == EventState.RUNNING) {
                startIsland(next);
            }
        }, delayTicks);
    }

    private void playCollapseSound(Location at) {
        if (!config.isCollapseSoundEnabled()) {
            return;
        }
        int radius = config.getCollapseSoundRadius();
        context.getSounds().get("parkour-collapse").ifPresent(sound ->
                forEachAliveOnline(player -> {
                    if (player.getWorld().equals(at.getWorld())
                            && player.getLocation().distanceSquared(at) <= (double) radius * radius) {
                        sound.play(player, at);
                    }
                }));
    }

    // ------------------------------------------------------------------
    // Caidas, HUD y cierre
    // ------------------------------------------------------------------

    private void checkFalls() {
        int threshold = lowestY - config.getFallDistance();
        List<Player> fallen = new ArrayList<>();
        forEachAliveOnline(player -> {
            Location location = player.getLocation();
            if (location.getWorld() != null
                    && location.getWorld().equals(courseWorld)
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
        Location spawn = resolveStartLocation();
        if (spawn != null) {
            player.teleport(spawn);
        }
        applySpectatorState(player);
        context.getMessages().send(player, "event.parkour.eliminated-info");
    }

    private void sendCounter(int remaining) {
        String format;
        String time;
        if (!collapsing) {
            format = config.getActionbarWaiting();
            time = TimeUtil.formatSeconds(Math.max(0, config.getStartDelaySeconds() - getElapsedSeconds()));
        } else if (pausedBetweenIslands && longPause) {
            // Solo las pausas especiales cambian la actionbar: las
            // pausas cortas por defecto mantienen el formato estable.
            format = config.getActionbarPause();
            time = TimeUtil.formatSeconds(Math.max(0, pauseSecondsLeft));
        } else {
            format = config.getActionbarCollapsing();
            time = TimeUtil.formatSeconds(Math.max(0, remaining));
        }
        String island = Math.min(currentIsland + 1, islands.size()) + "/" + islands.size();
        TagResolver[] resolvers = {
                Placeholder.unparsed("time", time),
                Placeholder.unparsed("island", island),
                Placeholder.unparsed("next", String.valueOf(nextIslandNumber)),
                Placeholder.unparsed("progress", String.valueOf(progressPercent())),
                Placeholder.unparsed("alive", String.valueOf(getAliveCount()))
        };
        forEachParticipantOnline(player -> context.getTitles().sendActionBar(player, format, resolvers));
    }

    /**
     * Porcentaje de bloques del recorrido ya desintegrados, para el
     * placeholder {@code <progress>} del HUD y las actionbars.
     */
    private int progressPercent() {
        if (totalBlocks <= 0) {
            return 0;
        }
        return Math.min(100, brokenBlocks * 100 / totalBlocks);
    }

    @Override
    protected List<TagResolver> extraHudResolvers() {
        String island = islands.isEmpty() ? "0/0"
                : Math.min(currentIsland + 1, islands.size()) + "/" + islands.size();
        List<TagResolver> resolvers = new ArrayList<>(2);
        resolvers.add(Placeholder.unparsed("island", island));
        resolvers.add(Placeholder.unparsed("progress", String.valueOf(progressPercent())));
        return resolvers;
    }

    @Override
    protected void onEnd(EventEndReason reason) {
        if (stepTask != null) {
            stepTask.cancel();
            stepTask = null;
        }
        for (List<BlockState> island : islands) {
            for (BlockState state : island) {
                state.update(true, false);
            }
        }
        islands.clear();
        if (reason == EventEndReason.TIMEOUT) {
            announceSurvivors();
        }
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
