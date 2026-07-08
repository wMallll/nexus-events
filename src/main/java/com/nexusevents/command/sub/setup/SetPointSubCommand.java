package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/**
 * Comando generico de setup de puntos.
 *
 * <p>Una unica clase implementa {@code setspawn}, {@code setlobby},
 * {@code sethunterspawn} y {@code setcircle}: cada instancia se asocia a
 * una clave de punto distinta (DRY). Guarda la posicion exacta del
 * administrador (incluyendo orientacion) en la arena seleccionada y la
 * persiste automaticamente.</p>
 */
public final class SetPointSubCommand extends PlayerSubCommand {

    private final String pointKey;
    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final MessageService messages;
    private final SoundService sounds;

    public SetPointSubCommand(String name, String pointKey, ArenaManager arenaManager,
                              SetupSessionService sessions, MessageService messages, SoundService sounds) {
        super(messages, name, Permissions.setup(name), "/evento " + name);
        this.pointKey = pointKey;
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        Arena arena = sessions.resolveSelected(player, arenaManager).orElse(null);
        if (arena == null) {
            messages.send(player, "arena.none-selected");
            sounds.play(player, "command-error");
            return;
        }
        arena.setPoint(pointKey, ArenaLocation.from(player.getLocation()));
        arenaManager.save(arena);
        messages.send(player, "arena.point-set",
                Placeholder.parsed("point", messages.rawOr("arena.point-names." + pointKey, pointKey)),
                Placeholder.unparsed("arena", arena.getName()));
        sounds.play(player, "command-success");
    }
}
