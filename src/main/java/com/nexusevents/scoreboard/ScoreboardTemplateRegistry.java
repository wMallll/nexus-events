package com.nexusevents.scoreboard;

import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registro central de plantillas de scoreboard.
 *
 * <p>Carga todas las plantillas definidas en {@code scoreboards.yml},
 * aplicando el intervalo de actualizacion global como valor por defecto.
 * Los modulos de eventos consultan sus plantillas por clave.</p>
 */
public final class ScoreboardTemplateRegistry implements Manager, Reloadable {

    private static final String SETTINGS_INTERVAL_PATH = "settings.update-interval-ticks";
    private static final String ROOT_SECTION = "scoreboards";
    private static final int DEFAULT_INTERVAL_TICKS = 10;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private final Map<String, ScoreboardTemplate> templates = new LinkedHashMap<>();

    public ScoreboardTemplateRegistry(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "Scoreboards";
    }

    @Override
    public void enable() {
        loadTemplates();
    }

    @Override
    public void disable() {
        templates.clear();
    }

    @Override
    public void reload() {
        loadTemplates();
    }

    private void loadTemplates() {
        templates.clear();

        FileConfiguration file = configManager.getFile(ConfigManager.SCOREBOARDS_FILE).get();
        int defaultInterval = Math.max(1, file.getInt(SETTINGS_INTERVAL_PATH, DEFAULT_INTERVAL_TICKS));

        ConfigurationSection root = file.getConfigurationSection(ROOT_SECTION);
        if (root == null) {
            plugin.getLogger().warning("scoreboards.yml no contiene la seccion '" + ROOT_SECTION + "'.");
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                plugin.getLogger().warning("La plantilla de scoreboard '" + key + "' es invalida: se ignora.");
                continue;
            }
            templates.put(key, ScoreboardTemplate.parse(key, section, defaultInterval));
        }
        plugin.getLogger().info("Cargadas " + templates.size() + " plantillas de scoreboard.");
    }

    /**
     * Obtiene la plantilla asociada a la clave dada.
     *
     * @param key clave de la plantilla.
     * @return plantilla, si existe.
     */
    public Optional<ScoreboardTemplate> get(String key) {
        return Optional.ofNullable(templates.get(key));
    }

    /**
     * Devuelve las claves de todas las plantillas cargadas.
     *
     * @return claves registradas.
     */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(templates.keySet());
    }
}
