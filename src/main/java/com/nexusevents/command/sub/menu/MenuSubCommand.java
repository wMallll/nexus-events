package com.nexusevents.command.sub.menu;

import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.menu.MenuService;
import com.nexusevents.menu.impl.MainMenu;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import org.bukkit.entity.Player;

/**
 * Subcomando {@code /evento menu}: abre el menu principal del plugin,
 * desde el que se accede a TODAS las funciones sin escribir comandos.
 */
public final class MenuSubCommand extends PlayerSubCommand {

    private final MenuService menus;

    public MenuSubCommand(MenuService menus, MessageService messages) {
        super(messages, "menu", Permissions.MENU, "/evento menu", "gui", "menus");
        this.menus = menus;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        menus.open(player, new MainMenu());
    }
}
