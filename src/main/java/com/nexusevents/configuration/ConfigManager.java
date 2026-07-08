package com.nexusevents.configuration;

import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centraliza el acceso a todos los archivos YAML del plugin.
 *
 * <p>Los modulos de fases futuras registran sus propios archivos mediante
 * {@link #register(String)} sin necesidad de modificar esta clase
 * (principio abierto/cerrado).</p>
 */
public final class ConfigManager implements Manager, Reloadable {

    public static final String CONFIG_FILE = "config.yml";
    public static final String MESSAGES_FILE = "messages.yml";
    public static final String SOUNDS_FILE = "sounds.yml";
    public static final String SCOREBOARDS_FILE = "scoreboards.yml";

    private final JavaPlugin plugin;
    private final Map<String, ConfigurationFile> files = new LinkedHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Configuracion";
    }

    @Override
    public void enable() {
        register(CONFIG_FILE);
        register(MESSAGES_FILE);
        register(SOUNDS_FILE);
        register(SCOREBOARDS_FILE);
    }

    @Override
    public void disable() {
        files.clear();
    }

    @Override
    public void reload() {
        for (ConfigurationFile file : files.values()) {
            file.reload();
        }
    }

    /**
     * Registra y carga un archivo YAML administrado por el plugin.
     * Si ya estaba registrado, devuelve la instancia existente.
     *
     * @param fileName nombre del archivo relativo a la carpeta del plugin.
     * @return archivo de configuracion cargado.
     */
    public ConfigurationFile register(String fileName) {
        ConfigurationFile existing = files.get(fileName);
        if (existing != null) {
            return existing;
        }
        ConfigurationFile file = new ConfigurationFile(plugin, fileName);
        file.load();
        files.put(fileName, file);
        return file;
    }

    /**
     * Obtiene un archivo previamente registrado.
     *
     * @param fileName nombre del archivo.
     * @return archivo de configuracion.
     * @throws IllegalArgumentException si el archivo no fue registrado.
     */
    public ConfigurationFile getFile(String fileName) {
        ConfigurationFile file = files.get(fileName);
        if (file == null) {
            throw new IllegalArgumentException("El archivo no esta registrado: " + fileName);
        }
        return file;
    }

    public FileConfiguration getConfig() {
        return getFile(CONFIG_FILE).get();
    }

    public FileConfiguration getMessages() {
        return getFile(MESSAGES_FILE).get();
    }

    public boolean isDebug() {
        return getConfig().getBoolean("settings.debug", false);
    }
}
