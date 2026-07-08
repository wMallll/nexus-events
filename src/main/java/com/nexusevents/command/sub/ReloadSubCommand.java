package com.nexusevents.command.sub;

import com.nexusevents.command.SubCommand;
import com.nexusevents.manager.ManagerRegistry;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/**
 * Subcomando {@code /evento reload}.
 *
 * <p>Recarga en caliente todos los managers que implementan
 * {@code Reloadable} a traves del {@link ManagerRegistry} e informa el
 * tiempo total de la operacion.</p>
 */
public final class ReloadSubCommand extends SubCommand {

    private final ManagerRegistry managerRegistry;
    private final MessageService messages;

    public ReloadSubCommand(ManagerRegistry managerRegistry, MessageService messages) {
        super("reload", Permissions.RELOAD, "/evento reload", "recargar", "rl");
        this.managerRegistry = managerRegistry;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        long elapsed = managerRegistry.reloadAll();
        messages.send(sender, "general.reload-success",
                Placeholder.unparsed("time", String.valueOf(elapsed)));
    }
}
