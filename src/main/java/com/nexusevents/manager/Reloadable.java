package com.nexusevents.manager;

/**
 * Contrato para componentes que soportan recarga en caliente.
 *
 * <p>Se mantiene separado de {@link Manager} (principio de segregacion de
 * interfaces): solo los managers que realmente tienen estado recargable
 * lo implementan, evitando metodos vacios en el resto.</p>
 */
public interface Reloadable {

    /**
     * Vuelve a cargar el estado del componente desde su fuente
     * (archivos YAML, cache, etc.) sin reiniciar el servidor.
     */
    void reload();
}
