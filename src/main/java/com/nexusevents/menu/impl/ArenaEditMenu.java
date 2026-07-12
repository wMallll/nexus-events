package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

/**
 * Editor de una arena: puntos generales, circulo con selector de
 * radio, fragmentos del parkour y region de pixel party.
 *
 * <p>Todos los botones de posicion usan la ubicacion ACTUAL del
 * jugador y ejecutan el comando real equivalente. Las variantes por
 * evento (setspawn pixel-party, etc.) siguen disponibles por
 * comandos.</p>
 */
public final class ArenaEditMenu extends Menu {

    private final String arenaName;
    private int radius = 50;
    private boolean confirmDelete;

    public ArenaEditMenu(String arenaName) {
        this.arenaName = arenaName;
    }

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Editar: " + arenaName;
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        Arena arena = menus.getArenas().get(arenaName).orElse(null);
        int fragments = arena == null ? 0 : countFragments(arena);
        inventory.setItem(4, MenuItems.item(XMaterial.PAPER, "<yellow><bold>" + arenaName,
                "<gray>Puntos: <white>" + (arena == null ? 0 : arena.getPoints().size())
                        + " <gray>| Regiones: <white>" + (arena == null ? 0 : arena.getRegions().size()),
                "<gray>Fragmentos de parkour: <white>" + fragments,
                "<dark_gray>Los botones usan tu posición actual."));

        inventory.setItem(19, MenuItems.item(XMaterial.RED_BED, "<aqua>Fijar Lobby",
                "<gray>Espera del evento (general)."));
        inventory.setItem(20, MenuItems.item(XMaterial.COMPASS, "<aqua>Fijar Spawn",
                "<gray>Punto de partida (general)."));
        inventory.setItem(21, MenuItems.item(XMaterial.IRON_SWORD, "<aqua>Spawn del Cazador",
                "<gray>Escondete si puedes."));

        inventory.setItem(22, MenuItems.item(XMaterial.IRON_BARS, "<aqua>Altura mínima acá",
                "<gray>Usa tu Y actual: quien caiga",
                "<gray>debajo vuelve al lobby.",
                "<dark_gray>Por evento: /evento setminy (evento)"));
        inventory.setItem(23, MenuItems.item(XMaterial.SLIME_BALL, "<gold>Radio del círculo: <yellow>" + radius,
                "<gray>Click izq: <white>+5 <gray>| der: <white>-5",
                "<gray>Shift: <white>±25", "<gray>Rango: 3 - 500"));
        inventory.setItem(24, MenuItems.item(XMaterial.SNOWBALL, "<gold>Fijar Círculo acá",
                "<gray>Centro en tu posición,", "<gray>radio <yellow>" + radius + "</yellow> bloques."));

        inventory.setItem(28, MenuItems.item(XMaterial.WHITE_WOOL, "<light_purple>Pixel Party: Pos 1",
                "<gray>Esquina 1 de la plataforma", "<gray>(al nivel de los bloques)."));
        inventory.setItem(29, MenuItems.item(XMaterial.ORANGE_WOOL, "<light_purple>Pixel Party: Pos 2",
                "<gray>Esquina 2 de la plataforma."));

        inventory.setItem(31, MenuItems.item(XMaterial.SMOOTH_STONE, "<green>Parkour: Pos 1",
                "<gray>Esquina 1 del fragmento."));
        inventory.setItem(32, MenuItems.item(XMaterial.STONE_BRICKS, "<green>Parkour: Pos 2",
                "<gray>Esquina 2 del fragmento."));
        inventory.setItem(33, MenuItems.item(XMaterial.EMERALD, "<green>Parkour: Agregar fragmento",
                "<gray>Guarda pos1+pos2 como isla #" + (fragments + 1) + "."));
        inventory.setItem(34, MenuItems.item(XMaterial.REDSTONE, "<red>Parkour: Quitar último",
                "<gray>Elimina el fragmento #" + fragments + "."));
        inventory.setItem(35, MenuItems.item(XMaterial.TNT, "<red>Parkour: Limpiar todo",
                "<gray>Borra los " + fragments + " fragmentos."));

        inventory.setItem(38, MenuItems.item(XMaterial.SHIELD, "<gold>Zona de lobby",
                "<gray>Región protegida de esta arena:",
                "<gray>daño, hambre, PvP, colisión y más."));
        inventory.setItem(40, MenuItems.item(XMaterial.BOOK, "<green><bold>Guardar arenas"));
        inventory.setItem(44, MenuItems.item(XMaterial.LAVA_BUCKET,
                confirmDelete ? "<dark_red><bold>¿SEGURO? Click de nuevo" : "<red><bold>Eliminar arena",
                "<gray>Borra la arena definitivamente."));
        inventory.setItem(49, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    private int countFragments(Arena arena) {
        int index = 1;
        while (arena.hasRegion(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + index)) {
            index++;
        }
        return index - 1;
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        switch (slot) {
            case 19:
                run(player, menus, "evento setlobby");
                break;
            case 20:
                run(player, menus, "evento setspawn");
                break;
            case 21:
                run(player, menus, "evento sethunterspawn");
                break;
            case 22:
                run(player, menus, "evento setminy");
                break;
            case 23:
                adjustRadius(click);
                menus.open(player, this);
                break;
            case 24:
                run(player, menus, "evento setcircle " + radius);
                break;
            case 28:
                run(player, menus, "evento setpixelparty 1");
                break;
            case 29:
                run(player, menus, "evento setpixelparty 2");
                break;
            case 31:
                run(player, menus, "evento parkour pos1");
                break;
            case 32:
                run(player, menus, "evento parkour pos2");
                break;
            case 33:
                run(player, menus, "evento parkour add");
                break;
            case 34:
                removeLastFragment(player, menus);
                break;
            case 35:
                run(player, menus, "evento parkour clear");
                break;
            case 38:
                menus.open(player, new LobbyZoneMenu(arenaName));
                break;
            case 40:
                run(player, menus, "evento save");
                break;
            case 44:
                deleteFlow(player, menus);
                break;
            case 49:
                menus.open(player, new ArenasMenu());
                break;
            default:
                break;
        }
    }

    private void run(Player player, MenuService menus, String command) {
        player.performCommand("evento select " + arenaName);
        player.performCommand(command);
        menus.reopenLater(player, this, 2L);
    }

    private void adjustRadius(ClickType click) {
        int step = click.isShiftClick() ? 25 : 5;
        if (click.isRightClick()) {
            step = -step;
        }
        radius = Math.max(3, Math.min(500, radius + step));
    }

    private void removeLastFragment(Player player, MenuService menus) {
        Arena arena = menus.getArenas().get(arenaName).orElse(null);
        int fragments = arena == null ? 0 : countFragments(arena);
        if (fragments > 0) {
            run(player, menus, "evento parkour remove " + fragments);
        }
    }

    private void deleteFlow(Player player, MenuService menus) {
        if (!confirmDelete) {
            confirmDelete = true;
            menus.open(player, this);
            return;
        }
        player.performCommand("evento deletearena " + arenaName + " confirm");
        menus.reopenLater(player, new ArenasMenu(), 2L);
    }
}
