package com.nexusevents.event;

import com.nexusevents.arena.Arena;

import java.util.List;

/**
 * Definicion de un tipo de evento.
 *
 * <p>Separa el <b>tipo</b> (esta interfaz: id, requisitos de arena,
 * fabrica) de la <b>partida en curso</b> ({@link EventSession}). Agregar
 * un evento nuevo al plugin se reduce a implementar esta interfaz y
 * registrarla en el {@link EventManager}.</p>
 */
public interface GameEvent {

    /**
     * Identificador unico y estable del evento (por ejemplo
     * {@code hide-and-seek}). Se usa en comandos, permisos, mensajes
     * ({@code event.names.<id>}) y archivos de configuracion.
     *
     * @return id del evento en minusculas.
     */
    String getId();

    /**
     * Claves de puntos que la arena debe tener configurados para poder
     * iniciar este evento, ademas de los obligatorios globales
     * (spawn y lobby).
     *
     * @return claves de puntos requeridos (puede ser vacia).
     */
    List<String> getRequiredPoints();

    /**
     * Claves de regiones que la arena debe tener configuradas para
     * poder iniciar este evento.
     *
     * @return claves de regiones requeridas (puede ser vacia).
     */
    List<String> getRequiredRegions();

    /**
     * Crea una nueva partida de este evento sobre la arena dada.
     *
     * @param context servicios del plugin.
     * @param arena   arena validada donde se jugara.
     * @return sesion lista para abrirse.
     */
    EventSession createSession(EventContext context, Arena arena);
}
