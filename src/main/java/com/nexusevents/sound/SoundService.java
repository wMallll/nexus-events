package com.nexusevents.sound;

import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.configuration.model.SoundConfig;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registro central de sonidos configurables.
 *
 * <p>El codigo del plugin reproduce sonidos por clave logica
 * (por ejemplo {@code command-success}) y el sonido real, su volumen y
 * su tono se definen en {@code sounds.yml}. Cada entrada acepta el
 * formato completo (seccion con sound/volume/pitch/enabled) o el
 * abreviado (solo el nombre del sonido).</p>
 */
public final class SoundService implements Manager, Reloadable {

    private static final String ROOT_SECTION = "sounds";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private final Map<String, SoundConfig> sounds = new HashMap<>();
    private final Set<String> warnedMissing = new HashSet<>();

    public SoundService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "Sonidos";
    }

    @Override
    public void enable() {
        loadSounds();
    }

    @Override
    public void disable() {
        sounds.clear();
        warnedMissing.clear();
    }

    @Override
    public void reload() {
        loadSounds();
    }

    private void loadSounds() {
        sounds.clear();
        warnedMissing.clear();

        FileConfiguration file = configManager.getFile(ConfigManager.SOUNDS_FILE).get();
        ConfigurationSection root = file.getConfigurationSection(ROOT_SECTION);
        if (root == null) {
            plugin.getLogger().warning("sounds.yml no contiene la seccion '" + ROOT_SECTION + "'.");
            return;
        }

        for (String key : root.getKeys(false)) {
            sounds.put(key, parseEntry(root, key));
        }
        plugin.getLogger().info("Cargados " + sounds.size() + " sonidos configurables.");
    }

    private SoundConfig parseEntry(ConfigurationSection root, String key) {
        if (root.isConfigurationSection(key)) {
            return SoundConfig.parse(root.getConfigurationSection(key), plugin.getLogger());
        }
        String shorthand = root.getString(key, "");
        return SoundConfig.of(shorthand, 1.0F, 1.0F, plugin.getLogger(), ROOT_SECTION + "." + key);
    }

    /**
     * Reproduce para el jugador el sonido asociado a la clave logica.
     * Si la clave no existe, avisa una unica vez en consola.
     *
     * @param player jugador receptor.
     * @param key    clave logica del sonido.
     */
    public void play(Player player, String key) {
        SoundConfig sound = sounds.get(key);
        if (sound == null) {
            if (warnedMissing.add(key)) {
                plugin.getLogger().warning("Se solicito el sonido '" + key
                        + "' pero no esta definido en sounds.yml.");
            }
            return;
        }
        sound.play(player);
    }

    /**
     * Reproduce el sonido para el emisor si es un jugador; la consola
     * simplemente no recibe sonido.
     *
     * @param sender emisor del comando.
     * @param key    clave logica del sonido.
     */
    public void play(org.bukkit.command.CommandSender sender, String key) {
        if (sender instanceof Player) {
            play((Player) sender, key);
        }
    }

    /**
     * Obtiene el sonido asociado a una clave logica.
     *
     * @param key clave logica.
     * @return sonido configurado, si existe.
     */
    public Optional<SoundConfig> get(String key) {
        return Optional.ofNullable(sounds.get(key));
    }
}
