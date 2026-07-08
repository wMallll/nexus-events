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
 * <p>El tipo de particula del borde se parsea contra la version del
 * servidor: si el nombre configurado no existe en esa version, el borde
 * visual se desactiva con warning sin romper el evento.</p>
 */
public final class CircleConfig {

    private final int startDelaySeconds;
    private final int maxDurationSeconds;
    private final double initialRadius;
    private final double minRadius;
    private final double shrinkPerSecond;
    private final double damagePerSecond;
    private final Particle borderParticle;
    private final int particlePoints;
    private final int particleRings;
    private final TitleConfig warningTitle;
    private final TitleConfig shrinkTitle;
    private final String actionbarInside;
    private final String actionbarOutside;

    private CircleConfig(int startDelaySeconds, int maxDurationSeconds, double initialRadius,
                         double minRadius, double shrinkPerSecond, double damagePerSecond,
                         Particle borderParticle, int particlePoints, int particleRings,
                         TitleConfig warningTitle, TitleConfig shrinkTitle,
                         String actionbarInside, String actionbarOutside) {
        this.startDelaySeconds = startDelaySeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.initialRadius = initialRadius;
        this.minRadius = minRadius;
        this.shrinkPerSecond = shrinkPerSecond;
        this.damagePerSecond = damagePerSecond;
        this.borderParticle = borderParticle;
        this.particlePoints = particlePoints;
        this.particleRings = particleRings;
        this.warningTitle = warningTitle;
        this.shrinkTitle = shrinkTitle;
        this.actionbarInside = actionbarInside;
        this.actionbarOutside = actionbarOutside;
    }

    /**
     * Parsea la configuracion completa del evento.
     *
     * @param file   archivo events/circle.yml.
     * @param logger logger para valores invalidos.
     * @return configuracion validada.
     */
    public static CircleConfig parse(FileConfiguration file, Logger logger) {
        double initialRadius = Math.max(3.0, file.getDouble("settings.initial-radius", 30.0));
        double minRadius = Math.max(1.0, Math.min(initialRadius,
                file.getDouble("settings.min-radius", 3.0)));
        return new CircleConfig(
                TimeUtil.parseSeconds(file.getString("settings.start-delay", ""), 10),
                TimeUtil.parseSeconds(file.getString("settings.max-duration", ""), 300),
                initialRadius,
                minRadius,
                Math.max(0.01, file.getDouble("settings.shrink-per-second", 0.25)),
                Math.max(0.0, file.getDouble("settings.damage-per-second", 2.0)),
                parseParticle(file, logger),
                Math.max(8, file.getInt("particles.points", 60)),
                Math.max(1, file.getInt("particles.rings", 2)),
                titleOr(file, "titles.warning", "<yellow><bold><seconds>", "<gray>El círculo empieza a cerrarse..."),
                titleOr(file, "titles.shrink-start", "<red><bold>¡EL CÍRCULO SE CIERRA!", "<gray>Mantenete adentro"),
                file.getString("actionbar.inside",
                        "<gray>Radio: <yellow><radius> <dark_gray>| <gray>Vivos: <green><alive> <dark_gray>| <gray>Tiempo: <yellow><time>"),
                file.getString("actionbar.outside",
                        "<red><bold>¡ESTÁS FUERA DEL CÍRCULO!</bold> <gray>Radio: <yellow><radius>")
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

    public double getInitialRadius() {
        return initialRadius;
    }

    public double getMinRadius() {
        return minRadius;
    }

    public double getShrinkPerSecond() {
        return shrinkPerSecond;
    }

    public double getDamagePerSecond() {
        return damagePerSecond;
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

    public TitleConfig getShrinkTitle() {
        return shrinkTitle;
    }

    public String getActionbarInside() {
        return actionbarInside;
    }

    public String getActionbarOutside() {
        return actionbarOutside;
    }
}
