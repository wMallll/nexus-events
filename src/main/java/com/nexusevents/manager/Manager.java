package com.nexusevents.manager;

/**
 * Contrato base para todo modulo del plugin con ciclo de vida propio.
 *
 * <p>Los managers se habilitan en orden de registro y se deshabilitan
 * en orden inverso desde {@link ManagerRegistry}.</p>
 */
public interface Manager {

    /**
     * Nombre legible del manager, usado en logs de consola.
     *
     * @return nombre del manager.
     */
    String getName();

    /**
     * Inicializa el manager. Se invoca una unica vez durante el arranque.
     */
    void enable();

    /**
     * Libera todos los recursos del manager. Se invoca durante el apagado.
     */
    void disable();
}
