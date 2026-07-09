package com.nexusevents.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Holder que vincula un inventario abierto con su {@link Menu}.
 *
 * <p>Permite identificar los inventarios del plugin con
 * {@code instanceof} en los listeners, sin comparar titulos.</p>
 */
public final class MenuHolder implements InventoryHolder {

    private final Menu menu;
    private Inventory inventory;

    public MenuHolder(Menu menu) {
        this.menu = menu;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Menu getMenu() {
        return menu;
    }
}
