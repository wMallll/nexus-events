package com.nexusevents.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

/**
 * Menu de inventario del plugin.
 *
 * <p>Cada instancia representa un menu abierto: define su titulo,
 * tamano y contenido, y maneja los clicks. El {@link MenuService} lo
 * envuelve en un {@link MenuHolder} para identificarlo sin depender del
 * titulo, de forma compatible con todas las versiones.</p>
 */
public abstract class Menu {

    /**
     * Titulo visible del inventario (acepta MiniMessage/legacy).
     *
     * @param player jugador que lo abre.
     * @return titulo crudo.
     */
    public abstract String getTitle(Player player);

    /**
     * Tamano del inventario (multiplo de 9, entre 9 y 54).
     *
     * @return cantidad de slots.
     */
    public abstract int getSize();

    /**
     * Construye el contenido del inventario.
     *
     * @param player    jugador que lo abre.
     * @param inventory inventario a poblar.
     * @param menus     servicio de menus (datos y navegacion).
     */
    public abstract void build(Player player, Inventory inventory, MenuService menus);

    /**
     * Maneja un click sobre el menu (ya cancelado).
     *
     * @param player jugador que clickeo.
     * @param slot   slot clickeado.
     * @param click  tipo de click.
     * @param menus  servicio de menus (datos y navegacion).
     */
    public abstract void onClick(Player player, int slot, ClickType click, MenuService menus);
}
