package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import com.nexusevents.permission.Permissions;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

/**
 * Menu principal del plugin: acceso a eventos, arenas, mundos y
 * moderacion segun los permisos del jugador.
 */
public final class MainMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>NexusEvents";
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        inventory.setItem(10, MenuItems.item(XMaterial.NETHER_STAR, "<gold><bold>Eventos",
                "<gray>Iniciar, unirse, salir o detener", "<gray>los eventos del servidor."));
        if (player.hasPermission("evento.arena.list")) {
            inventory.setItem(12, MenuItems.item(XMaterial.MAP, "<aqua><bold>Arenas",
                    "<gray>Crear y configurar arenas:", "<gray>puntos, círculo, parkour, pixel party."));
        }
        if (player.hasPermission(Permissions.WORLD)) {
            inventory.setItem(14, MenuItems.item(XMaterial.GRASS_BLOCK, "<green><bold>Mundos",
                    "<gray>Crear mundos vacíos y", "<gray>configurarlos a gusto."));
        }
        if (player.hasPermission(Permissions.DISQUALIFY) || player.hasPermission(Permissions.TP_DEAD)
                || player.hasPermission(Permissions.LOCKOUT)) {
            inventory.setItem(16, MenuItems.item(XMaterial.IRON_SWORD, "<red><bold>Moderación",
                    "<gray>Descalificar, teletransportar", "<gray>eliminados y modo torneo."));
        }
        if (player.hasPermission(Permissions.setup("mainlobby"))) {
            inventory.setItem(4, MenuItems.item(XMaterial.BEACON, "<gold><bold>Lobby Global",
                    "<gray>Spawn, región protegida,", "<gray>altura mínima y protecciones."));
        }
        inventory.setItem(22, MenuItems.close());
        MenuItems.fillBorder(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        switch (slot) {
            case 4:
                if (player.hasPermission(Permissions.setup("mainlobby"))) {
                    menus.open(player, new MainLobbyMenu());
                }
                break;
            case 10:
                menus.open(player, new EventsMenu());
                break;
            case 12:
                if (player.hasPermission("evento.arena.list")) {
                    menus.open(player, new ArenasMenu());
                }
                break;
            case 14:
                if (player.hasPermission(Permissions.WORLD)) {
                    menus.open(player, new WorldsMenu());
                }
                break;
            case 16:
                menus.open(player, new ModerationMenu());
                break;
            case 22:
                player.closeInventory();
                break;
            default:
                break;
        }
    }
}
