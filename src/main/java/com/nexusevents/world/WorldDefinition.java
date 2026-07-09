package com.nexusevents.world;

import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Definicion persistente de un mundo administrado por el plugin:
 * entorno y configuracion de comportamiento (mobs, ciclo de dia,
 * clima, pvp, dificultad, keep-inventory).
 */
public final class WorldDefinition {

    /** Claves de configuracion aceptadas por {@code /evento world set}. */
    public static final List<String> SETTING_KEYS = Arrays.asList(
            "spawn-mobs", "always-day", "time", "no-rain", "pvp", "keep-inventory", "difficulty");

    private final String name;
    private final World.Environment environment;

    private boolean spawnMobs;
    private boolean alwaysDay;
    private int fixedTime;
    private boolean noRain;
    private boolean pvp;
    private boolean keepInventory;
    private Difficulty difficulty;

    private WorldDefinition(String name, World.Environment environment) {
        this.name = name;
        this.environment = environment;
    }

    /**
     * Crea la definicion por defecto de un mundo de eventos: sin mobs,
     * siempre de dia (mediodia), sin lluvia, con pvp, dificultad normal
     * y keep-inventory activado.
     *
     * @param name        nombre del mundo.
     * @param environment entorno (afecta el cielo y la niebla).
     * @return definicion lista para aplicar.
     */
    public static WorldDefinition createDefault(String name, World.Environment environment) {
        WorldDefinition definition = new WorldDefinition(name, environment);
        definition.spawnMobs = false;
        definition.alwaysDay = true;
        definition.fixedTime = 6000;
        definition.noRain = true;
        definition.pvp = true;
        definition.keepInventory = true;
        definition.difficulty = Difficulty.NORMAL;
        return definition;
    }

    /**
     * Reconstruye una definicion desde worlds.yml.
     *
     * @param name    nombre del mundo.
     * @param section seccion worlds.(nombre).
     * @param logger  logger para valores invalidos.
     * @return definicion cargada.
     */
    public static WorldDefinition parse(String name, ConfigurationSection section, Logger logger) {
        World.Environment environment = parseEnvironment(
                section.getString("environment", "NORMAL"), logger, name);
        WorldDefinition definition = new WorldDefinition(name, environment);
        definition.spawnMobs = section.getBoolean("settings.spawn-mobs", false);
        definition.alwaysDay = section.getBoolean("settings.always-day", true);
        definition.fixedTime = section.getInt("settings.time", 6000);
        definition.noRain = section.getBoolean("settings.no-rain", true);
        definition.pvp = section.getBoolean("settings.pvp", true);
        definition.keepInventory = section.getBoolean("settings.keep-inventory", true);
        definition.difficulty = parseDifficulty(
                section.getString("settings.difficulty", "NORMAL"), logger, name);
        return definition;
    }

    private static World.Environment parseEnvironment(String raw, Logger logger, String world) {
        try {
            return World.Environment.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            logger.warning("worlds.yml: entorno invalido '" + raw + "' en '" + world + "'. Se usa NORMAL.");
            return World.Environment.NORMAL;
        }
    }

    private static Difficulty parseDifficulty(String raw, Logger logger, String world) {
        try {
            return Difficulty.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            logger.warning("worlds.yml: dificultad invalida '" + raw + "' en '" + world + "'. Se usa NORMAL.");
            return Difficulty.NORMAL;
        }
    }

    /**
     * Escribe la definicion en la seccion raiz worlds.
     *
     * @param root seccion "worlds" del archivo.
     */
    public void save(ConfigurationSection root) {
        String base = name + ".";
        root.set(base + "environment", environment.name());
        root.set(base + "settings.spawn-mobs", spawnMobs);
        root.set(base + "settings.always-day", alwaysDay);
        root.set(base + "settings.time", fixedTime);
        root.set(base + "settings.no-rain", noRain);
        root.set(base + "settings.pvp", pvp);
        root.set(base + "settings.keep-inventory", keepInventory);
        root.set(base + "settings.difficulty", difficulty.name());
    }

    /**
     * Actualiza una opcion desde texto (comando set). No persiste ni
     * aplica: eso lo hace el servicio.
     *
     * @param key   opcion (ver SETTING_KEYS).
     * @param value valor en texto.
     * @return true si la opcion y el valor son validos.
     */
    public boolean applySetting(String key, String value) {
        String lowered = value.trim().toLowerCase(Locale.ROOT);
        switch (key.toLowerCase(Locale.ROOT)) {
            case "spawn-mobs":
                return assignBoolean(lowered, result -> spawnMobs = result);
            case "always-day":
                return assignBoolean(lowered, result -> alwaysDay = result);
            case "no-rain":
                return assignBoolean(lowered, result -> noRain = result);
            case "pvp":
                return assignBoolean(lowered, result -> pvp = result);
            case "keep-inventory":
                return assignBoolean(lowered, result -> keepInventory = result);
            case "time":
                try {
                    int time = Integer.parseInt(lowered);
                    if (time < 0 || time > 24000) {
                        return false;
                    }
                    this.fixedTime = time;
                    return true;
                } catch (NumberFormatException exception) {
                    return false;
                }
            case "difficulty":
                try {
                    this.difficulty = Difficulty.valueOf(lowered.toUpperCase(Locale.ROOT));
                    return true;
                } catch (IllegalArgumentException exception) {
                    return false;
                }
            default:
                return false;
        }
    }

    private boolean assignBoolean(String value, java.util.function.Consumer<Boolean> setter) {
        if (value.equals("true") || value.equals("false")) {
            setter.accept(Boolean.parseBoolean(value));
            return true;
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public boolean isSpawnMobs() {
        return spawnMobs;
    }

    public boolean isAlwaysDay() {
        return alwaysDay;
    }

    public int getFixedTime() {
        return fixedTime;
    }

    public boolean isNoRain() {
        return noRain;
    }

    public boolean isPvp() {
        return pvp;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }
}
