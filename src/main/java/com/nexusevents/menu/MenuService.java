package com.nexusevents.menu;

import com.nexusevents.arena.ArenaManager;
import com.nexusevents.event.EventManager;
import com.nexusevents.lockout.LockoutService;
import com.nexusevents.manager.Manager;
import com.nexusevents.message.MessageService;
import com.nexusevents.scheduler.TaskScheduler;
import com.nexusevents.util.TextUtil;
import com.nexusevents.world.WorldService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Servicio central de menus: abre y refresca inventarios, expone los
 * servicios que los menus necesitan para renderizar datos y gestiona
 * las capturas de chat (para entradas de texto como el nombre de un
 * mundo nuevo).
 *
 * <p>Las acciones de los menus ejecutan los comandos reales del plugin
 * ({@code performCommand}), de modo que permisos, validaciones y
 * mensajes son identicos a la via por comandos.</p>
 */
public final class MenuService implements Manager {

    private final JavaPlugin plugin;
    private final TaskScheduler scheduler;
    private final MessageService messages;
    private final ArenaManager arenas;
    private final EventManager events;
    private final WorldService worlds;
    private final LockoutService lockouts;

    private final Map<UUID, Consumer<String>> prompts = new HashMap<>();

    public MenuService(JavaPlugin plugin, TaskScheduler scheduler, MessageService messages,
                       ArenaManager arenas, EventManager events, WorldService worlds,
                       LockoutService lockouts) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.messages = messages;
        this.arenas = arenas;
        this.events = events;
        this.worlds = worlds;
        this.lockouts = lockouts;
    }

    @Override
    public String getName() {
        return "Menus";
    }

    @Override
    public void enable() {
        prompts.clear();
    }

    @Override
    public void disable() {
        prompts.clear();
    }

    /**
     * Abre un menu para el jugador (en el siguiente tick, seguro
     * incluso desde un click de otro menu).
     *
     * @param player jugador destinatario.
     * @param menu   menu a abrir.
     */
    public void open(Player player, Menu menu) {
        scheduler.sync(() -> {
            if (!player.isOnline()) {
                return;
            }
            MenuHolder holder = new MenuHolder(menu);
            Inventory inventory = Bukkit.createInventory(holder, menu.getSize(),
                    TextUtil.toLegacy(menu.getTitle(player)));
            holder.setInventory(inventory);
            menu.build(player, inventory, this);
            player.openInventory(inventory);
        });
    }

    /**
     * Reabre un menu tras un pequeno retardo (para reflejar cambios de
     * un comando recien ejecutado).
     *
     * @param player     jugador destinatario.
     * @param menu       menu (nueva instancia) a abrir.
     * @param delayTicks retardo en ticks.
     */
    public void reopenLater(Player player, Menu menu, long delayTicks) {
        scheduler.syncLater(() -> open(player, menu), delayTicks);
    }

    /**
     * Cierra el menu del jugador y captura su proximo mensaje de chat.
     * Escribir "cancelar" aborta.
     *
     * @param player     jugador.
     * @param messageKey clave del mensaje de instrucciones.
     * @param handler    consumidor del texto ingresado (en el hilo
     *                   principal).
     */
    public void prompt(Player player, String messageKey, Consumer<String> handler) {
        prompts.put(player.getUniqueId(), handler);
        player.closeInventory();
        messages.send(player, messageKey);
    }

    /**
     * Extrae la captura de chat pendiente del jugador, si existe.
     *
     * @param playerId identificador del jugador.
     * @return consumidor pendiente o null.
     */
    public Consumer<String> consumePrompt(UUID playerId) {
        return prompts.remove(playerId);
    }

    /**
     * Indica si el jugador tiene una captura de chat pendiente.
     *
     * @param playerId identificador del jugador.
     * @return true si el proximo chat sera capturado.
     */
    public boolean hasPrompt(UUID playerId) {
        return prompts.containsKey(playerId);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public TaskScheduler getScheduler() {
        return scheduler;
    }

    public MessageService getMessages() {
        return messages;
    }

    public ArenaManager getArenas() {
        return arenas;
    }

    public EventManager getEvents() {
        return events;
    }

    public WorldService getWorlds() {
        return worlds;
    }

    public LockoutService getLockouts() {
        return lockouts;
    }
}
