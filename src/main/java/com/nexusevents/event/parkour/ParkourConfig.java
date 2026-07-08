package com.nexusevents.event.parkour;

import com.nexusevents.configuration.model.TitleConfig;
import com.nexusevents.util.TimeUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Configuracion tipada del Parkour Colapsable, parseada desde
 * {@code events/parkour.yml}.
 *
 * <p>Controla el retraso inicial, la velocidad del colapso (intervalo,
 * bloques por paso y aceleracion progresiva con tope), la eliminacion
 * por caida y los efectos.</p>
 */
public final class ParkourConfig {

    private final int startDelaySeconds;
    private final long intervalTicks;
    private final int blocksPerStepStart;
    private final int speedUpEverySeconds;
    private final int speedUpAmount;
    private final int maxBlocksPerStep;
    private final int fallDistance;
    private final int maxDurationSeconds;
    private final boolean winWhenOneRemains;
    private final boolean collapseSoundEnabled;
    private final int collapseSoundRadius;
    private final TitleConfig warningTitle;
    private final TitleConfig startTitle;
    private final String actionbarWaiting;
    private final String actionbarCollapsing;

    private ParkourConfig(int startDelaySeconds, long intervalTicks, int blocksPerStepStart,
                          int speedUpEverySeconds, int speedUpAmount, int maxBlocksPerStep,
                          int fallDistance, int maxDurationSeconds, boolean winWhenOneRemains,
                          boolean collapseSoundEnabled, int collapseSoundRadius,
                          TitleConfig warningTitle, TitleConfig startTitle,
                          String actionbarWaiting, String actionbarCollapsing) {
        this.startDelaySeconds = startDelaySeconds;
        this.intervalTicks = intervalTicks;
        this.blocksPerStepStart = blocksPerStepStart;
        this.speedUpEverySeconds = speedUpEverySeconds;
        this.speedUpAmount = speedUpAmount;
        this.maxBlocksPerStep = maxBlocksPerStep;
        this.fallDistance = fallDistance;
        this.maxDurationSeconds = maxDurationSeconds;
        this.winWhenOneRemains = winWhenOneRemains;
        this.collapseSoundEnabled = collapseSoundEnabled;
        this.collapseSoundRadius = collapseSoundRadius;
        this.warningTitle = warningTitle;
        this.startTitle = startTitle;
        this.actionbarWaiting = actionbarWaiting;
        this.actionbarCollapsing = actionbarCollapsing;
    }

    /**
     * Parsea la configuracion completa del evento.
     *
     * @param file   archivo events/parkour.yml.
     * @param logger logger para valores invalidos.
     * @return configuracion validada.
     */
    public static ParkourConfig parse(FileConfiguration file, Logger logger) {
        int blocksPerStep = Math.max(1, file.getInt("collapse.blocks-per-step", 4));
        return new ParkourConfig(
                TimeUtil.parseSeconds(file.getString("settings.start-delay", ""), 10),
                Math.max(1L, TimeUtil.parseTicks(file.getString("collapse.interval", ""), 5L)),
                blocksPerStep,
                TimeUtil.parseSeconds(file.getString("collapse.speed-up-every", ""), 15),
                Math.max(0, file.getInt("collapse.speed-up-amount", 2)),
                Math.max(blocksPerStep, file.getInt("collapse.max-blocks-per-step", 40)),
                Math.max(1, file.getInt("settings.fall-distance", 4)),
                TimeUtil.parseSeconds(file.getString("settings.max-duration", ""), 300),
                file.getBoolean("settings.win-when-one-remains", true),
                file.getBoolean("collapse.sound.enabled", true),
                Math.max(1, file.getInt("collapse.sound.radius", 24)),
                titleOr(file, "titles.warning", "<yellow><bold><seconds>", "<gray>El recorrido empieza a colapsar..."),
                titleOr(file, "titles.start", "<red><bold>¡COLAPSO!", "<gray>¡Corré por tu vida!"),
                file.getString("actionbar.waiting",
                        "<gray>El colapso comienza en <yellow><time> <dark_gray>| <gray>Vivos: <green><alive>"),
                file.getString("actionbar.collapsing",
                        "<gray>Colapsado: <red><progress>% <dark_gray>| <gray>Vivos: <green><alive> <dark_gray>| <gray>Tiempo: <yellow><time>")
        );
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

    public long getIntervalTicks() {
        return intervalTicks;
    }

    public int getBlocksPerStepStart() {
        return blocksPerStepStart;
    }

    public int getSpeedUpEverySeconds() {
        return speedUpEverySeconds;
    }

    public int getSpeedUpAmount() {
        return speedUpAmount;
    }

    public int getMaxBlocksPerStep() {
        return maxBlocksPerStep;
    }

    public int getFallDistance() {
        return fallDistance;
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public boolean isWinWhenOneRemains() {
        return winWhenOneRemains;
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
}
