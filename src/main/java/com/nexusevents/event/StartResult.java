package com.nexusevents.event;

/**
 * Resultado de un intento de inicio de evento.
 *
 * <p>Cuando el estado es {@link Status#ARENA_INCOMPLETE}, {@code detail}
 * contiene la lista legible de puntos y regiones que faltan configurar.</p>
 */
public final class StartResult {

    /** Estado del intento de inicio. */
    public enum Status {
        SUCCESS,
        EVENT_NOT_FOUND,
        ARENA_NOT_FOUND,
        ARENA_IN_USE,
        ARENA_INCOMPLETE
    }

    private final Status status;
    private final String detail;

    private StartResult(Status status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    /**
     * Crea un resultado sin detalle adicional.
     *
     * @param status estado del intento.
     * @return resultado.
     */
    public static StartResult of(Status status) {
        return new StartResult(status, "");
    }

    /**
     * Crea un resultado de arena incompleta con el detalle de faltantes.
     *
     * @param missing lista legible de puntos/regiones faltantes.
     * @return resultado.
     */
    public static StartResult incomplete(String missing) {
        return new StartResult(Status.ARENA_INCOMPLETE, missing);
    }

    public Status getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
