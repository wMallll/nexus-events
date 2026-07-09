package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.event.EventSession;
import com.nexusevents.event.EventState;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Lista de sesiones activas, en modo union o detencion.
 */
public final class SessionsMenu extends Menu {

    /** Accion aplicada al clickear una sesion. */
    public enum Mode { JOIN, STOP }

    private final Mode mode;
    private final Map<Integer, String> arenaBySlot = new HashMap<>();

    public SessionsMenu(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String getTitle(Player player) {
        return mode == Mode.JOIN ? "<dark_gray>Unirse a un evento" : "<dark_gray>Detener un evento";
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        arenaBySlot.clear();
        int slot = 10;
        for (EventSession session : menus.getEvents().getSessions()) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            String arena = session.getArena().getName();
            inventory.setItem(slot, MenuItems.item(iconOf(session.getState()),
                    "<yellow><bold>" + arena,
                    "<gray>Evento: <white>" + menus.getMessages().rawOr(
                            "event.names." + session.getType().getId(), session.getType().getId()),
                    "<gray>Estado: <white>" + menus.getMessages().rawOr(
                            "event.states." + session.getState().getKey(), session.getState().getKey()),
                    "<gray>Vivos: <green>" + session.getAliveCount()
                            + " <gray>| Espectadores: <red>" + session.getEliminatedCount(),
                    mode == Mode.JOIN ? "<yellow>Click para unirte" : "<red>Click para detener"));
            arenaBySlot.put(slot, arena);
            slot++;
        }
        inventory.setItem(49, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    private XMaterial iconOf(EventState state) {
        if (state == EventState.WAITING) {
            return XMaterial.LIME_WOOL;
        }
        if (state == EventState.COUNTDOWN) {
            return XMaterial.YELLOW_WOOL;
        }
        return XMaterial.RED_WOOL;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        if (slot == 49) {
            menus.open(player, new EventsMenu());
            return;
        }
        String arena = arenaBySlot.get(slot);
        if (arena == null) {
            return;
        }
        player.closeInventory();
        player.performCommand(mode == Mode.JOIN ? "evento join " + arena : "evento stop " + arena);
    }
}
