package com.nexusevents.listener;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Punto unico de registro de listeners del plugin.
 *
 * <p>Los modulos registran sus listeners aqui en lugar de hacerlo
 * directamente contra Bukkit. Esto permite auditar que listeners estan
 * activos y desregistrar absolutamente todo durante el apagado o antes
 * de una recarga completa.</p>
 */
public final class ListenerManager {

    private final JavaPlugin plugin;
    private final List<Listener> registered = new ArrayList<>();

    public ListenerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registra un listener contra el PluginManager de Bukkit y lo
     * agrega al indice interno.
     *
     * @param listener listener a registrar.
     */
    public void register(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        registered.add(listener);
    }

    /**
     * Registra varios listeners en el orden recibido.
     *
     * @param listeners listeners a registrar.
     */
    public void registerAll(Listener... listeners) {
        for (Listener listener : listeners) {
            register(listener);
        }
    }

    /**
     * Desregistra un listener puntual.
     *
     * @param listener listener a desregistrar.
     */
    public void unregister(Listener listener) {
        HandlerList.unregisterAll(listener);
        registered.remove(listener);
    }

    /**
     * Desregistra todos los listeners del plugin.
     */
    public void unregisterAll() {
        HandlerList.unregisterAll(plugin);
        registered.clear();
    }

    /**
     * Devuelve una vista de solo lectura de los listeners registrados.
     *
     * @return listeners activos registrados por el plugin.
     */
    public List<Listener> getRegistered() {
        return Collections.unmodifiableList(registered);
    }
}
