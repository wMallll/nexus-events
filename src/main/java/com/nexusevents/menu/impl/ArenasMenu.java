package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.arena.Arena;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Listado de arenas: click para abrir su editor, mas creacion de
 * arenas nuevas con nombre por chat.
 */
public final class ArenasMenu extends Menu {

    private final Map<Integer, String> arenaBySlot = new HashMap<>();

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Arenas";
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        arenaBySlot.clear();
        inventory.setItem(4, MenuItems.item(XMaterial.NETHER_STAR, "<green><bold>Crear arena",
                "<gray>Se te pedirá el nombre por chat."));
        int slot = 10;
        for (Arena arena : menus.getArenas().getAll()) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            inventory.setItem(slot, MenuItems.item(XMaterial.MAP, "<yellow><bold>" + arena.getName(),
                    "<gray>Puntos: <white>" + arena.getPoints().size()
                            + " <gray>| Regiones: <white>" + arena.getRegions().size(),
                    "<yellow>Click: editar esta arena"));
            arenaBySlot.put(slot, arena.getName());
            slot++;
        }
        inventory.setItem(49, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        if (slot == 49) {
            menus.open(player, new MainMenu());
            return;
        }
        if (slot == 4) {
            menus.prompt(player, "menu.prompt-arena-name", name -> {
                player.performCommand("evento createarena " + name);
                menus.reopenLater(player, new ArenaEditMenu(name), 3L);
            });
            return;
        }
        String arena = arenaBySlot.get(slot);
        if (arena != null) {
            player.performCommand("evento select " + arena);
            menus.open(player, new ArenaEditMenu(arena));
        }
    }
}
