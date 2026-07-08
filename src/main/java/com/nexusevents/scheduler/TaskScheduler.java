package com.nexusevents.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Fachada sobre el scheduler de Bukkit.
 *
 * <p>Centraliza la creacion de tareas sincronas y asincronas del plugin.
 * Todos los temporizadores de eventos y procesos en segundo plano deben
 * crearse a traves de esta clase, lo que da un unico punto de control
 * sobre el threading y permite cancelar todo durante el apagado.</p>
 */
public final class TaskScheduler {

    private final JavaPlugin plugin;

    public TaskScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ejecuta una tarea en el hilo principal en el proximo tick.
     *
     * @param task tarea a ejecutar.
     * @return tarea programada.
     */
    public BukkitTask sync(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Ejecuta una tarea fuera del hilo principal.
     *
     * @param task tarea a ejecutar.
     * @return tarea programada.
     */
    public BukkitTask async(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    /**
     * Ejecuta una tarea en el hilo principal luego de un retraso.
     *
     * @param task       tarea a ejecutar.
     * @param delayTicks retraso en ticks (20 ticks = 1 segundo).
     * @return tarea programada.
     */
    public BukkitTask syncLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Ejecuta una tarea fuera del hilo principal luego de un retraso.
     *
     * @param task       tarea a ejecutar.
     * @param delayTicks retraso en ticks.
     * @return tarea programada.
     */
    public BukkitTask asyncLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    /**
     * Ejecuta una tarea repetitiva en el hilo principal.
     *
     * @param task        tarea a ejecutar.
     * @param delayTicks  retraso inicial en ticks.
     * @param periodTicks periodo de repeticion en ticks.
     * @return tarea programada.
     */
    public BukkitTask syncTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Ejecuta una tarea repetitiva fuera del hilo principal.
     *
     * @param task        tarea a ejecutar.
     * @param delayTicks  retraso inicial en ticks.
     * @param periodTicks periodo de repeticion en ticks.
     * @return tarea programada.
     */
    public BukkitTask asyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Cancela todas las tareas pendientes registradas por el plugin.
     */
    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
