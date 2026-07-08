package com.nexusevents.event.pixelparty;

import com.cryptomorin.xseries.XBlock;
import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.arena.Region;
import com.nexusevents.event.EventContext;
import com.nexusevents.event.EventEndReason;
import com.nexusevents.event.EventSession;
import com.nexusevents.event.EventState;
import com.nexusevents.event.GameEvent;
import com.nexusevents.event.pixelparty.PixelPartyConfig.PaletteColor;
import com.nexusevents.util.TimeUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Partida de Pixel Party.
 *
 * <p>La plataforma se divide en mosaicos y se repinta con colores
 * aleatorios en cada ronda. Se anuncia un color por titulo (y opcional
 * item en la hotbar); al agotarse el tiempo, los bloques de los demas
 * colores desaparecen y quien cae queda eliminado. Las rondas se
 * aceleran progresivamente y el evento termina por tiempo: todos los
 * que siguen en pie ganan (nunca por ultimo jugador, segun la
 * especificacion). Al finalizar, la plataforma se restaura bloque por
 * bloque a su estado original.</p>
 */
public final class PixelPartySession extends EventSession {

    private enum Phase {
        SHOWING,
        PAUSE
    }

    private static final int TARGET_ITEM_SLOT = 4;

    private final PixelPartyConfig config;
    private final Random random = new Random();
    private final List<BlockState> originalBlocks = new ArrayList<>();

    private Region platform;
    private int[][] tileColors;
    private int tilesX;
    private int tilesZ;

    private Phase phase = Phase.PAUSE;
    private int round;
    private int phaseSecondsLeft;
    private int targetColor;

    public PixelPartySession(EventContext context, GameEvent type, Arena arena, PixelPartyConfig config) {
        super(context, type, arena);
        this.config = config;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Override
    protected void onStart() {
        this.platform = getArena().getRegion(ArenaKeys.REGION_PIXEL_PARTY).orElse(null);
        if (platform == null || platform.getWorld() == null) {
            context.getPlugin().getLogger().severe("pixel-party: la plataforma no esta disponible. Se cancela.");
            end(EventEndReason.CANCELLED);
            return;
        }
        captureOriginalBlocks();
        int tileSize = config.getTileSize();
        this.tilesX = (platform.getWidthX() + tileSize - 1) / tileSize;
        this.tilesZ = (platform.getWidthZ() + tileSize - 1) / tileSize;
        this.tileColors = new int[tilesX][tilesZ];

        this.round = 0;
        this.phase = Phase.PAUSE;
        this.phaseSecondsLeft = config.getPauseSeconds();
        paintNewPattern();
    }

    private void captureOriginalBlocks() {
        originalBlocks.clear();
        platform.forEachBlock(block -> originalBlocks.add(block.getState()));
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (platform == null) {
            return;
        }
        checkFalls();
        if (getState() != EventState.RUNNING) {
            return;
        }
        if (phase == Phase.SHOWING) {
            tickShowing();
        } else {
            tickPause();
        }
        sendCounter(remaining);
    }

    private void tickShowing() {
        phaseSecondsLeft--;
        if (phaseSecondsLeft > 0) {
            if (phaseSecondsLeft <= 3) {
                forEachAliveOnline(player -> context.getSounds().play(player, "countdown-tick"));
            }
            return;
        }
        clearWrongBlocks();
        phase = Phase.PAUSE;
        phaseSecondsLeft = config.getPauseSeconds();
        forEachParticipantOnline(player -> {
            context.getTitles().showTitle(player, config.getClearedTitle());
            context.getSounds().play(player, "pixel-clear");
            if (config.isGiveTargetItem() && isAlive(player.getUniqueId())) {
                player.getInventory().setItem(TARGET_ITEM_SLOT, null);
            }
        });
    }

    private void tickPause() {
        phaseSecondsLeft--;
        if (phaseSecondsLeft <= 0) {
            startRound();
        }
    }

    private void startRound() {
        round++;
        paintNewPattern();
        targetColor = random.nextInt(config.getPalette().size());
        phase = Phase.SHOWING;
        phaseSecondsLeft = config.roundTimeFor(round);

        PaletteColor color = config.getPalette().get(targetColor);
        TagResolver[] resolvers = {
                Placeholder.parsed("color", color.getColorTag()),
                Placeholder.unparsed("colorname", color.getName()),
                Placeholder.unparsed("round", String.valueOf(round))
        };
        broadcast("event.pixel-party.round", resolvers);
        ItemStack targetItem = config.isGiveTargetItem() ? color.getMaterial().parseItem() : null;
        forEachAliveOnline(player -> {
            context.getTitles().showTitle(player, config.getColorTitle(), resolvers);
            context.getSounds().play(player, "pixel-color");
            if (targetItem != null) {
                player.getInventory().setItem(TARGET_ITEM_SLOT, targetItem.clone());
            }
        });
    }

    @Override
    protected void onEnd(EventEndReason reason) {
        restoreOriginalBlocks();
        if (reason == EventEndReason.TIMEOUT) {
            announceSurvivors();
        }
    }

    private void restoreOriginalBlocks() {
        for (BlockState state : originalBlocks) {
            state.update(true, false);
        }
        originalBlocks.clear();
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
            context.getMessages().broadcast("event.pixel-party.winners",
                    Placeholder.unparsed("winners", winners.toString()));
        }
    }

