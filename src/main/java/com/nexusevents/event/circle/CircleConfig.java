package com.nexusevents.event.circle;

import com.nexusevents.configuration.model.TitleConfig;
import com.nexusevents.util.TimeUtil;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Configuracion tipada de "El Circulo", parseada desde
 * {@code events/circle.yml}.
 *
 * <p>Controla el escaneo del piso (radio por defecto y capas
 * verticales), el rompedor aleatorio de bloques (retardos y cantidades
 * minimas/maximas con aceleracion opcional), las bolas de nieve
 * iniciales y el borde visual de particulas.</p>
 */
public final class CircleConfig {

    private final int startDelaySeconds;
    private final int maxDurationSeconds;
    private final int defaultRadius;
    private final int fallDistance;
    private final int scanDown;
    private final int scanUp;

    private final boolean snowballsEnabled;
    private final int snowballStacks;
    private final int snowballPerStack;
    private final double knockbackHorizontal;
    private final double knockbackVertical;

    private final long breakMinDelayTicks;
    private final long breakMaxDelayTicks;
    private final int breakMinBlocks;
    private final int breakMaxBlocks;
    private final int speedUpEverySeconds;
    private final int maxBlocksCap;
    private final boolean breakEffect;

    private final Particle borderParticle;
    private final int particlePoints;
    private final int particleRings;

    private final TitleConfig warningTitle;
    private final TitleConfig startTitle;
    private final String actionbarWaiting;
    private final String actionbarPlaying;

    private CircleConfig(int startDelaySeconds, int maxDurationSeconds, int defaultRadius,
                         int fallDistance, int scanDown, int scanUp,
                         boolean snowballsEnabled, int snowballStacks, int snowballPerStack,
                         double knockbackHorizontal, double knockbackVertical,
                         long breakMinDelayTicks, long breakMaxDelayTicks,
                         int breakMinBlocks, int breakMaxBlocks,
                         int speedUpEverySeconds, int maxBlocksCap, boolean breakEffect,
                         Particle borderParticle, int particlePoints, int particleRings,
                         TitleConfig warningTitle, TitleConfig startTitle,
                         String actionbarWaiting, String actionbarPlaying) {
        this.startDelaySeconds = startDelaySeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.defaultRadius = defaultRadius;
        this.fallDistance = fallDistance;
        this.scanDown = scanDown;
        this.scanUp = scanUp;
        this.snowballsEnabled = snowballsEnabled;
        this.snowballStacks = snowballStacks;
        this.snowballPerStack = snowballPerStack;
        this.knockbackHorizontal = knockbackHorizontal;
        this.knockbackVertical = knockbackVertical;
        this.breakMinDelayTicks = breakMinDelayTicks;
        this.breakMaxDelayTicks = breakMaxDelayTicks;
        this.breakMinBlocks = breakMinBlocks;
        this.breakMaxBlocks = breakMaxBlocks;
        this.speedUpEverySeconds = speedUpEverySeconds;
        this.maxBlocksCap = maxBlocksCap;
        this.breakEffect = breakEffect;
        this.borderParticle = borderParticle;
        this.particlePoints = particlePoints;
        this.particleRings = particleRings;
        this.warningTitle = warningTitle;
        this.startTitle = startTitle;
        this.actionbarWaiting = actionbarWaiting;
        this.actionbarPlaying = actionbarPlaying;
    }

