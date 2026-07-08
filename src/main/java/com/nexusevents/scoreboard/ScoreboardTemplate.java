package com.nexusevents.scoreboard;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

/**
 * Plantilla inmutable de scoreboard definida en {@code scoreboards.yml}.
 *
 * <p>Contiene el titulo, las lineas (con placeholders dinamicos que se
 * resuelven al renderizar) y el intervalo de actualizacion en ticks.
 * El renderizado se realiza con FastBoard desde los modulos de eventos.</p>
 */
public final class ScoreboardTemplate {

    private final String key;
    private final String title;
    private final List<String> lines;
    private final int updateIntervalTicks;

    private ScoreboardTemplate(String key, String title, List<String> lines, int updateIntervalTicks) {
        this.key = key;
        this.title = title;
        this.lines = Collections.unmodifiableList(lines);
        this.updateIntervalTicks = updateIntervalTicks;
    }

    /**
     * Parsea una plantilla desde su seccion YAML.
     *
     * @param key             identificador de la plantilla.
     * @param section         seccion de configuracion de la plantilla.
     * @param defaultInterval intervalo de actualizacion por defecto en ticks.
     * @return plantilla inmutable.
     */
    public static ScoreboardTemplate parse(String key, ConfigurationSection section, int defaultInterval) {
        String title = section.getString("title", key);
        List<String> lines = section.getStringList("lines");
        int interval = section.getInt("update-interval-ticks", defaultInterval);
        return new ScoreboardTemplate(key, title, lines, Math.max(1, interval));
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLines() {
        return lines;
    }

    public int getUpdateIntervalTicks() {
        return updateIntervalTicks;
    }
}
