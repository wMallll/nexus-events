package com.nexusevents.event;

/**
 * Motivo por el cual finaliza una sesion de evento. Determina que
 * anuncio se envia al cerrar.
 */
public enum EventEndReason {

    /** Quedo un ganador. */
    WINNER,

    /** Se agoto la duracion maxima. */
    TIMEOUT,

    /** Un administrador detuvo el evento. */
    CANCELLED,

    /** No se alcanzo el minimo de jugadores en el lobby. */
    NOT_ENOUGH_PLAYERS,

    /** Todos los jugadores fueron eliminados o abandonaron. */
    NO_PLAYERS
}
