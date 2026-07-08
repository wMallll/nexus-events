package com.nexusevents.arena;

/**
 * Claves canonicas de los puntos y regiones que puede almacenar una arena.
 *
 * <p>Los comandos de setup escriben bajo estas claves y los eventos las
 * leen. Un evento nuevo puede definir claves adicionales sin modificar
 * la clase {@link Arena}.</p>
 */
public final class ArenaKeys {

    /** Punto: spawn general del evento. */
    public static final String SPAWN = "spawn";

    /** Punto: lobby de espera de la arena. */
    public static final String LOBBY = "lobby";

    /** Punto: spawn del cazador (Escondete si puedes). */
    public static final String HUNTER_SPAWN = "hunter-spawn";

    /** Punto: centro del circulo (Circulo que se cierra). */
    public static final String CIRCLE_CENTER = "circle-center";

    /** Region: plataforma de Pixel Party. */
    public static final String REGION_PIXEL_PARTY = "pixel-party";

    /** Region: zona del recorrido de Parkour. */
    public static final String REGION_PARKOUR = "parkour";

    private ArenaKeys() {
        throw new UnsupportedOperationException("Clase de constantes: no debe instanciarse.");
    }
}
