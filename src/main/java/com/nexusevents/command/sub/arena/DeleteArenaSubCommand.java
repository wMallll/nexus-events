package com.nexusevents.command.sub.arena;

import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.SubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Subcomando {@code /evento deletearena (nombre) confirm}.
 *
 * <p>Elimina una arena de forma permanente. Requiere confirmacion
 * explicita y limpia toda sesion de setup que la referenciara.</p>
 */
public final class DeleteArenaSubCommand extends SubCommand {

    private static final String CONFIRM_KEYWORD = "confirm";

    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final MessageService messages;
    private final SoundService sounds;

    public DeleteArenaSubCommand(ArenaManager arenaManager, SetupSessionService sessions,
                                 MessageService messages, SoundService sounds) {
        super("deletearena", Permissions.ARENA_DELETE, "/evento deletearena (nombre) confirm", "eliminar");
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            messages.send(sender, "arena.delete-usage");
            sounds.play(sender, "command-error");
            return;
        }
        String name = args[0];
        if (!arenaManager.exists(name)) {
            messages.send(sender, "arena.not-found", Placeholder.unparsed("arena", name));
            sounds.play(sender, "command-error");
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase(CONFIRM_KEYWORD)) {
            messages.send(sender, "arena.delete-confirm", Placeholder.unparsed("arena", name));
            return;
        }
        arenaManager.delete(name);
        sessions.clearForArena(name);
        messages.send(sender, "arena.deleted", Placeholder.unparsed("arena", name));
        sounds.play(sender, "command-success");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterNames(args[0]);
        }
        if (args.length == 2) {
            return Collections.singletonList(CONFIRM_KEYWORD);
        }
        return Collections.emptyList();
    }

    private List<String> filterNames(String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (String name : arenaManager.getNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}
