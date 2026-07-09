package com.nexusevents.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Enruta los clicks de los menus del plugin (cancelandolos siempre) y
 * captura los mensajes de chat de los prompts activos.
 */
public final class MenuListener implements Listener {

    private final MenuService menus;

    public MenuListener(MenuService menus) {
        this.menus = menus;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof MenuHolder) || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        event.setCancelled(true);
        if (!top.equals(event.getClickedInventory())) {
            return;
        }
        int slot = event.getSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        ((MenuHolder) holder).getMenu()
                .onClick((Player) event.getWhoClicked(), slot, event.getClick(), menus);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent event) {
        if (!menus.hasPrompt(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        Consumer<String> handler = menus.consumePrompt(event.getPlayer().getUniqueId());
        String input = event.getMessage().trim();
        menus.getScheduler().sync(() -> {
            if (!event.getPlayer().isOnline() || handler == null) {
                return;
            }
            if (input.toLowerCase(Locale.ROOT).equals("cancelar")) {
                menus.getMessages().send(event.getPlayer(), "menu.prompt-cancelled");
                return;
            }
            handler.accept(input);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        menus.consumePrompt(event.getPlayer().getUniqueId());
    }
}
