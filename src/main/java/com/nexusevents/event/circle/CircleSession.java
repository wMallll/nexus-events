package com.nexusevents.event.circle;

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
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Partida de "El Circulo que se cierra".
 *
 * <p>Todos aparecen dentro de un circulo centrado en el punto
 * {@code circle-center}; tras el retraso inicial, el radio se reduce de
 * forma continua a la velocidad configurada hasta un minimo. Quien
 * queda fuera recibe dano configurable por segundo, pero nunca muere:
 * cuando el dano lo mataria, el jugador es eliminado directamente (sin
 * drops ni pantalla de respawn). El borde se dibuja con anillos de
 * particulas y el ultimo jugador vivo gana.</p>
 */
public final class CircleSession extends EventSession {

    private final CircleConfig config;

    private Location center;
    private double radius;
    private boolean shrinking;

    public CircleSession(EventContext context, GameEvent type, Arena arena, CircleConfig config) {
        super(context, type, arena);
        this.config = config;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Override
    protected void onStart() {
        this.center = resolvePoint(ArenaKeys.CIRCLE_CENTER);
        if (center == null || center.getWorld() == null) {
            context.getPlugin().getLogger().severe("circle: el centro del circulo no esta disponible. Se cancela.");
            end(EventEndReason.CANCELLED);
            return;
        }
        this.radius = config.getInitialRadius();
        this.shrinking = false;
    }

    @Override
    protected void onSecond(int elapsed, int remaining) {
        if (center == null) {
            return;
        }
        if (!shrinking) {
            tickWaiting(elapsed);
        } else {
            radius = Math.max(config.getMinRadius(), radius - config.getShrinkPerSecond());
        }
        drawBorder();
        punishOutside();
        sendCounter(remaining);
    }

    private void tickWaiting(int elapsed) {
        int secondsToShrink = config.getStartDelaySeconds() - elapsed;
        if (secondsToShrink > 0 && secondsToShrink <= 3) {
            TagResolver seconds = Placeholder.unparsed("seconds", String.valueOf(secondsToShrink));
            forEachAliveOnline(player -> {
                context.getTitles().showTitle(player, config.getWarningTitle(), seconds);
                context.getSounds().play(player, "countdown-tick");
            });
        }
        if (elapsed >= config.getStartDelaySeconds()) {
            beginShrink();
        }
    }

    private void beginShrink() {
        shrinking = true;
        broadcast("event.circle.shrink-start");
        forEachParticipantOnline(player -> {
            context.getTitles().showTitle(player, config.getShrinkTitle());
            context.getSounds().play(player, "event-start");
        });
    }

    @Override
    protected void onEnd(EventEndReason reason) {
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
            context.getMessages().broadcast("event.circle.winners",
                    Placeholder.unparsed("winners", winners.toString()));
        }
    }

    // ------------------------------------------------------------------
    // Borde y castigo de zona
    // ------------------------------------------------------------------

    /**
     * Dibuja el borde del circulo con anillos de particulas, si el tipo
     * configurado existe en esta version del servidor.
     */
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

    /**
     * Aplica el dano de zona a los jugadores fuera del circulo. Si el
     * dano los mataria, se los elimina en lugar de dejarlos morir.
     */
    private void punishOutside() {
        if (config.getDamagePerSecond() <= 0) {
            return;
        }
        List<Player> toEliminate = new ArrayList<>();
        forEachAliveOnline(player -> {
            if (isInside(player)) {
                return;
            }
            if (player.getHealth() - config.getDamagePerSecond() <= 0.5) {
                toEliminate.add(player);
            } else {
                player.damage(config.getDamagePerSecond());
            }
        });
        for (Player player : toEliminate) {
            eliminate(player);
        }
    }

    private boolean isInside(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null || !world.equals(center.getWorld())) {
            return false;
        }
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

    @Override
    protected void onPlayerEliminated(Player player) {
        player.setHealth(player.getMaxHealth());
        player.teleport(center);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        context.getMessages().send(player, "event.circle.eliminated-info");
    }

    private void sendCounter(int remaining) {
        String radiusText = String.format(Locale.ROOT, "%.1f", radius);
        String time = TimeUtil.formatSeconds(Math.max(0, remaining));
        String alive = String.valueOf(getAliveCount());
        forEachParticipantOnline(player -> {
            boolean outside = isAlive(player.getUniqueId()) && !isInside(player);
            String format = outside ? config.getActionbarOutside() : config.getActionbarInside();
            context.getTitles().sendActionBar(player, format,
                    Placeholder.unparsed("radius", radiusText),
                    Placeholder.unparsed("alive", alive),
                    Placeholder.unparsed("time", time));
        });
    }

    // ------------------------------------------------------------------
    // Reglas
    // ------------------------------------------------------------------

    @Override
    protected int getMaxDurationSeconds() {
        return config.getMaxDurationSeconds();
    }

    /**
     * Solo se permite el dano de zona del propio circulo (causa
     * CUSTOM); PvP, caidas y cualquier otra causa quedan bloqueados.
     */
    @Override
    public boolean allowDamage(Player player, EntityDamageEvent event) {
        return getState() == EventState.RUNNING
                && event.getCause() == EntityDamageEvent.DamageCause.CUSTOM;
    }
}
