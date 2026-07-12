package com.nexusevents.menu.impl;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.event.EventSession;
import com.nexusevents.menu.Menu;
import com.nexusevents.menu.MenuItems;
import com.nexusevents.menu.MenuService;
import com.nexusevents.permission.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Herramientas de moderacion: teletransportar eliminados, palo
 * descalificador, descalificacion por seleccion y modo torneo.
 */
public final class ModerationMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "<dark_gray>Moderación";
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public void build(Player player, Inventory inventory, MenuService menus) {
        if (player.hasPermission(Permissions.TP_DEAD)) {
            inventory.setItem(10, MenuItems.item(XMaterial.ENDER_EYE, "<aqua><bold>Traer eliminados",
                    "<gray>Teletransporta a TODOS los", "<gray>eliminados hasta tu posición,",
                    "<gray>sin importar el mundo."));
        }
        if (player.hasPermission(Permissions.DQ_STICK)) {
            inventory.setItem(12, MenuItems.item(XMaterial.STICK, "<red><bold>Palo Descalificador",
                    "<gray>Te da el palo: golpeá a un", "<gray>participante para descalificarlo."));
        }
        if (player.hasPermission(Permissions.DISQUALIFY)) {
            inventory.setItem(14, MenuItems.item(XMaterial.NAME_TAG, "<red><bold>Descalificar...",
                    "<gray>Elegí al jugador de la lista."));
        }
        if (player.hasPermission(Permissions.COLLISION)) {
            boolean disabled = menus.getCollision().isDisabled();
            inventory.setItem(22, MenuItems.item(
                    disabled ? XMaterial.SLIME_BALL : XMaterial.LEAD,
                    "<gold><bold>Colisión del servidor",
                    "<gray>Estado: " + (disabled
                            ? "<red>DESACTIVADA <gray>(se atraviesan)" : "<green>activada"),
                    "<yellow>Click: alternar en TODO el servidor"));
        }
        if (player.hasPermission(Permissions.LOCKOUT)) {
            boolean enabled = menus.getLockouts().isEnabled();
            inventory.setItem(16, MenuItems.item(
                    enabled ? XMaterial.REDSTONE_TORCH : XMaterial.LEVER,
                    "<gold><bold>Modo torneo",
                    "<gray>Estado: " + (enabled ? "<green>ACTIVADO" : "<red>desactivado"),
                    "<gray>Bloqueados: <yellow>" + menus.getLockouts().getLockedCount(),
                    "<yellow>Click: alternar on/off",
                    "<yellow>Shift + click der: limpiar registro"));
        }
        inventory.setItem(31, MenuItems.back());
        MenuItems.fillBorder(inventory);
    }

    @Override
    public void onClick(Player player, int slot, ClickType click, MenuService menus) {
        switch (slot) {
            case 10:
                player.performCommand("evento tpdead");
                break;
            case 12:
                player.performCommand("evento dqstick");
                break;
            case 14:
                openPlayerPicker(player, menus);
                break;
            case 16:
                lockoutFlow(player, click, menus);
                break;
            case 22:
                if (player.hasPermission(Permissions.COLLISION)) {
                    player.performCommand("evento collision "
                            + (menus.getCollision().isDisabled() ? "on" : "off"));
                    menus.reopenLater(player, new ModerationMenu(), 2L);
                }
                break;
            case 31:
                menus.open(player, new MainMenu());
                break;
            default:
                break;
        }
    }

    private void openPlayerPicker(Player player, MenuService menus) {
        List<String> alive = new ArrayList<>();
        for (EventSession session : menus.getEvents().getSessions()) {
            for (UUID id : session.getAlive()) {
                Player participant = Bukkit.getPlayer(id);
                if (participant != null) {
                    alive.add(participant.getName());
                }
            }
        }
        menus.open(player, new PlayerPickMenu(alive));
    }

    private void lockoutFlow(Player player, ClickType click, MenuService menus) {
        if (click == ClickType.SHIFT_RIGHT) {
            player.performCommand("evento lockout clear");
        } else {
            player.performCommand("evento lockout " + (menus.getLockouts().isEnabled() ? "off" : "on"));
        }
        menus.reopenLater(player, new ModerationMenu(), 2L);
    }
}
