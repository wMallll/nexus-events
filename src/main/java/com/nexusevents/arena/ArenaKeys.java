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

    /** Punto: centro del circulo (El Circulo). */
    public static final String CIRCLE_CENTER = "circle-center";

    /** Propiedad: radio del circulo en bloques (El Circulo). */
    public static final String CIRCLE_RADIUS = "circle-radius";

    /** Prefijo de region: fragmentos ordenados del Parkour (islas). */
    public static final String PARKOUR_FRAGMENT_PREFIX = "parkour-fragment-";

    /** Propiedad: altura minima de seguridad (teleport al lobby). */
    public static final String MIN_Y = "min-y";

    /** Region: plataforma de Pixel Party. */
    public static final String REGION_PIXEL_PARTY = "pixel-party";


    private ArenaKeys() {
        throw new UnsupportedOperationException("Clase de constantes: no debe instanciarse.");
    }
}