    /**
     * Parsea la configuracion completa del evento.
     *
     * @param file   archivo events/circle.yml.
     * @param logger logger para valores invalidos.
     * @return configuracion validada.
     */
    public static CircleConfig parse(FileConfiguration file, Logger logger) {
        int minBlocks = Math.max(1, file.getInt("break.min-blocks", 5));
        int maxBlocks = Math.max(minBlocks, file.getInt("break.max-blocks", 7));
        long minDelay = Math.max(1L, TimeUtil.parseTicks(file.getString("break.min-delay", ""), 5L));
        long maxDelay = Math.max(minDelay, TimeUtil.parseTicks(file.getString("break.max-delay", ""), 40L));
        return new CircleConfig(
                TimeUtil.parseSeconds(file.getString("settings.start-delay", ""), 5),
                TimeUtil.parseSeconds(file.getString("settings.max-duration", ""), 300),
                Math.max(3, file.getInt("settings.default-radius", 30)),
                Math.max(1, file.getInt("settings.fall-distance", 5)),
                Math.max(0, file.getInt("settings.vertical-scan-down", 5)),
                Math.max(0, file.getInt("settings.vertical-scan-up", 0)),
                file.getBoolean("snowballs.enabled", true),
                Math.max(0, file.getInt("snowballs.stacks", 5)),
                Math.max(1, file.getInt("snowballs.per-stack", 16)),
                file.getDouble("knockback.horizontal", 0.9),
                file.getDouble("knockback.vertical", 0.35),
                minDelay,
                maxDelay,
                minBlocks,
                maxBlocks,
                TimeUtil.parseSeconds(file.getString("break.speed-up-every", ""), 20),
                Math.max(maxBlocks, file.getInt("break.max-blocks-cap", 12)),
                file.getBoolean("break.effect", true),
                parseParticle(file, logger),
                Math.max(8, file.getInt("particles.points", 60)),
                Math.max(1, file.getInt("particles.rings", 2)),
                titleOr(file, "titles.warning", "<yellow><bold><seconds>",
                        "<gray>Los bloques empiezan a romperse..."),
                titleOr(file, "titles.start", "<red><bold>¡PVP ACTIVADO!",
                        "<gray>Que no te tiren al vacío"),
                file.getString("actionbar.waiting",
                        "<gray>Los bloques se rompen en <yellow><time> <dark_gray>| <gray>Vivos: <green><alive>"),
                file.getString("actionbar.playing",
                        "<gray>Piso roto: <red><broken>% <dark_gray>| <gray>Vivos: <green><alive> <dark_gray>| <gray>Tiempo: <yellow><time>")
        );
    }

    private static Particle parseParticle(FileConfiguration file, Logger logger) {
        if (!file.getBoolean("particles.enabled", true)) {
            return null;
        }
        String name = file.getString("particles.type", "FLAME").trim().toUpperCase(Locale.ROOT);
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException exception) {
            logger.warning("circle: la particula '" + name
                    + "' no existe en esta version del servidor. Se desactiva el borde visual.");
            return null;
        }
    }

    private static TitleConfig titleOr(FileConfiguration file, String path,
                                       String fallbackTitle, String fallbackSubtitle) {
        ConfigurationSection section = file.getConfigurationSection(path);
        TitleConfig parsed = TitleConfig.parse(section);
        if (section == null || !parsed.isEnabled()) {
            return TitleConfig.of(fallbackTitle, fallbackSubtitle, 0, 25, 5);
        }
        return parsed;
    }

    public int getStartDelaySeconds() {
        return startDelaySeconds;
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public int getDefaultRadius() {
        return defaultRadius;
    }

    public int getFallDistance() {
        return fallDistance;
    }

    public int getScanDown() {
        return scanDown;
    }

    public int getScanUp() {
        return scanUp;
    }

    public boolean isSnowballsEnabled() {
        return snowballsEnabled;
    }

    public int getSnowballStacks() {
        return snowballStacks;
    }

    public int getSnowballPerStack() {
        return snowballPerStack;
    }

    /**
     * Fuerza horizontal del empuje de la bola de nieve.
     *
     * @return multiplicador horizontal.
     */
    public double getKnockbackHorizontal() {
        return knockbackHorizontal;
    }

    /**
     * Impulso vertical del empuje de la bola de nieve.
     *
     * @return componente vertical.
     */
    public double getKnockbackVertical() {
        return knockbackVertical;
    }

    public long getBreakMinDelayTicks() {
        return breakMinDelayTicks;
    }

    public long getBreakMaxDelayTicks() {
        return breakMaxDelayTicks;
    }

    public int getBreakMinBlocks() {
        return breakMinBlocks;
    }

    public int getBreakMaxBlocks() {
        return breakMaxBlocks;
    }

    public int getSpeedUpEverySeconds() {
        return speedUpEverySeconds;
    }

    public int getMaxBlocksCap() {
        return maxBlocksCap;
    }

    /**
     * Animacion de rotura (particulas + sonido del bloque) por cada
     * bloque del piso que se rompe.
     *
     * @return true si esta activada.
     */
    public boolean isBreakEffect() {
        return breakEffect;
    }

    /**
     * Particula del borde, o null si esta desactivada o no existe en
     * la version del servidor.
     *
     * @return particula del borde o null.
     */
    public Particle getBorderParticle() {
        return borderParticle;
    }

    public int getParticlePoints() {
        return particlePoints;
    }

    public int getParticleRings() {
        return particleRings;
    }

    public TitleConfig getWarningTitle() {
        return warningTitle;
    }

    public TitleConfig getStartTitle() {
        return startTitle;
    }

    public String getActionbarWaiting() {
        return actionbarWaiting;
    }

    public String getActionbarPlaying() {
        return actionbarPlaying;
    }
}
