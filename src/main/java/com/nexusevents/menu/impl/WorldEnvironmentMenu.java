package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

/**
 * Eleccion del entorno para un mundo nuevo (afecta cielo y niebla).
 */
public final class WorldEnvironmentMenu extends Menu {

    private final String worldName;

    public WorldEnvironmentMenu(String worldName) {
        this.worldName = worldName;
    }

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Entorno de: " + worldName;
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        inventory.setItem(11, MenuItems.item(XMaterial.GRASS_BLOCK, "<green><bold>Normal",
                "<gray>Cielo diurno clásico."));
        inventory.setItem(13, MenuItems.item(XMaterial.NETHERRACK, "<red><bold>Nether",
                "<gray>Cielo y niebla del Nether."));
        inventory.setItem(15, MenuItems.item(XMaterial.END_STONE, "<light_purple><bold>End",
                "<gray>Cielo oscuro del End."));
        inventory.setItem(22, MenuItems.item(XMaterial.BARRIER, "<red>Cancelar"));
        MenuItems.fillBorder(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        switch (slot) {
            case 11:
                create(player, "normal");
                break;
            case 13:
                create(player, "nether");
                break;
            case 15:
                create(player, "end");
                break;
            case 22:
                menus.open(player, new WorldsMenu());
                break;
            default:
                break;
        }
    }

    private void create(Player player, String environment) {
        player.closeInventory();
        player.performCommand("evento world create " + worldName + " " + environment);
    }
}
