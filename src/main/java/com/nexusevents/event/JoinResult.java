package com.nexusevents.event;

/**
 * Resultado de un intento de union a un evento. El comando
 * {@code /evento join} mapea cada valor a su mensaje configurable.
 */
public enum JoinResult {

    /** El jugador entro correctamente. */
    SUCCESS,

    /** No hay ningun evento activo. */
    NONE_ACTIVE,

    /** No hay evento activo en la arena indicada. */
    NOT_FOUND,

    /** Hay varios eventos activos y no se indico arena. */
    AMBIGUOUS,

    /** El jugador ya participa de un evento. */
    ALREADY_IN,

    /** El evento alcanzo el maximo de jugadores. */
    FULL,

    /** El evento ya comenzo. */
    ALREADY_STARTED,

    /** El jugador esta bloqueado por el modo torneo. */
    LOCKED
}
