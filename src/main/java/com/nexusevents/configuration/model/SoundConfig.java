package com.nexusevents.configuration.model;

import com.cryptomorin.xseries.XSound;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Sonido configurable e inmutable.
 *
 * <p>El nombre se escribe en YAML usando la nomenclatura moderna
 * (por ejemplo {@code ENTITY_EXPERIENCE_ORB_PICKUP}) y XSound lo traduce
 * automaticamente al equivalente de la version del servidor, desde 1.9.
 * Un nombre invalido genera una instancia desactivada con warning:
 * una mala configuracion nunca rompe el juego.</p>
 */
public final class SoundConfig {

    private static final SoundConfig DISABLED = new SoundConfig(false, null, 1.0F, 1.0F);

    private final boolean enabled;
    private final Sound sound;
    private final float volume;
    private final float pitch;

    private SoundConfig(boolean enabled, Sound sound, float volume, float pitch) {
        this.enabled = enabled;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * Parsea un sonido desde una seccion YAML con las claves
     * {@code sound}, {@code volume}, {@code pitch} y {@code enabled}.
     *
     * @param section seccion de configuracion (puede ser null).
     * @param logger  logger para reportar sonidos invalidos.
     * @return sonido configurado o instancia desactivada.
     */
    public static SoundConfig parse(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return DISABLED;
        }
        boolean enabled = section.getBoolean("enabled", true);
        if (!enabled) {
            return DISABLED;
        }
        String name = section.getString("sound", "");
        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);
        return of(name, volume, pitch, logger, section.getCurrentPath());
    }

    /**
     * Crea un sonido a partir de su nombre y parametros.
     *
     * @param name    nombre del sonido en nomenclatura moderna.
     * @param volume  volumen.
     * @param pitch   tono.
     * @param logger  logger para reportar nombres invalidos.
     * @param context ruta de configuracion, usada en el warning.
     * @return sonido configurado o instancia desactivada.
     */
    public static SoundConfig of(String name, float volume, float pitch, Logger logger, String context) {
        if (name == null || name.trim().isEmpty()) {
            return DISABLED;
        }
        Sound resolved = XSound.matchXSound(name.trim()).map(XSound::parseSound).orElse(null);
        if (resolved == null) {
            logger.warning("Sonido desconocido '" + name + "' en '" + context
                    + "': no existe en esta version del servidor. Se desactiva.");
            return DISABLED;
        }
        return new SoundConfig(true, resolved, volume, pitch);
    }

    /**
     * Devuelve la instancia desactivada compartida.
     *
     * @return sonido desactivado.
     */
    public static SoundConfig disabled() {
        return DISABLED;
    }

    /**
     * Reproduce el sonido en la posicion del jugador.
     *
     * @param player jugador receptor.
     */
    public void play(Player player) {
        play(player, player.getLocation());
    }

    /**
     * Reproduce el sonido en una posicion concreta, solo audible
     * para el jugador dado.
     *
     * @param player   jugador receptor.
     * @param location posicion de origen del sonido.
     */
    public void play(Player player, Location location) {
        if (enabled && sound != null) {
            player.playSound(location, sound, volume, pitch);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }
}
