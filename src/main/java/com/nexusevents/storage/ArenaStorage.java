package com.nexusevents.storage;

import com.nexusevents.arena.Arena;

import java.util.List;

/**
 * Contrato de persistencia de arenas.
 *
 * <p>El {@code ArenaManager} depende de esta abstraccion y no de una
 * implementacion concreta: hoy se persiste en YAML y en el futuro puede
 * cambiarse por SQLite u otro backend sin tocar el resto del plugin.</p>
 */
public interface ArenaStorage {

    /**
     * Carga todas las arenas persistidas.
     *
     * @return arenas encontradas (lista vacia si no hay ninguna).
     */
    List<Arena> loadAll();

    /**
     * Persiste el estado completo de una arena.
     *
     * @param arena arena a guardar.
     */
    void save(Arena arena);

    /**
     * Elimina definitivamente la persistencia de una arena.
     *
     * @param arena arena a eliminar.
     */
    void delete(Arena arena);
}
