package com.nexusevents.configuration.model;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Titulo configurable e inmutable.
 *
 * <p>Contiene el texto principal, el subtitulo y los tiempos de
 * animacion en ticks. Los textos soportan legacy, HEX, MiniMessage y
 * placeholders dinamicos, como todo el sistema de mensajes.</p>
 */
public final class TitleConfig {

    private static final TitleConfig DISABLED = new TitleConfig(false, "", "", 10, 60, 10);

    private final boolean enabled;
    private final String title;
    private final String subtitle;
    private final int fadeInTicks;
    private final int stayTicks;
    private final int fadeOutTicks;

    private TitleConfig(boolean enabled, String title, String subtitle,
                        int fadeInTicks, int stayTicks, int fadeOutTicks) {
        this.enabled = enabled;
        this.title = title;
        this.subtitle = subtitle;
        this.fadeInTicks = fadeInTicks;
        this.stayTicks = stayTicks;
        this.fadeOutTicks = fadeOutTicks;
    }

    /**
     * Parsea un titulo desde una seccion YAML con las claves
     * {@code title}, {@code subtitle}, {@code fade-in}, {@code stay},
     * {@code fade-out} y {@code enabled}.
     *
     * @param section seccion de configuracion (puede ser null).
     * @return titulo configurado o instancia desactivada.
     */
    public static TitleConfig parse(ConfigurationSection section) {
        if (section == null) {
            return DISABLED;
        }
        boolean enabled = section.getBoolean("enabled", true);
        if (!enabled) {
            return DISABLED;
        }
        return new TitleConfig(
                true,
                section.getString("title", ""),
                section.getString("subtitle", ""),
                section.getInt("fade-in", 10),
                section.getInt("stay", 60),
                section.getInt("fade-out", 10)
        );
    }

    /**
     * Crea un titulo a partir de textos y tiempos ya resueltos.
     *
     * @param title        texto principal.
     * @param subtitle     subtitulo.
     * @param fadeInTicks  ticks de aparicion.
     * @param stayTicks    ticks de permanencia.
     * @param fadeOutTicks ticks de desaparicion.
     * @return titulo configurado.
     */
    public static TitleConfig of(String title, String subtitle,
                                 int fadeInTicks, int stayTicks, int fadeOutTicks) {
        return new TitleConfig(true, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
    }

    /**
     * Devuelve la instancia desactivada compartida.
     *
     * @return titulo desactivado.
     */
    public static TitleConfig disabled() {
        return DISABLED;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getFadeInTicks() {
        return fadeInTicks;
    }

    public int getStayTicks() {
        return stayTicks;
    }

    public int getFadeOutTicks() {
        return fadeOutTicks;
    }
}
