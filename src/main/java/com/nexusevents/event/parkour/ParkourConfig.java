package com.nexusevents.event.parkour;

import com.nexusevents.configuration.model.TitleConfig;
import com.nexusevents.util.TimeUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuracion tipada del Parkour por islas, parseada desde
 * {@code events/parkour.yml}.
 *
 * <p>Controla la ventaja inicial, la desintegracion progresiva de cada
 * isla (bloques por paso, intervalo entre pasos, pausa entre islas y
 * aceleracion por isla) y los textos del evento.</p>
 */
public final class ParkourConfig {

    private final int startDelaySeconds;
    private final int maxDurationSeconds;
    private final int fallDistance;
    private final boolean winWhenOneRemains;

    private final int blocksPerStep;
    private final long stepIntervalTicks;
    private final long islandDelayTicks;
    private final double islandSpeedMultiplier;
    private final Map<Integer, Long> islandDelayOverrides;
    private final boolean announceIslands;
    private final boolean breakEffect;

    private final boolean collapseSoundEnabled;
    private final int collapseSoundRadius;

    private final TitleConfig warningTitle;
    private final TitleConfig startTitle;
    private final String actionbarWaiting;
    private final String actionbarCollapsing;
    private final String actionbarPause;

    private ParkourConfig(int startDelaySeconds, int maxDurationSeconds, int fallDistance,
                          boolean winWhenOneRemains, int blocksPerStep, long stepIntervalTicks,
                          long islandDelayTicks, double islandSpeedMultiplier,
                          Map<Integer, Long> islandDelayOverrides, boolean announceIslands,
                          boolean breakEffect,
                          boolean collapseSoundEnabled, int collapseSoundRadius,
                          TitleConfig warningTitle, TitleConfig startTitle,
                          String actionbarWaiting, String actionbarCollapsing,
                          String actionbarPause) {
        this.startDelaySeconds = startDelaySeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.fallDistance = fallDistance;
        this.winWhenOneRemains = winWhenOneRemains;
        this.blocksPerStep = blocksPerStep;
        this.stepIntervalTicks = stepIntervalTicks;
        this.islandDelayTicks = islandDelayTicks;
        this.islandSpeedMultiplier = islandSpeedMultiplier;
        this.islandDelayOverrides = Collections.unmodifiableMap(islandDelayOverrides);
        this.announceIslands = announceIslands;
        this.breakEffect = breakEffect;
        this.collapseSoundEnabled = collapseSoundEnabled;
        this.collapseSoundRadius = collapseSoundRadius;
        this.warningTitle = warningTitle;
        this.startTitle = startTitle;
        this.actionbarWaiting = actionbarWaiting;
        this.actionbarCollapsing = actionbarCollapsing;
        this.actionbarPause = actionbarPause;
    }

    /**
     * Parsea la configuracion completa del evento.
     *
     * @param file   archivo events/parkour.yml.
     * @param logger logger para valores invalidos.
     * @return configuracion validada.
     */
    public static ParkourConfig parse(FileConfiguration file, Logger logger) {
        Map<Integer, Long> overrides = parseIslandDelays(file, logger);
        double multiplier = file.getDouble("collapse.island-speed-multiplier", 0.9);
        if (multiplier < 0.2 || multiplier > 2.0) {
            logger.warning("parkour: island-speed-multiplier fuera de rango (0.2 - 2.0). Se usa 0.9.");
            multiplier = 0.9;
        }
        return new ParkourConfig(
                TimeUtil.parseSeconds(file.getString("settings.start-delay", ""), 10),
                TimeUtil.parseSeconds(file.getString("settings.max-duration", ""), 300),
                Math.max(1, file.getInt("settings.fall-distance", 4)),
                file.getBoolean("settings.win-when-one-remains", true),
                Math.max(1, file.getInt("collapse.blocks-per-step", 2)),
                Math.max(1L, TimeUtil.parseTicks(file.getString("collapse.step-interval", ""), 4L)),
                Math.max(0L, TimeUtil.parseTicks(file.getString("collapse.island-delay", ""), 40L)),
                multiplier,
                overrides,
                file.getBoolean("collapse.announce-island-collapse", false),
                file.getBoolean("collapse.break-effect", true),
                file.getBoolean("sound.enabled", true),
                Math.max(4, file.getInt("sound.radius", 24)),
                titleOr(file, "titles.warning", "<yellow><bold><seconds>",
                        "<gray>El recorrido empieza a desintegrarse..."),
                titleOr(file, "titles.start", "<red><bold>¡EL RECORRIDO COLAPSA!",
                        "<gray>Las islas caen en orden: ¡corré!"),
                file.getString("actionbar.waiting",
                        "<gray>El colapso empieza en <yellow><time> <dark_gray>| <gray>Vivos: <green><alive>"),
                file.getString("actionbar.collapsing",
                        "<gray>Isla: <red><island> <dark_gray>| <gray>Vivos: <green><alive> <dark_gray>| <gray>Tiempo: <yellow><time>"),
                file.getString("actionbar.pause",
                        "<gray>Isla <yellow>#<next></yellow> en <yellow><time> <dark_gray>| <gray>Vivos: <green><alive>")
        );
    }

