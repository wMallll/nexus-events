package com.nexusevents.world;

import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.configuration.ConfigurationFile;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import com.nexusevents.scheduler.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Gestor de mundos del plugin (estilo Multiverse, minimalista).
 *
 * <p>Crea mundos completamente vacios con {@link VoidChunkGenerator},
 * los registra en {@code worlds.yml}, los carga automaticamente al
 * arrancar y les aplica su configuracion: aparicion de mobs, dia
 * permanente con hora fija, clima bloqueado, pvp, dificultad y
 * keep-inventory. Una tarea de refuerzo periodica garantiza el dia y
 * el clima incluso en versiones sin las gamerules modernas (1.9/1.10)
 * o si otro plugin los altera.</p>
 */
public final class WorldService implements Manager, Reloadable {

    public static final String FILE = "worlds.yml";
    private static final String ROOT = "worlds";
    private static final long ENFORCER_PERIOD_TICKS = 100L;

    /** Resultado de la creacion de un mundo. */
    public enum CreateResult { SUCCESS, ALREADY_EXISTS, INVALID_NAME, FAILED }

    /** Resultado de la eliminacion de un mundo. */
    public enum DeleteResult { SUCCESS, NOT_FOUND, MAIN_WORLD, FAILED }

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final TaskScheduler scheduler;
    private final Map<String, WorldDefinition> definitions = new LinkedHashMap<>();

    private BukkitTask enforcer;

    public WorldService(JavaPlugin plugin, ConfigManager configManager, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scheduler = scheduler;
    }

    @Override
    public String getName() {
        return "Mundos";
    }

    @Override
    public void enable() {
        configManager.register(FILE);
        loadDefinitions();
        for (WorldDefinition definition : definitions.values()) {
            loadWorld(definition);
        }
        this.enforcer = scheduler.syncTimer(this::enforceSettings, ENFORCER_PERIOD_TICKS, ENFORCER_PERIOD_TICKS);
    }

    @Override
    public void disable() {
        if (enforcer != null) {
            enforcer.cancel();
            enforcer = null;
        }
        definitions.clear();
    }

    @Override
    public void reload() {
        loadDefinitions();
        for (WorldDefinition definition : definitions.values()) {
            World world = Bukkit.getWorld(definition.getName());
            if (world != null) {
                applySettings(world, definition);
            }
        }
    }

