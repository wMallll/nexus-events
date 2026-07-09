package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.event.GameEvent;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import com.nexusevents.event.EventSession;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Menu de eventos: iniciar (eligiendo evento y luego arena), unirse,
 * salir y detener sesiones activas.
 */
public final class EventsMenu extends Menu {

    private final Map<Integer, String> eventBySlot = new HashMap<>();

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Eventos";
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        eventBySlot.clear();
        int slot = 10;
        for (GameEvent type : menus.getEvents().getTypes()) {
            if (slot > 13) {
                break;
            }
            inventory.setItem(slot, MenuItems.item(iconOf(type.getId()),
                    "<gold><bold>" + menus.getMessages().rawOr("event.names." + type.getId(), type.getId()),
                    "<gray>Click: elegir arena e iniciar."));
            eventBySlot.put(slot, type.getId());
            slot++;
        }
        int active = menus.getEvents().getSessions().size();
        inventory.setItem(15, MenuItems.item(XMaterial.SLIME_BALL, "<green><bold>Unirme",
                "<gray>Sesiones activas: <yellow>" + active));
        inventory.setItem(16, MenuItems.item(XMaterial.REDSTONE, "<red><bold>Salir",
                "<gray>Abandona tu evento actual."));
        if (player.hasPermission("evento.stop")) {
            inventory.setItem(17, MenuItems.item(XMaterial.BARRIER, "<dark_red><bold>Detener",
                    "<gray>Detiene una sesión activa."));
        }
        inventory.setItem(31, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    private XMaterial iconOf(String eventId) {
        switch (eventId.toLowerCase(Locale.ROOT)) {
            case "hide-and-seek":
                return XMaterial.ENDER_EYE;
            case "pixel-party":
                return XMaterial.WHITE_WOOL;
            case "parkour":
                return XMaterial.LADDER;
            case "circle":
                return XMaterial.SNOWBALL;
            default:
                return XMaterial.PAPER;
        }
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        String eventId = eventBySlot.get(slot);
        if (eventId != null) {
            menus.open(player, new ArenaPickMenu(eventId));
            return;
        }
        switch (slot) {
            case 15:
                joinFlow(player, menus);
                break;
            case 16:
                player.closeInventory();
                player.performCommand("evento leave");
                break;
            case 17:
                if (player.hasPermission("evento.stop")) {
                    menus.open(player, new SessionsMenu(SessionsMenu.Mode.STOP));
                }
                break;
            case 31:
                menus.open(player, new MainMenu());
                break;
            default:
                break;
        }
    }

    private void joinFlow(Player player, MenuService menus) {
        Collection<EventSession> sessions = menus.getEvents().getSessions();
        if (sessions.isEmpty()) {
            player.closeInventory();
            menus.getMessages().send(player, "menu.no-sessions");
            return;
        }
        if (sessions.size() == 1) {
            String arena = sessions.iterator().next().getArena().getName();
            player.closeInventory();
            player.performCommand("evento join " + arena);
            return;
        }
        menus.open(player, new SessionsMenu(SessionsMenu.Mode.JOIN));
    }
}
