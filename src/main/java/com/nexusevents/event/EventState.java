package com.nexusevents.event;

/**
 * Estados del ciclo de vida de una sesion de evento.
 *
 * <p>Flujo normal: {@code WAITING -> COUNTDOWN -> RUNNING -> ENDING}.
 * Si los jugadores bajan del minimo durante la cuenta regresiva, la
 * sesion vuelve de {@code COUNTDOWN} a {@code WAITING}.</p>
 */
public enum EventState {

    /** Lobby abierto, esperando jugadores. */
    WAITING("waiting"),

    /** Cuenta regresiva previa al inicio. */
    COUNTDOWN("countdown"),

    /** Evento en juego. */
    RUNNING("running"),

    /** Finalizando y restaurando jugadores. */
    ENDING("ending");

    private final String key;

    EventState(String key) {
        this.key = key;
    }

    /**
     * Clave usada para resolver el nombre visible del estado en
     * {@code messages.yml} ({@code event.states.<clave>}).
     *
     * @return clave del estado.
     */
    public String getKey() {
        return key;
    }

    /**
     * Indica si un jugador puede unirse en este estado.
     *
     * @return true durante el lobby y la cuenta regresiva.
     */
    public boolean isJoinable() {
        return this == WAITING || this == COUNTDOWN;
    }
}
