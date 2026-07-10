package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.arena.Region;
import com.nexusevents.lobby.LobbyProtectionSettings;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Editor completo del lobby global: spawn, region protegida, altura
 * minima y un toggle en vivo por cada proteccion.
 */
public final class MainLobbyMenu extends Menu {

    private static final String[] TOGGLE_KEYS = {
            "no-damage", "no-hunger", "no-pvp", "no-mob-spawn", "no-explosions",
            "no-block-break", "no-block-place", "no-fire", "no-item-drop",
            "no-item-pickup", "no-interact"
    };
    private static final int[] TOGGLE_SLOTS = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23, 28};

    private final Map<Integer, String> toggleBySlot = new HashMap<>();

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Lobby Global";
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        toggleBySlot.clear();
        LobbyProtectionSettings protection = menus.getMainLobby().getProtection();
        String region = menus.getMainLobby().getRegion().map(Region::describe).orElse("sin definir");
        String minY = menus.getMainLobby().getMinY().map(y -> "Y=" + y).orElse("sin definir");

        inventory.setItem(4, MenuItems.item(XMaterial.BEACON, "<gold><bold>Lobby Global",
                "<gray>Spawn: " + (menus.getMainLobby().getLobby().isPresent()
                        ? "<green>definido" : "<red>sin definir"),
                "<gray>Región: <yellow>" + region,
                "<gray>Altura mínima: <yellow>" + minY,
                "<dark_gray>Las protecciones aplican dentro",
                "<dark_gray>de la región (participantes exentos)."));

        for (int i = 0; i < TOGGLE_KEYS.length; i++) {
            String key = TOGGLE_KEYS[i];
            inventory.setItem(TOGGLE_SLOTS[i], MenuItems.toggle(protection.isEnabled(key),
                    "<yellow>" + labelOf(key)));
            toggleBySlot.put(TOGGLE_SLOTS[i], key);
        }

        inventory.setItem(37, MenuItems.item(XMaterial.COMPASS, "<aqua>Fijar Spawn acá",
                "<gray>Primer ingreso de los jugadores."));
        inventory.setItem(38, MenuItems.item(XMaterial.SMOOTH_STONE, "<green>Región: Pos 1",
                "<gray>Esquina 1 en tu posición."));
        inventory.setItem(39, MenuItems.item(XMaterial.STONE_BRICKS, "<green>Región: Pos 2",
                "<gray>Esquina 2 (se guarda con ambas)."));
        inventory.setItem(40, MenuItems.item(XMaterial.TNT, "<red>Quitar región"));
        inventory.setItem(41, MenuItems.item(XMaterial.IRON_BARS, "<aqua>Altura mínima acá",
                "<gray>Quien caiga debajo vuelve al lobby."));
        inventory.setItem(49, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    private String labelOf(String key) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "no-damage":
                return "Sin daño (vida intocable)";
            case "no-hunger":
                return "Sin hambre";
            case "no-pvp":
                return "Sin PvP";
            case "no-mob-spawn":
                return "Sin aparición de mobs";
            case "no-explosions":
                return "Sin explosiones";
            case "no-block-break":
                return "Sin romper bloques";
            case "no-block-place":
                return "Sin colocar bloques";
            case "no-fire":
                return "Sin fuego";
            case "no-item-drop":
                return "Sin tirar items";
            case "no-item-pickup":
                return "Sin recoger items";
            case "no-interact":
                return "Sin interactuar";
            default:
                return key;
        }
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        String toggle = toggleBySlot.get(slot);
        if (toggle != null) {
            boolean enabled = menus.getMainLobby().getProtection().isEnabled(toggle);
            player.performCommand("evento mainlobby set " + toggle + " " + !enabled);
            menus.reopenLater(player, this, 2L);
            return;
        }
        switch (slot) {
            case 37:
                player.performCommand("evento setmainlobby");
                menus.reopenLater(player, this, 2L);
                break;
            case 38:
                player.performCommand("evento mainlobby pos1");
                menus.reopenLater(player, this, 2L);
                break;
            case 39:
                player.performCommand("evento mainlobby pos2");
                menus.reopenLater(player, this, 2L);
                break;
            case 40:
                player.performCommand("evento mainlobby removeregion");
                menus.reopenLater(player, this, 2L);
                break;
            case 41:
                player.performCommand("evento mainlobby setminy");
                menus.reopenLater(player, this, 2L);
                break;
            case 49:
                menus.open(player, new MainMenu());
                break;
            default:
                break;
        }
    }
}
