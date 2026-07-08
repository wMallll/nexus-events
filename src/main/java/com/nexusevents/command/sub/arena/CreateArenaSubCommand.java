package com.nexusevents.command.sub.arena;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/**
 * Subcomando {@code /evento createarena (nombre)}.
 *
 * <p>Crea una arena nueva con nombre validado, la persiste
 * inmediatamente y la deja seleccionada para continuar el setup.</p>
 */
public final class CreateArenaSubCommand extends PlayerSubCommand {

    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final MessageService messages;
    private final SoundService sounds;

    public CreateArenaSubCommand(ArenaManager arenaManager, SetupSessionService sessions,
                                 MessageService messages, SoundService sounds) {
        super(messages, "createarena", Permissions.ARENA_CREATE, "/evento createarena (nombre)", "crear");
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (args.length < 1) {
            messages.send(player, "arena.create-usage");
            sounds.play(player, "command-error");
            return;
        }
        String name = args[0];
        if (!arenaManager.isValidName(name)) {
            messages.send(player, "arena.invalid-name");
            sounds.play(player, "command-error");
            return;
        }
        if (arenaManager.exists(name)) {
            messages.send(player, "arena.already-exists", Placeholder.unparsed("arena", name));
            sounds.play(player, "command-error");
            return;
        }
        Arena arena = arenaManager.create(name);
        sessions.select(player.getUniqueId(), arena.getName());
        messages.send(player, "arena.created", Placeholder.unparsed("arena", arena.getName()));
        sounds.play(player, "command-success");
    }
}
