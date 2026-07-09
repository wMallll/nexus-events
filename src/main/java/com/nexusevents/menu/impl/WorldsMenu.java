package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import com.nexusevents.world.WorldDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Listado de mundos del plugin: creacion con nombre por chat y acceso
 * al editor de cada mundo.
 */
public final class WorldsMenu extends Menu {

    private final Map<Integer, String> worldBySlot = new HashMap<>();

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Mundos";
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        worldBySlot.clear();
        inventory.setItem(4, MenuItems.item(XMaterial.NETHER_STAR, "<green><bold>Crear mundo vacío",
                "<gray>Se te pedirá el nombre por chat", "<gray>y después el entorno."));
        int slot = 10;
        for (WorldDefinition definition : menus.getWorlds().getDefinitions()) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            boolean loaded = Bukkit.getWorld(definition.getName()) != null;
            inventory.setItem(slot, MenuItems.item(
                    loaded ? XMaterial.GRASS_BLOCK : XMaterial.BEDROCK,
                    "<yellow><bold>" + definition.getName(),
                    "<gray>Entorno: <white>" + definition.getEnvironment().name(),
                    "<gray>Estado: " + (loaded ? "<green>cargado" : "<red>descargado"),
                    "<yellow>Click: configurar"));
            worldBySlot.put(slot, definition.getName());
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
            menus.prompt(player, "menu.prompt-world-name",
                    name -> menus.open(player, new WorldEnvironmentMenu(name)));
            return;
        }
        String world = worldBySlot.get(slot);
        if (world != null) {
            menus.open(player, new WorldEditMenu(world));
        }
    }
}
