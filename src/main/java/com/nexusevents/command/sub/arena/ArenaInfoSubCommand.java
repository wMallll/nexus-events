package com.nexusevents.command.sub.arena;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.Region;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.SubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Subcomando {@code /evento arena [nombre]}.
 *
 * <p>Muestra el estado de setup de una arena: cada punto con sus
 * coordenadas y cada region con sus dimensiones. Si no se indica
 * nombre, usa la arena seleccionada por el administrador.</p>
 */
public final class ArenaInfoSubCommand extends SubCommand {

    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final MessageService messages;

    public ArenaInfoSubCommand(ArenaManager arenaManager, SetupSessionService sessions,
                               MessageService messages) {
        super("arena", Permissions.ARENA_EDIT, "/evento arena [nombre]");
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Arena arena = resolveTarget(sender, args);
        if (arena == null) {
            if (args.length >= 1) {
                messages.send(sender, "arena.not-found", Placeholder.unparsed("arena", args[0]));
            } else {
                messages.send(sender, "arena.none-selected");
            }
            return;
        }

        messages.send(sender, "arena.info-header", Placeholder.unparsed("arena", arena.getName()));
        if (arena.getPoints().isEmpty() && arena.getRegions().isEmpty()
                && arena.getProperties().isEmpty()) {
            messages.send(sender, "arena.info-empty");
            return;
        }
        for (Map.Entry<String, ArenaLocation> entry : arena.getPoints().entrySet()) {
            messages.send(sender, "arena.info-point",
                    Placeholder.parsed("point", displayName("arena.point-names.", entry.getKey())),
                    Placeholder.unparsed("location", entry.getValue().describe()));
        }
        for (Map.Entry<String, Region> entry : arena.getRegions().entrySet()) {
            messages.send(sender, "arena.info-region",
                    Placeholder.parsed("region", displayName("arena.region-names.", entry.getKey())),
                    Placeholder.unparsed("dimensions", entry.getValue().describe()));
        }
        for (Map.Entry<String, String> entry : arena.getProperties().entrySet()) {
            messages.send(sender, "arena.info-property",
                    Placeholder.parsed("property", displayName("arena.property-names.", entry.getKey())),
                    Placeholder.unparsed("value", entry.getValue()));
        }
    }

    private Arena resolveTarget(CommandSender sender, String[] args) {
        if (args.length >= 1) {
            return arenaManager.get(args[0]).orElse(null);
        }
        if (sender instanceof Player) {
            return sessions.resolveSelected((Player) sender, arenaManager).orElse(null);
        }
        return null;
    }

    private String displayName(String basePath, String key) {
        int separator = key.indexOf('_');
        if (separator > 0) {
            String base = key.substring(0, separator);
            String eventId = key.substring(separator + 1);
            return messages.rawOr(basePath + base, base)
                    + " <dark_gray>(<gray>" + messages.rawOr("event.names." + eventId, eventId)
                    + "<dark_gray>)";
        }
        return messages.rawOr(basePath + key, key);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String lowered = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (String name : arenaManager.getNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}
