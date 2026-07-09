package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import com.nexusevents.world.WorldDefinition;
import org.bukkit.Difficulty;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.Locale;

/**
 * Editor en vivo de un mundo del plugin: alternables de mobs, dia
 * permanente, clima, pvp y keep-inventory; cicladores de dificultad y
 * hora; teleport, spawn y borrado con doble confirmacion.
 */
public final class WorldEditMenu extends Menu {

    private static final int[] TIMES = {0, 6000, 13000, 18000};
    private static final Difficulty[] DIFFICULTIES = {
            Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD
    };

    private final String worldName;
    private boolean confirmDelete;

    public WorldEditMenu(String worldName) {
        this.worldName = worldName;
    }

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Mundo: " + worldName;
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        WorldDefinition definition = menus.getWorlds().getDefinition(worldName).orElse(null);
        if (definition == null) {
            inventory.setItem(22, MenuItems.item(XMaterial.BARRIER, "<red>El mundo ya no existe."));
            inventory.setItem(49, MenuItems.back());
            return;
        }
        inventory.setItem(10, MenuItems.toggle(definition.isSpawnMobs(), "<yellow>Aparición de mobs",
                "<gray>Monstruos y animales."));
        inventory.setItem(11, MenuItems.toggle(definition.isAlwaysDay(), "<yellow>Siempre de día",
                "<gray>Hora fija: <white>" + definition.getFixedTime()));
        inventory.setItem(12, MenuItems.toggle(definition.isNoRain(), "<yellow>Sin lluvia",
                "<gray>Clima siempre despejado."));
        inventory.setItem(13, MenuItems.toggle(definition.isPvp(), "<yellow>PvP"));
        inventory.setItem(14, MenuItems.toggle(definition.isKeepInventory(), "<yellow>Keep Inventory",
                "<gray>Conservar inventario al morir."));

        inventory.setItem(20, MenuItems.item(XMaterial.DIAMOND_SWORD,
                "<gold>Dificultad: <yellow>" + definition.getDifficulty().name(),
                "<gray>Click para ciclar", "<gray>PEACEFUL → EASY → NORMAL → HARD"));
        inventory.setItem(21, MenuItems.item(XMaterial.CLOCK,
                "<gold>Hora fija: <yellow>" + definition.getFixedTime(),
                "<gray>Click para ciclar", "<gray>0 → 6000 → 13000 → 18000",
                "<dark_gray>(aplica con 'Siempre de día')"));

        inventory.setItem(30, MenuItems.item(XMaterial.ENDER_PEARL, "<green><bold>Teletransportarme"));
        inventory.setItem(31, MenuItems.item(XMaterial.BEACON, "<green>Fijar spawn del mundo",
                "<gray>Usa tu posición actual", "<gray>(tenés que estar en ese mundo)."));
        inventory.setItem(44, MenuItems.item(XMaterial.LAVA_BUCKET,
                confirmDelete ? "<dark_red><bold>¿SEGURO? Click de nuevo" : "<red><bold>Eliminar mundo",
                "<gray>Borra el mundo y su carpeta", "<gray>del disco DEFINITIVAMENTE."));
        inventory.setItem(49, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        WorldDefinition definition = menus.getWorlds().getDefinition(worldName).orElse(null);
        if (definition == null) {
            menus.open(player, new WorldsMenu());
            return;
        }
        switch (slot) {
            case 10:
                set(player, menus, "spawn-mobs", String.valueOf(!definition.isSpawnMobs()));
                break;
            case 11:
                set(player, menus, "always-day", String.valueOf(!definition.isAlwaysDay()));
                break;
            case 12:
                set(player, menus, "no-rain", String.valueOf(!definition.isNoRain()));
                break;
            case 13:
                set(player, menus, "pvp", String.valueOf(!definition.isPvp()));
                break;
            case 14:
                set(player, menus, "keep-inventory", String.valueOf(!definition.isKeepInventory()));
                break;
            case 20:
                set(player, menus, "difficulty", next(definition.getDifficulty()).name().toLowerCase(Locale.ROOT));
                break;
            case 21:
                set(player, menus, "time", String.valueOf(nextTime(definition.getFixedTime())));
                break;
            case 30:
                player.closeInventory();
                player.performCommand("evento world tp " + worldName);
                break;
            case 31:
                player.performCommand("evento world setspawn " + worldName);
                menus.reopenLater(player, this, 2L);
                break;
            case 44:
                deleteFlow(player, menus);
                break;
            case 49:
                menus.open(player, new WorldsMenu());
                break;
            default:
                break;
        }
    }

    private void set(Player player, MenuService menus, String key, String value) {
        player.performCommand("evento world set " + worldName + " " + key + " " + value);
        menus.reopenLater(player, this, 2L);
    }

    private Difficulty next(Difficulty current) {
        for (int i = 0; i < DIFFICULTIES.length; i++) {
            if (DIFFICULTIES[i] == current) {
                return DIFFICULTIES[(i + 1) % DIFFICULTIES.length];
            }
        }
        return Difficulty.NORMAL;
    }

    private int nextTime(int current) {
        for (int i = 0; i < TIMES.length; i++) {
            if (TIMES[i] == current) {
                return TIMES[(i + 1) % TIMES.length];
            }
        }
        return TIMES[1];
    }

    private void deleteFlow(Player player, MenuService menus) {
        if (!confirmDelete) {
            confirmDelete = true;
            menus.open(player, this);
            return;
        }
        player.closeInventory();
        player.performCommand("evento world delete " + worldName + " confirm");
        menus.reopenLater(player, new WorldsMenu(), 3L);
    }
}