    /**
     * Parsea las pausas especiales por isla: numero de la isla que
     * acaba de caer, con la espera antes de la siguiente.
     */
    private static Map<Integer, Long> parseIslandDelays(FileConfiguration file, Logger logger) {
        Map<Integer, Long> overrides = new LinkedHashMap<>();
        ConfigurationSection section = file.getConfigurationSection("collapse.island-delays");
        if (section == null) {
            return overrides;
        }
        for (String key : section.getKeys(false)) {
            try {
                int island = Integer.parseInt(key.trim());
                long ticks = TimeUtil.parseTicks(section.getString(key, ""), 0L);
                if (island >= 1 && ticks > 0) {
                    overrides.put(island, ticks);
                } else {
                    logger.warning("parkour: island-delays." + key + " invalido: se ignora.");
                }
            } catch (NumberFormatException exception) {
                logger.warning("parkour: island-delays." + key + " no es un numero de isla: se ignora.");
            }
        }
        return overrides;
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

    public int getFallDistance() {
        return fallDistance;
    }

    public boolean isWinWhenOneRemains() {
        return winWhenOneRemains;
    }

    public int getBlocksPerStep() {
        return blocksPerStep;
    }

    public long getStepIntervalTicks() {
        return stepIntervalTicks;
    }

    public long getIslandDelayTicks() {
        return islandDelayTicks;
    }

    /**
     * Espera tras caer la isla indicada: la pausa especial configurada
     * para ese numero, o la pausa por defecto.
     *
     * @param islandNumber numero (desde 1) de la isla que cayo.
     * @return espera en ticks antes de la siguiente isla.
     */
    public long delayAfterIsland(int islandNumber) {
        Long override = islandDelayOverrides.get(islandNumber);
        return override != null ? override : islandDelayTicks;
    }

    /**
     * Indica si la isla tiene una pausa especial configurada.
     *
     * @param islandNumber numero (desde 1) de la isla que cayo.
     * @return true si hay override.
     */
    public boolean hasDelayOverride(int islandNumber) {
        return islandDelayOverrides.containsKey(islandNumber);
    }

    /**
     * Indica si se anuncia por chat cada isla desintegrada.
     * Desactivado por defecto para no llenar el chat: la actionbar y
     * el scoreboard ya muestran el avance.
     *
     * @return true si el anuncio por isla esta activado.
     */
    public boolean isAnnounceIslands() {
        return announceIslands;
    }

    /**
     * Animacion de rotura (particulas + sonido del bloque) por cada
     * bloque desintegrado.
     *
     * @return true si esta activada.
     */
    public boolean isBreakEffect() {
        return breakEffect;
    }

    /**
     * Multiplicador aplicado al intervalo de pasos en cada isla
     * sucesiva: menor a 1 significa que cada isla cae mas rapido.
     *
     * @return multiplicador por isla.
     */
    public double getIslandSpeedMultiplier() {
        return islandSpeedMultiplier;
    }

    public boolean isCollapseSoundEnabled() {
        return collapseSoundEnabled;
    }

    public int getCollapseSoundRadius() {
        return collapseSoundRadius;
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

    public String getActionbarCollapsing() {
        return actionbarCollapsing;
    }

    public String getActionbarPause() {
        return actionbarPause;
    }
}
