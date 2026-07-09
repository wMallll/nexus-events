package com.nexusevents.event;

import com.nexusevents.configuration.model.TitleConfig;
import com.nexusevents.util.TimeUtil;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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
    private final boolean bossBarEnabled;
    private final String bossBarTitle;
    private final BarColor bossBarColor;
    private final BarStyle bossBarStyle;

    private EventSettings(int minPlayers, int maxPlayers, int lobbyTimeoutSeconds,
                          int countdownSeconds, int maxDurationSeconds, TitleConfig countdownTitle,
                          boolean bossBarEnabled, String bossBarTitle,
                          BarColor bossBarColor, BarStyle bossBarStyle) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.lobbyTimeoutSeconds = lobbyTimeoutSeconds;
        this.countdownSeconds = countdownSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.countdownTitle = countdownTitle;
        this.bossBarEnabled = bossBarEnabled;
        this.bossBarTitle = bossBarTitle;
        this.bossBarColor = bossBarColor;
        this.bossBarStyle = bossBarStyle;
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
            return new EventSettings(2, 40, 120, 15, 600, defaultCountdownTitle(),
                    true, defaultBossBarTitle(), BarColor.YELLOW, BarStyle.SOLID);
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
                title,
                section.getBoolean("bossbar.enabled", true),
                section.getString("bossbar.title", defaultBossBarTitle()),
                parseColor(section.getString("bossbar.color", "YELLOW"), logger),
                parseStyle(section.getString("bossbar.style", "SOLID"), logger)
        );
    }

    private static String defaultBossBarTitle() {
        return "<event> <dark_gray>| <gray>Tiempo: <yellow><time> <dark_gray>| <gray>Vivos: <green><alive>";
    }

    private static BarColor parseColor(String name, Logger logger) {
        try {
            return BarColor.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            logger.warning("Color de bossbar invalido '" + name + "': se usa YELLOW.");
            return BarColor.YELLOW;
        }
    }

    private static BarStyle parseStyle(String name, Logger logger) {
        try {
            return BarStyle.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            logger.warning("Estilo de bossbar invalido '" + name + "': se usa SOLID.");
            return BarStyle.SOLID;
        }
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

    public boolean isBossBarEnabled() {
        return bossBarEnabled;
    }

    public String getBossBarTitle() {
        return bossBarTitle;
    }

    public BarColor getBossBarColor() {
        return bossBarColor;
    }

    public BarStyle getBossBarStyle() {
        return bossBarStyle;
    }
}
