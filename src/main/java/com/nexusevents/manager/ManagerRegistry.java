package com.nexusevents.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registro central de managers del plugin.
 *
 * <p>Garantiza un ciclo de vida ordenado: habilita en orden de registro,
 * deshabilita en orden inverso y recarga unicamente los managers que
 * implementan {@link Reloadable}. Un fallo en un manager queda aislado
 * y no interrumpe al resto.</p>
 */
public final class ManagerRegistry {

    private final Logger logger;
    private final Map<Class<? extends Manager>, Manager> managers = new LinkedHashMap<>();

    public ManagerRegistry(Logger logger) {
        this.logger = logger;
    }

    /**
     * Registra un manager bajo su tipo. El orden de registro define
     * el orden de habilitado.
     *
     * @param type    clase del manager, usada como clave de acceso.
     * @param manager instancia del manager.
     * @param <T>     tipo concreto del manager.
     * @throws IllegalStateException si el tipo ya fue registrado.
     */
    public <T extends Manager> void register(Class<T> type, T manager) {
        if (managers.containsKey(type)) {
            throw new IllegalStateException("El manager ya fue registrado: " + type.getName());
        }
        managers.put(type, manager);
    }

    /**
     * Obtiene un manager registrado por su tipo.
     *
     * @param type clase del manager.
     * @param <T>  tipo concreto del manager.
     * @return la instancia registrada.
     * @throws IllegalStateException si el tipo no fue registrado.
     */
    public <T extends Manager> T get(Class<T> type) {
        Manager manager = managers.get(type);
        if (manager == null) {
            throw new IllegalStateException("El manager no esta registrado: " + type.getName());
        }
        return type.cast(manager);
    }

    public void enableAll() {
        for (Manager manager : managers.values()) {
            long start = System.currentTimeMillis();
            try {
                manager.enable();
                logger.info("Modulo [" + manager.getName() + "] habilitado ("
                        + (System.currentTimeMillis() - start) + "ms).");
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Error habilitando el modulo [" + manager.getName() + "].", exception);
            }
        }
    }

    public void disableAll() {
        List<Manager> reversed = new ArrayList<>(managers.values());
        Collections.reverse(reversed);
        for (Manager manager : reversed) {
            try {
                manager.disable();
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Error deshabilitando el modulo [" + manager.getName() + "].", exception);
            }
        }
        managers.clear();
    }

    /**
     * Recarga todos los managers que implementan {@link Reloadable}.
     *
     * @return tiempo total de recarga en milisegundos.
     */
    public long reloadAll() {
        long start = System.currentTimeMillis();
        for (Manager manager : managers.values()) {
            if (manager instanceof Reloadable) {
                try {
                    ((Reloadable) manager).reload();
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Error recargando el modulo [" + manager.getName() + "].", exception);
                }
            }
        }
        return System.currentTimeMillis() - start;
    }
}
