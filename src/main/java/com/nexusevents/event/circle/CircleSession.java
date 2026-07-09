package com.nexusevents.event.circle;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
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
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Partida de "El Circulo".
 *
 * <p>El administrador define centro y radio parado en el medio del piso
 * circular ({@code /evento setcircle (radio)}); el evento escanea los
 * bloques solidos existentes dentro de ese cilindro y, al comenzar, los
 * va rompiendo de forma doblemente aleatoria: posiciones uniformes sin
 * repeticion (lista barajada) y momentos aleatorios (tarea que se
 * auto-reprograma con retardo variable), con aceleracion opcional.</p>
 *
 * <p>Al iniciar se activa el PvP (solo golpes de mano y proyectiles) y
 * cada jugador recibe bolas de nieve para empujar rivales al vacio.
 * Nadie muere de verdad: los golpes letales eliminan limpiamente, igual
 * que caer bajo el piso. El ultimo jugador vivo gana y todos los
 * bloques se restauran al finalizar.</p>
 */
public final class CircleSession extends EventSession {

    private final CircleConfig config;
    private final Random random = new Random();
    private final List<BlockState> blocks = new ArrayList<>();
    private final Set<Long> memberIndex = new HashSet<>();

    private Location center;
    private int radius;
    private int cursor;
    private int brokenBlocks;
    private int currentMaxBlocks;
    private boolean breaking;
    private int breakingElapsedSeconds;
    private BukkitTask breakTask;
    private CircleListener listener;

    public CircleSession(EventContext context, GameEvent type, Arena arena, CircleConfig config) {
        super(context, type, arena);
        this.config = config;
    }

    // ------------------------------------------------------------------
    // Preparacion e inicio
    // ------------------------------------------------------------------

    @Override
    protected void onPrepare() {
        this.center = resolvePoint(ArenaKeys.CIRCLE_CENTER);
        if (center == null || center.getWorld() == null) {
            context.getPlugin().getLogger().severe("circle: el centro del circulo no esta disponible. Se cancela.");
            end(EventEndReason.CANCELLED);
            return;
        }
        this.radius = resolveRadius();
        this.currentMaxBlocks = config.getBreakMaxBlocks();
        this.cursor = 0;
        this.brokenBlocks = 0;
        this.breaking = false;
        this.breakingElapsedSeconds = 0;
        captureAndShuffleBlocks();
        if (blocks.isEmpty()) {
            context.getPlugin().getLogger().severe("circle: no se encontraron bloques dentro del radio "
                    + radius + ". Se cancela.");
            end(EventEndReason.CANCELLED);
            return;
        }
        if (context.getConfigManager().isDebug()) {
            context.getPlugin().getLogger().info("circle: capturados " + blocks.size()
                    + " bloques del piso (radio " + radius + ").");
        }
    }

    private int resolveRadius() {
        String raw = getArena().getProperty(ArenaKeys.CIRCLE_RADIUS).orElse(null);
        if (raw != null) {
            try {
                return Math.max(3, Integer.parseInt(raw.trim()));
            } catch (NumberFormatException exception) {
                context.getPlugin().getLogger().warning("circle: radio invalido '" + raw
                        + "' en la arena '" + getArena().getName() + "'. Se usa el radio por defecto.");
            }
        }
        return config.getDefaultRadius();
    }

