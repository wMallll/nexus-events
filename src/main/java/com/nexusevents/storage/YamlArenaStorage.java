package com.nexusevents.storage;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.Region;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Persistencia de arenas en YAML: un archivo por arena dentro de
 * {@code plugins/NexusEvents/arenas/}.
 *
 * <p>Las entradas corruptas se ignoran con warning sin interrumpir la
 * carga del resto de arenas.</p>
 */
public final class YamlArenaStorage implements ArenaStorage {

    private static final String DIRECTORY_NAME = "arenas";
    private static final String EXTENSION = ".yml";

    private final JavaPlugin plugin;
    private final File directory;

    public YamlArenaStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), DIRECTORY_NAME);
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("No se pudo crear el directorio de arenas: " + directory.getPath());
        }
    }

    @Override
    public List<Arena> loadAll() {
        List<Arena> arenas = new ArrayList<>();
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(EXTENSION));
        if (files == null) {
            return arenas;
        }
        for (File file : files) {
            arenas.add(loadArena(file));
        }
        return arenas;
    }

    private Arena loadArena(File file) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        String fallbackName = file.getName().substring(0, file.getName().length() - EXTENSION.length());
        Arena arena = new Arena(configuration.getString("name", fallbackName));

        ConfigurationSection points = configuration.getConfigurationSection("points");
        if (points != null) {
            for (String key : points.getKeys(false)) {
                try {
                    arena.setPoint(key, ArenaLocation.deserialize(points.getString(key, "")));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Punto '" + key + "' invalido en la arena '"
                            + arena.getName() + "': se ignora.");
                }
            }
        }

        ConfigurationSection regions = configuration.getConfigurationSection("regions");
        if (regions != null) {
            for (String key : regions.getKeys(false)) {
                try {
                    arena.setRegion(key, Region.deserialize(regions.getString(key, "")));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Region '" + key + "' invalida en la arena '"
                            + arena.getName() + "': se ignora.");
                }
            }
        }
        ConfigurationSection properties = configuration.getConfigurationSection("properties");
        if (properties != null) {
            for (String key : properties.getKeys(false)) {
                arena.setProperty(key, properties.getString(key, ""));
            }
        }
        return arena;
    }

    @Override
    public void save(Arena arena) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("name", arena.getName());

        for (Map.Entry<String, ArenaLocation> entry : arena.getPoints().entrySet()) {
            configuration.set("points." + entry.getKey(), entry.getValue().serialize());
        }
        for (Map.Entry<String, Region> entry : arena.getRegions().entrySet()) {
            configuration.set("regions." + entry.getKey(), entry.getValue().serialize());
        }
        for (Map.Entry<String, String> entry : arena.getProperties().entrySet()) {
            configuration.set("properties." + entry.getKey(), entry.getValue());
        }

        try {
            configuration.save(fileOf(arena));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando la arena '" + arena.getName() + "'.", exception);
        }
    }

    @Override
    public void delete(Arena arena) {
        File file = fileOf(arena);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("No se pudo eliminar el archivo de la arena '" + arena.getName() + "'.");
        }
    }

    private File fileOf(Arena arena) {
        return new File(directory, arena.getName().toLowerCase(Locale.ROOT) + EXTENSION);
    }
}