    // ------------------------------------------------------------------
    // Plataforma
    // ------------------------------------------------------------------

    private void paintNewPattern() {
        for (int tx = 0; tx < tilesX; tx++) {
            for (int tz = 0; tz < tilesZ; tz++) {
                tileColors[tx][tz] = random.nextInt(config.getPalette().size());
            }
        }
        platform.forEachBlock(block ->
                XBlock.setType(block, config.getPalette().get(colorIndexAt(block.getX(), block.getZ())).getMaterial(), false));
    }

    private void clearWrongBlocks() {
        platform.forEachBlock(block -> {
            if (colorIndexAt(block.getX(), block.getZ()) != targetColor) {
                block.setType(Material.AIR);
            }
        });
    }

    private int colorIndexAt(int x, int z) {
        int tx = (x - platform.getMinX()) / config.getTileSize();
        int tz = (z - platform.getMinZ()) / config.getTileSize();
        return tileColors[Math.min(tx, tilesX - 1)][Math.min(tz, tilesZ - 1)];
    }

    // ------------------------------------------------------------------
    // Caidas y eliminaciones
    // ------------------------------------------------------------------

    private void checkFalls() {
        int threshold = platform.getMinY() - config.getFallDistance();
        List<Player> fallen = new ArrayList<>();
        forEachAliveOnline(player -> {
            Location location = player.getLocation();
            if (location.getWorld() != null
                    && location.getWorld().getName().equalsIgnoreCase(platform.getWorldName())
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
        player.setAllowFlight(true);
        player.setFlying(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        context.getMessages().send(player, "event.pixel-party.eliminated-info");
    }

    private void sendCounter(int remaining) {
        PaletteColor color = config.getPalette().get(targetColor);
        String format = phase == Phase.SHOWING ? config.getActionbarShowing() : config.getActionbarPause();
        TagResolver[] resolvers = {
                Placeholder.unparsed("round", String.valueOf(Math.max(1, round))),
                Placeholder.parsed("color", color.getColorTag()),
                Placeholder.unparsed("colorname", color.getName()),
                Placeholder.unparsed("time", TimeUtil.formatSeconds(Math.max(0, phaseSecondsLeft))),
                Placeholder.unparsed("total", TimeUtil.formatSeconds(Math.max(0, remaining))),
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
        return false;
    }

    @Override
    public boolean allowDamage(Player player) {
        return false;
    }
}
