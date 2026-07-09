package com.nexusevents.menu;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabrica de items para los menus: materiales multi-version via
 * XSeries y textos MiniMessage/legacy convertidos a legacy.
 */
public final class MenuItems {

    private MenuItems() {
    }

    /**
     * Crea un item de menu.
     *
     * @param material material multi-version.
     * @param name     nombre (MiniMessage/legacy).
     * @param lore     lineas de descripcion (MiniMessage/legacy).
     * @return item construido.
     */
    @SuppressWarnings("deprecation")
    public static ItemStack item(XMaterial material, String name, String... lore) {
        ItemStack stack = material.parseItem();
        if (stack == null) {
            stack = new ItemStack(Material.STONE);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.toLegacy(name));
            if (lore.length > 0) {
                List<String> lines = new ArrayList<>(lore.length);
                for (String line : lore) {
                    lines.add(TextUtil.toLegacy(line));
                }
                meta.setLore(lines);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Item para alternables: muestra el estado con color y material.
     *
     * @param enabled estado actual.
     * @param name    nombre base.
     * @param lore    descripcion adicional.
     * @return item lime (activado) o gris (desactivado).
     */
    public static ItemStack toggle(boolean enabled, String name, String... lore) {
        String state = enabled ? "<green>ACTIVADO" : "<red>desactivado";
        String[] lines = new String[lore.length + 2];
        System.arraycopy(lore, 0, lines, 0, lore.length);
        lines[lore.length] = "<gray>Estado: " + state;
        lines[lore.length + 1] = "<yellow>Click para alternar";
        return item(enabled ? XMaterial.LIME_DYE : XMaterial.GRAY_DYE, name, lines);
    }

    /**
     * Rellena el borde del inventario con paneles decorativos.
     *
     * @param inventory inventario destino.
     */
    public static void fillBorder(Inventory inventory) {
        ItemStack pane = item(XMaterial.GRAY_STAINED_GLASS_PANE, "<dark_gray> ");
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            boolean border = row == 0 || row == size / 9 - 1 || column == 0 || column == 8;
            if (border && inventory.getItem(slot) == null) {
                inventory.setItem(slot, pane);
            }
        }
    }

    /**
     * Item estandar de volver.
     *
     * @return flecha de volver.
     */
    public static ItemStack back() {
        return item(XMaterial.ARROW, "<yellow>« Volver");
    }

    /**
     * Item estandar de cerrar.
     *
     * @return barrera de cerrar.
     */
    public static ItemStack close() {
        return item(XMaterial.BARRIER, "<red>Cerrar");
    }
}
