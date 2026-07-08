package com.nexusevents.event;

import com.nexusevents.configuration.model.TitleConfig;
import com.nexusevents.util.TimeUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Ajustes globales del sistema de eventos, parseados desde la seccion
 * {@code events} de {@code config.yml}.
 *
 * <p>Los eventos concretos pueden sobreescribir valores puntuales (por
 * ejemplo su duracion maxima) desde sus propios archivos en las fases
 * siguientes; estos son los valores por defecto validados.</p>
 */
public final class EventSettings {

    private static final String SECTION = "events";

    private final int minPlayers;
    private final int maxPlayers;
    private final int lobbyTimeoutSeconds;
    private final int countdownSeconds;
    private final int maxDurationSeconds;
    private final TitleConfig countdownTitle;

    private EventSettings(int minPlayers, int maxPlayers, int lobbyTimeoutSeconds,
                          int countdownSeconds, int maxDurationSeconds, TitleConfig countdownTitle) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.lobbyTimeoutSeconds = lobbyTimeoutSeconds;
        this.countdownSeconds = countdownSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.countdownTitle = countdownTitle;
    }

    /**
     * Parsea y valida los ajustes desde la configuracion principal.
     *
     * @param config configuracion principal del plugin.
     * @param logger logger para avisos de configuracion faltante.
     * @return ajustes validados (con valores por defecto si faltan).
     */
    public static EventSettings parse(FileConfiguration config, Logger logger) {
        ConfigurationSection section = config.getConfigurationSection(SECTION);
        if (section == null) {
            logger.warning("config.yml no contiene la seccion '" + SECTION + "': se usan valores por defecto.");
            return new EventSettings(2, 40, 120, 15, 600, defaultCountdownTitle());
        }
        int minPlayers = Math.max(1, section.getInt("min-players", 2));
        int maxPlayers = Math.max(minPlayers, section.getInt("max-players", 40));

        TitleConfig title = TitleConfig.parse(section.getConfigurationSection("countdown-title"));
        if (!title.isEnabled()) {
            title = defaultCountdownTitle();
        }
        return new EventSettings(
                minPlayers,
                maxPlayers,
                seconds(section, "lobby-timeout", 120),
                seconds(section, "countdown", 15),
                seconds(section, "max-duration", 600),
                title
        );
    }

    private static int seconds(ConfigurationSection section, String path, int fallbackSeconds) {
        long ticks = TimeUtil.parseTicks(section.getString(path, ""), fallbackSeconds * TimeUtil.TICKS_PER_SECOND);
        return Math.max(1, TimeUtil.ticksToSeconds(ticks));
    }

    private static TitleConfig defaultCountdownTitle() {
        return TitleConfig.of("<yellow><seconds>", "<gray>El evento está por comenzar", 0, 25, 5);
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getLobbyTimeoutSeconds() {
        return lobbyTimeoutSeconds;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public TitleConfig getCountdownTitle() {
        return countdownTitle;
    }
}
