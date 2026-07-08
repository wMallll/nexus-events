package com.nexusevents.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Representa un unico archivo YAML del plugin.
 *
 * <p>Si el archivo no existe en la carpeta de datos, se copia el recurso
 * embebido en el jar (si existe) o se crea vacio. Es la pieza reutilizable
 * sobre la que se apoyan todos los archivos del plugin: configuracion,
 * mensajes y, en fases futuras, arenas y eventos.</p>
 */
public final class ConfigurationFile {

    private final JavaPlugin plugin;
    private final String fileName;
    private final File file;

    private FileConfiguration configuration;

    public ConfigurationFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    /**
     * Carga (o recarga) el archivo desde disco, creandolo previamente
     * si no existe.
     */
    public void load() {
        if (!file.exists()) {
            createDefaults();
        }
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    private void createDefaults() {
        if (plugin.getResource(fileName) != null) {
            plugin.saveResource(fileName, false);
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("No se pudo crear el directorio para " + fileName + ".");
            }
            if (!file.createNewFile()) {
                plugin.getLogger().warning("No se pudo crear el archivo " + fileName + ".");
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Error creando el archivo " + fileName + ".", exception);
        }
    }

    /**
     * Persiste en disco el estado actual de la configuracion en memoria.
     */
    public void save() {
        if (configuration == null) {
            plugin.getLogger().warning("Se intento guardar " + fileName + " antes de cargarlo.");
            return;
        }
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando el archivo " + fileName + ".", exception);
        }
    }

    public void reload() {
        load();
    }

    /**
     * Devuelve la configuracion en memoria, cargandola si aun no lo esta.
     *
     * @return configuracion YAML del archivo.
     */
    public FileConfiguration get() {
        if (configuration == null) {
            load();
        }
        return configuration;
    }

    public String getFileName() {
        return fileName;
    }

    public File getFile() {
        return file;
    }
}