    /**
     * Captura los bloques solidos del cilindro (radio horizontal desde
     * el centro, capas verticales configurables) y los baraja una unica
     * vez: consumir la lista en orden equivale a elegir posiciones
     * aleatorias uniformes sin repeticion.
     */
    private void captureAndShuffleBlocks() {
        blocks.clear();
        memberIndex.clear();
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        int radiusSq = radius * radius;
        int minY = centerY - config.getScanDown();
        int maxY = centerY + config.getScanUp();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        blocks.add(block.getState());
                        memberIndex.add(pack(x, y, z));
                    }
                }
            }
        }
        Collections.shuffle(blocks, random);
    }

    /**
     * Los jugadores comienzan en el centro del circulo, salvo que la
     * arena tenga un punto de partida especifico para este evento.
     */
    @Override
    protected Location resolveStartLocation() {
        Location specific = resolvePoint(ArenaKeys.SPAWN + "_" + getType().getId());
        return specific != null ? specific : center;
    }

    @Override
    protected void onStart() {
        if (center == null) {
            return;
        }
        this.listener = new CircleListener(this);
        Bukkit.getPluginManager().registerEvents(listener, context.getPlugin());
        giveSnowballs();
        broadcast("event.circle.start");
        forEachAliveOnline(player -> context.getTitles().showTitle(player, config.getStartTitle()));
    }

    private void giveSnowballs() {
        if (!config.isSnowballsEnabled() || config.getSnowballStacks() <= 0) {
            return;
        }
        ItemStack snowball = XMaterial.SNOWBALL.parseItem();
        if (snowball == null) {
            context.getPlugin().getLogger().warning("circle: no se pudo crear la bola de nieve en esta version.");
            return;
        }
        snowball.setAmount(config.getSnowballPerStack());
        forEachAliveOnline(player -> {
            for (int slot = 0; slot < config.getSnowballStacks(); slot++) {
                player.getInventory().setItem(slot, snowball.clone());
            }
        });
    }

    // ------------------------------------------------------------------
    // Tick y rompedor aleatorio
    // ------------------------------------------------------------------

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (center == null) {
            return;
        }
        checkFalls();
        if (getState() != EventState.RUNNING) {
            return;
        }
        if (!breaking) {
            tickWaiting(elapsed);
        } else {
            tickBreaking();
        }
        drawBorder();
        sendCounter(remaining);
    }

    private void tickWaiting(int elapsed) {
        int secondsToBreak = config.getStartDelaySeconds() - elapsed;
        if (secondsToBreak > 0 && secondsToBreak <= 3) {
            TagResolver seconds = Placeholder.unparsed("seconds", String.valueOf(secondsToBreak));
            forEachAliveOnline(player -> {
                context.getTitles().showTitle(player, config.getWarningTitle(), seconds);
                context.getSounds().play(player, "countdown-tick");
            });
        }
        if (elapsed >= config.getStartDelaySeconds()) {
            breaking = true;
            broadcast("event.circle.breaking-start");
            scheduleNextBreak();
        }
    }

    private void tickBreaking() {
        breakingElapsedSeconds++;
        if (config.getSpeedUpEverySeconds() > 0
                && breakingElapsedSeconds % config.getSpeedUpEverySeconds() == 0) {
            currentMaxBlocks = Math.min(config.getMaxBlocksCap(), currentMaxBlocks + 1);
        }
    }

    /**
     * Programa la siguiente rotura con un retardo aleatorio entre el
     * minimo y el maximo configurados: el "cuando" tambien es azar.
     */
    private void scheduleNextBreak() {
        long delay = randomBetween(config.getBreakMinDelayTicks(), config.getBreakMaxDelayTicks());
        breakTask = context.getScheduler().syncLater(this::breakStep, delay);
    }

    private void breakStep() {
        if (getState() != EventState.RUNNING) {
            return;
        }
        Block seed = nextSeed();
        if (seed == null) {
            return;
        }
        int target = (int) randomBetween(config.getBreakMinBlocks(), currentMaxBlocks);
        for (Block block : collectCluster(seed, target)) {
            Material previousType = block.getType();
            // Sin fisica: evita caidas de arena, desprendimientos y
            // drops que desvirtuen el conteo y ensucien el piso.
            XBlock.setType(block, XMaterial.AIR, false);
            if (config.isBreakEffect()) {
                playBlockBreakEffect(block, previousType);
            }
            brokenBlocks++;
        }
        playBreakSound(seed.getLocation());
        if (brokenBlocks < blocks.size()) {
            scheduleNextBreak();
        }
    }

    /**
     * Devuelve el siguiente bloque semilla todavia intacto de la lista
     * barajada (posicion uniformemente aleatoria sin repeticion).
     */
    private Block nextSeed() {
        while (cursor < blocks.size()) {
            Block block = blocks.get(cursor).getBlock();
            cursor++;
            if (block.getType() != Material.AIR) {
                return block;
            }
        }
        return null;
    }

    /**
     * Junta un grupo de bloques PEGADOS a partir de la semilla,
     * expandiendo por vecinos adyacentes (BFS) dentro del piso
     * capturado, hasta alcanzar el tamano pedido.
     */
    private List<Block> collectCluster(Block seed, int target) {
        List<Block> cluster = new ArrayList<>(target);
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(seed);
        visited.add(pack(seed.getX(), seed.getY(), seed.getZ()));
        while (!queue.isEmpty() && cluster.size() < target) {
            Block block = queue.poll();
            if (block.getType() != Material.AIR) {
                cluster.add(block);
            }
            for (int[] dir : NEIGHBORS) {
                int x = block.getX() + dir[0];
                int y = block.getY() + dir[1];
                int z = block.getZ() + dir[2];
                long key = pack(x, y, z);
                if (memberIndex.contains(key) && visited.add(key)) {
                    queue.add(block.getWorld().getBlockAt(x, y, z));
                }
            }
        }
        return cluster;
    }

    private static final int[][] NEIGHBORS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}
    };

    private static long pack(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
    }

    private void playBreakSound(Location at) {
        context.getSounds().get("circle-break").ifPresent(sound ->
                forEachAliveOnline(player -> {
                    if (player.getWorld().equals(at.getWorld())
                            && player.getLocation().distanceSquared(at) <= 24 * 24) {
                        sound.play(player, at);
                    }
                }));
    }

    private long randomBetween(long min, long max) {
        if (max <= min) {
            return min;
        }
        return min + (long) (random.nextDouble() * (max - min + 1));
    }

    private void drawBorder() {
        if (config.getBorderParticle() == null) {
            return;
        }
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int points = config.getParticlePoints();
        double angleStep = (Math.PI * 2) / points;
        for (int ring = 0; ring < config.getParticleRings(); ring++) {
            double y = center.getY() + 0.5 + ring;
            for (int i = 0; i < points; i++) {
                double angle = angleStep * i;
                double x = center.getX() + Math.cos(angle) * radius;
                double z = center.getZ() + Math.sin(angle) * radius;
                world.spawnParticle(config.getBorderParticle(), x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------
    // Caidas, eliminaciones y cierre
    // ------------------------------------------------------------------

    /**
     * Empuje manual de la bola de nieve: se aplica un tick despues del
     * impacto para imponerse a la fisica vanilla (que con dano 0 puede
     * no empujar) y a cualquier otro plugin.
     *
     * @param victim             jugador golpeado.
     * @param projectileVelocity velocidad del proyectil al impactar.
     */
    void applySnowballPush(Player victim, Vector projectileVelocity) {
        Vector direction = projectileVelocity.clone().setY(0);
        if (direction.lengthSquared() < 1.0E-4) {
            return;
        }
        Vector push = direction.normalize()
                .multiply(config.getKnockbackHorizontal())
                .setY(config.getKnockbackVertical());
        context.getScheduler().syncLater(() -> {
            if (victim.isOnline() && isAlive(victim.getUniqueId())) {
                victim.setVelocity(push);
            }
        }, 1L);
    }

    private void checkFalls() {
        int threshold = center.getBlockY() - config.getFallDistance();
        List<Player> fallen = new ArrayList<>();
        forEachAliveOnline(player -> {
            Location location = player.getLocation();
            if (location.getWorld() != null
                    && location.getWorld().equals(center.getWorld())
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
        player.setHealth(player.getMaxHealth());
        player.getInventory().clear();
        player.teleport(center);
        applySpectatorState(player);
        context.getMessages().send(player, "event.circle.eliminated-info");
    }

    private void sendCounter(int remaining) {
        String format = breaking ? config.getActionbarPlaying() : config.getActionbarWaiting();
        int broken = blocks.isEmpty() ? 0 : Math.min(100, brokenBlocks * 100 / blocks.size());
        String time = breaking
                ? TimeUtil.formatSeconds(Math.max(0, remaining))
                : TimeUtil.formatSeconds(Math.max(0, config.getStartDelaySeconds() - getElapsedSeconds()));
        TagResolver[] resolvers = {
                Placeholder.unparsed("time", time),
                Placeholder.unparsed("broken", String.valueOf(broken)),
                Placeholder.unparsed("radius", String.valueOf(radius)),
                Placeholder.unparsed("alive", String.valueOf(getAliveCount()))
        };
        forEachParticipantOnline(player -> context.getTitles().sendActionBar(player, format, resolvers));
    }

    @Override
    protected void onEnd(EventEndReason reason) {
        if (breakTask != null) {
            breakTask.cancel();
            breakTask = null;
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
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
            context.getMessages().broadcast("event.circle.winners",
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

    /**
     * PvP activo: solo golpes cuerpo a cuerpo y proyectiles (bolas de
     * nieve, con su empuje). Caidas y cualquier otra causa quedan
     * bloqueadas; los golpes letales los convierte en eliminacion el
     * listener de la sesion.
     */
    @Override
    public boolean allowDamage(Player player, EntityDamageEvent event) {
        if (getState() != EventState.RUNNING) {
            return false;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        return cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || cause == EntityDamageEvent.DamageCause.PROJECTILE;
    }
}
