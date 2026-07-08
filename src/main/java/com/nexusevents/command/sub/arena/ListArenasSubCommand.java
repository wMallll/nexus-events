package com.nexusevents.command.sub.arena;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.command.SubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/**
 * Subcomando {@code /evento arenas}.
 *
 * <p>Lista todas las arenas registradas con un resumen de cuantos
 * puntos y regiones tiene configurados cada una.</p>
 */
public final class ListArenasSubCommand extends SubCommand {

    private final ArenaManager arenaManager;
    private final MessageService messages;

    public ListArenasSubCommand(ArenaManager arenaManager, MessageService messages) {
        super("arenas", Permissions.ARENA_LIST, "/evento arenas", "listarenas");
        this.arenaManager = arenaManager;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (arenaManager.getAll().isEmpty()) {
            messages.send(sender, "arena.list-empty");
            return;
        }
        messages.send(sender, "arena.list-header",
                Placeholder.unparsed("count", String.valueOf(arenaManager.getAll().size())));
        for (Arena arena : arenaManager.getAll()) {
            messages.send(sender, "arena.list-entry",
                    Placeholder.unparsed("arena", arena.getName()),
                    Placeholder.unparsed("points", String.valueOf(arena.getPoints().size())),
                    Placeholder.unparsed("regions", String.valueOf(arena.getRegions().size())));
        }
    }
}
