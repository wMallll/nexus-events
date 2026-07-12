package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.event.GameEvent;
import com.nexusevents.lobby.LobbyProtectionSettings;
import com.nexusevents.lobby.LobbyZoneService;
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
 * Editor de las zonas de lobby de una arena, con pestanas de alcance
 * (General + un evento por pestana) y un toggle en vivo por cada
 * proteccion del alcance activo.
 */
public final class LobbyZoneMenu extends Menu {

    private static final String[] TOGGLE_KEYS = {
            "no-damage", "no-hunger", "no-pvp", "no-mob-spawn", "no-explosions",
            "no-block-break", "no-block-place", "no-fire", "no-item-drop",
            "no-item-pickup", "no-interact"
    };
    private static final int[] TOGGLE_SLOTS = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23, 28};

    private final String arenaName;
    private final String scope;
    private final Map<Integer, String> toggleBySlot = new HashMap<>();
    private final Map<Integer, String> scopeBySlot = new HashMap<>();

    public LobbyZoneMenu(String arenaName) {
        this(arenaName, LobbyZoneService.GENERAL_SCOPE);
    }

    public LobbyZoneMenu(String arenaName, String scope) {
        this.arenaName = arenaName;
        this.scope = scope;
    }

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Zonas de lobby: " + arenaName;
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        toggleBySlot.clear();
        scopeBySlot.clear();

        // Pestanas de alcance: General + un evento por pestana.
        int tabSlot = 0;
        addScopeTab(inventory, menus, tabSlot++, LobbyZoneService.GENERAL_SCOPE, XMaterial.MAP);
        for (GameEvent event : menus.getEvents().getTypes()) {
            if (tabSlot > 8) {
                break;
            }
            addScopeTab(inventory, menus, tabSlot++, event.getId(), XMaterial.PAPER);
        }

        LobbyZoneService.LobbyZone zone = menus.getLobbyZones().getZone(arenaName, scope).orElse(null);
        inventory.setItem(31, MenuItems.item(XMaterial.SHIELD,
                "<gold><bold>" + scopeLabel(menus, scope),
                "<gray>Región: " + (zone != null
                        ? "<yellow>" + zone.getRegion().describe() : "<red>sin definir"),
                "<dark_gray>Marcá pos1 y pos2 para activarla.",
                "<dark_gray>Prioridad: zona del evento > general."));

        if (zone != null) {
            for (int i = 0; i < TOGGLE_KEYS.length; i++) {
                String key = TOGGLE_KEYS[i];
                inventory.setItem(TOGGLE_SLOTS[i], MenuItems.toggle(
                        zone.getSettings().isEnabled(key), "<yellow>" + labelOf(key)));
                toggleBySlot.put(TOGGLE_SLOTS[i], key);
            }
        }

        inventory.setItem(38, MenuItems.item(XMaterial.SMOOTH_STONE, "<green>Región: Pos 1",
                "<gray>Esquina 1 en tu posición."));
        inventory.setItem(39, MenuItems.item(XMaterial.STONE_BRICKS, "<green>Región: Pos 2",
                "<gray>Esquina 2 (se guarda con ambas)."));
        inventory.setItem(40, MenuItems.item(XMaterial.TNT, "<red>Quitar esta zona"));
        inventory.setItem(49, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    private void addScopeTab(Inventory inventory, MenuService menus, int slot,
                             String tabScope, XMaterial icon) {
        boolean active = tabScope.equals(scope);
        boolean defined = menus.getLobbyZones().getZone(arenaName, tabScope).isPresent();
        inventory.setItem(slot, MenuItems.item(
                active ? XMaterial.LIME_STAINED_GLASS_PANE : icon,
                (active ? "<green><bold>▶ " : "<yellow>") + scopeLabel(menus, tabScope),
                "<gray>Zona: " + (defined ? "<green>definida" : "<red>sin definir"),
                active ? "<dark_gray>Alcance actual" : "<yellow>Click para editar"));
        scopeBySlot.put(slot, tabScope);
    }

    private String scopeLabel(MenuService menus, String tabScope) {
        if (LobbyZoneService.GENERAL_SCOPE.equals(tabScope)) {
            return menus.getMessages().rawOr("lobbyzone.scope-general", "General de la arena");
        }
        return menus.getMessages().rawOr("event.names." + tabScope, tabScope);
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
        String tabScope = scopeBySlot.get(slot);
        if (tabScope != null && !tabScope.equals(scope)) {
            menus.open(player, new LobbyZoneMenu(arenaName, tabScope));
            return;
        }
        String toggle = toggleBySlot.get(slot);
        if (toggle != null) {
            boolean enabled = menus.getLobbyZones().getZone(arenaName, scope)
                    .map(zone -> zone.getSettings().isEnabled(toggle)).orElse(false);
            run(player, menus, "set " + toggle + " " + !enabled);
            return;
        }
        switch (slot) {
            case 38:
                run(player, menus, "pos1");
                break;
            case 39:
                run(player, menus, "pos2");
                break;
            case 40:
                run(player, menus, "removeregion");
                break;
            case 49:
                menus.open(player, new ArenaEditMenu(arenaName));
                break;
            default:
                break;
        }
    }

    private void run(Player player, MenuService menus, String action) {
        player.performCommand("evento lobbyzone " + scope + " " + arenaName + " " + action);
        menus.reopenLater(player, this, 2L);
    }
}
