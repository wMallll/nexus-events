package com.nexusevents.command.sub.arena;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Subcomando {@code /evento select (nombre)}.
 *
 * <p>Fija que arena esta editando el administrador; el resto de los
 * comandos de setup operan sobre la arena seleccionada.</p>
 */
public final class SelectArenaSubCommand extends PlayerSubCommand {

    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final MessageService messages;
    private final SoundService sounds;

    public SelectArenaSubCommand(ArenaManager arenaManager, SetupSessionService sessions,
                                 MessageService messages, SoundService sounds) {
        super(messages, "select", Permissions.ARENA_EDIT, "/evento select (nombre)", "seleccionar", "editar");
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (args.length < 1) {
            messages.send(player, "arena.select-usage");
            sounds.play(player, "command-error");
            return;
        }
        Arena arena = arenaManager.get(args[0]).orElse(null);
        if (arena == null) {
            messages.send(player, "arena.not-found", Placeholder.unparsed("arena", args[0]));
            sounds.play(player, "command-error");
            return;
        }
        sessions.select(player.getUniqueId(), arena.getName());
        messages.send(player, "arena.selected", Placeholder.unparsed("arena", arena.getName()));
        sounds.play(player, "command-success");
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
