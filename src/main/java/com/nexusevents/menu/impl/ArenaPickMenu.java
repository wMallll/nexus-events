package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Seleccion de arena para iniciar un evento concreto.
 */
public final class ArenaPickMenu extends Menu {

    private final String eventId;
    private final Map<Integer, String> arenaBySlot = new HashMap<>();

    public ArenaPickMenu(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Iniciar: elegí la arena";
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        arenaBySlot.clear();
        int slot = 10;
        for (String arena : menus.getArenas().getNames()) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            inventory.setItem(slot, MenuItems.item(XMaterial.MAP, "<yellow><bold>" + arena,
                    "<gray>Click: iniciar acá.",
                    "<dark_gray>Si falta setup, te aviso qué."));
            arenaBySlot.put(slot, arena);
            slot++;
        }
        inventory.setItem(49, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        if (slot == 49) {
            menus.open(player, new EventsMenu());
            return;
        }
        String arena = arenaBySlot.get(slot);
        if (arena != null) {
            player.closeInventory();
            player.performCommand("evento start " + eventId + " " + arena);
        }
    }
}
