package com.nexusevents.command.sub;

import com.nexusevents.command.CommandManager;
import com.nexusevents.command.SubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/**
 * Subcomando {@code /evento help}.
 *
 * <p>Genera la ayuda dinamicamente a partir de los subcomandos
 * registrados, mostrando unicamente aquellos para los que el emisor
 * tiene permiso. Los textos provienen de {@code messages.yml}.</p>
 */
public final class HelpSubCommand extends SubCommand {

    private final CommandManager commandManager;
    private final MessageService messages;

    public HelpSubCommand(CommandManager commandManager, MessageService messages) {
        super("help", Permissions.USE, "/evento help", "ayuda", "?");
        this.commandManager = commandManager;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        messages.send(sender, "help.header");
        for (SubCommand subCommand : commandManager.getSubCommands()) {
            String permission = subCommand.getPermission();
            if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
                continue;
            }
            messages.send(sender, "help.entry",
                    Placeholder.unparsed("usage", subCommand.getUsage()),
                    Placeholder.parsed("description", messages.raw("help.descriptions." + subCommand.getName())));
        }
        messages.send(sender, "help.footer");
    }
}