    private void loadDefinitions() {
        definitions.clear();
        ConfigurationSection root = configManager.getFile(FILE).get().getConfigurationSection(ROOT);
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section != null) {
                definitions.put(name.toLowerCase(Locale.ROOT),
                        WorldDefinition.parse(name, section, plugin.getLogger()));
            }
        }
    }

    private void saveAll() {
        ConfigurationFile file = configManager.getFile(FILE);
        file.get().set(ROOT, null);
        ConfigurationSection root = file.get().createSection(ROOT);
        for (WorldDefinition definition : definitions.values()) {
            definition.save(root);
        }
        file.save();
    }

    // ------------------------------------------------------------------
    // Operaciones
    // ------------------------------------------------------------------

    /**
     * Crea un mundo vacio nuevo con la configuracion por defecto y lo
     * registra.
     *
     * @param name        nombre del mundo (letras, numeros, - y _).
     * @param environment entorno del mundo.
     * @return resultado de la operacion.
     */
    public CreateResult create(String name, World.Environment environment) {
        if (!name.matches("[a-zA-Z0-9_-]{1,32}")) {
            return CreateResult.INVALID_NAME;
        }
        if (Bukkit.getWorld(name) != null || definitions.containsKey(name.toLowerCase(Locale.ROOT))) {
            return CreateResult.ALREADY_EXISTS;
        }
        WorldDefinition definition = WorldDefinition.createDefault(name, environment);
        World world = loadWorld(definition);
        if (world == null) {
            return CreateResult.FAILED;
        }
        prepareSpawn(world);
        definitions.put(name.toLowerCase(Locale.ROOT), definition);
        saveAll();
        return CreateResult.SUCCESS;
    }

    /**
     * Carga (o crea en disco si no existe) el mundo de una definicion
     * con el generador void y le aplica su configuracion.
     */
    private World loadWorld(WorldDefinition definition) {
        World existing = Bukkit.getWorld(definition.getName());
        if (existing != null) {
            applySettings(existing, definition);
            return existing;
        }
        try {
            World world = new WorldCreator(definition.getName())
                    .environment(definition.getEnvironment())
                    .generator(new VoidChunkGenerator())
                    .generateStructures(false)
                    .createWorld();
            if (world != null) {
                applySettings(world, definition);
            } else {
                plugin.getLogger().severe("No se pudo cargar el mundo '" + definition.getName() + "'.");
            }
            return world;
        } catch (Exception exception) {
            plugin.getLogger().severe("Error cargando el mundo '" + definition.getName()
                    + "': " + exception.getMessage());
            return null;
        }
    }

    /**
     * Carga un mundo registrado por nombre (por ejemplo, tras un
     * arranque en el que fallo).
     *
     * @param name nombre registrado.
     * @return mundo cargado, si fue posible.
     */
    public Optional<World> load(String name) {
        WorldDefinition definition = definitions.get(name.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadWorld(definition));
    }

    /**
     * Coloca una plataforma de vidrio en el origen y fija ahi el spawn,
     * para que nadie caiga al vacio en un mundo recien creado.
     */
    private void prepareSpawn(World world) {
        Block base = world.getBlockAt(0, 64, 0);
        if (base.getType() == Material.AIR) {
            base.setType(Material.GLASS);
        }
        world.setSpawnLocation(0, 65, 0);
    }

    /**
     * Aplica la configuracion de la definicion al mundo cargado:
     * gamerules, flags de spawn, pvp, dificultad, hora y clima.
     */
    @SuppressWarnings("deprecation")
    public void applySettings(World world, WorldDefinition definition) {
        world.setGameRuleValue("doMobSpawning", String.valueOf(definition.isSpawnMobs()));
        world.setGameRuleValue("doDaylightCycle", String.valueOf(!definition.isAlwaysDay()));
        world.setGameRuleValue("doWeatherCycle", String.valueOf(!definition.isNoRain()));
        world.setGameRuleValue("keepInventory", String.valueOf(definition.isKeepInventory()));
        world.setSpawnFlags(definition.isSpawnMobs(), definition.isSpawnMobs());
        world.setPVP(definition.isPvp());
        world.setDifficulty(definition.getDifficulty());
        if (definition.isAlwaysDay()) {
            world.setTime(definition.getFixedTime());
        }
        if (definition.isNoRain()) {
            world.setStorm(false);
            world.setThundering(false);
        }
    }

    /**
     * Refuerzo periodico: mantiene la hora fija y el cielo despejado en
     * los mundos que lo piden, en cualquier version y contra cualquier
     * interferencia.
     */
    private void enforceSettings() {
        for (WorldDefinition definition : definitions.values()) {
            World world = Bukkit.getWorld(definition.getName());
            if (world == null) {
                continue;
            }
            if (definition.isAlwaysDay()) {
                world.setTime(definition.getFixedTime());
            }
            if (definition.isNoRain() && (world.hasStorm() || world.isThundering())) {
                world.setStorm(false);
                world.setThundering(false);
            }
        }
    }

    /**
     * Actualiza una opcion de un mundo registrado, la aplica en vivo si
     * el mundo esta cargado y la persiste.
     *
     * @param name  nombre del mundo.
     * @param key   opcion.
     * @param value valor en texto.
     * @return true si se actualizo (false: opcion o valor invalidos).
     */
    public boolean setSetting(String name, String key, String value) {
        WorldDefinition definition = definitions.get(name.toLowerCase(Locale.ROOT));
        if (definition == null || !definition.applySetting(key, value)) {
            return false;
        }
        World world = Bukkit.getWorld(definition.getName());
        if (world != null) {
            applySettings(world, definition);
        }
        saveAll();
        return true;
    }

    /**
     * Elimina un mundo registrado: mueve a sus jugadores al mundo
     * principal, lo descarga y borra su carpeta del disco.
     *
     * @param name nombre del mundo.
     * @return resultado de la operacion.
     */
    public DeleteResult delete(String name) {
        WorldDefinition definition = definitions.get(name.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return DeleteResult.NOT_FOUND;
        }
        World world = Bukkit.getWorld(definition.getName());
        World main = Bukkit.getWorlds().get(0);
        if (world != null) {
            if (world.equals(main)) {
                return DeleteResult.MAIN_WORLD;
            }
            Location refuge = main.getSpawnLocation();
            for (Player player : world.getPlayers()) {
                player.teleport(refuge);
            }
            File folder = world.getWorldFolder();
            if (!Bukkit.unloadWorld(world, false)) {
                return DeleteResult.FAILED;
            }
            deleteRecursively(folder);
        } else {
            deleteRecursively(new File(Bukkit.getWorldContainer(), definition.getName()));
        }
        definitions.remove(name.toLowerCase(Locale.ROOT));
        saveAll();
        return DeleteResult.SUCCESS;
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            plugin.getLogger().warning("No se pudo borrar: " + file.getAbsolutePath());
        }
    }

    /**
     * Indica si el nombre corresponde a un mundo administrado por el
     * plugin.
     *
     * @param name nombre del mundo.
     * @return true si esta registrado.
     */
    public boolean isPluginWorld(String name) {
        return definitions.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Definicion registrada de un mundo.
     *
     * @param name nombre del mundo.
     * @return definicion, si existe.
     */
    public Optional<WorldDefinition> getDefinition(String name) {
        return Optional.ofNullable(definitions.get(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Todas las definiciones registradas.
     *
     * @return vista de solo lectura.
     */
    public Collection<WorldDefinition> getDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }
}
