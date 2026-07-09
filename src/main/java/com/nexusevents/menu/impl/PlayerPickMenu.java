package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seleccion de un participante vivo para descalificarlo.
 */
public final class PlayerPickMenu extends Menu {

    private final List<String> names;
    private final Map<Integer, String> nameBySlot = new HashMap<>();

    public PlayerPickMenu(List<String> names) {
        this.names = names;
    }

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Descalificar: elegí al jugador";
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void build(Player player, Inventory inventory, MenuService menus) {
        nameBySlot.clear();
        int slot = 10;
        for (String name : names) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            ItemStack head = MenuItems.item(XMaterial.PLAYER_HEAD, "<yellow><bold>" + name,
                    "<red>Click para descalificar");
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta) {
                ((SkullMeta) meta).setOwner(name);
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            nameBySlot.put(slot, name);
            slot++;
        }
        if (names.isEmpty()) {
            inventory.setItem(22, MenuItems.item(XMaterial.BARRIER, "<gray>No hay participantes vivos."));
        }
        inventory.setItem(49, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        if (slot == 49) {
            menus.open(player, new ModerationMenu());
            return;
        }
        String name = nameBySlot.get(slot);
        if (name != null) {
            player.closeInventory();
            player.performCommand("evento disqualify " + name);
        }
    }
}
