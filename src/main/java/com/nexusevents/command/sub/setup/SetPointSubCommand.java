package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.GameEvent;
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
 * Comando generico de setup de puntos.
 *
 * <p>Una unica clase implementa {@code setspawn}, {@code setlobby},
 * {@code sethunterspawn} y {@code setcircle}. Acepta un argumento
 * opcional con el id de un evento para guardar una variante especifica
 * de ese punto (por ejemplo {@code /evento setspawn pixel-party}); los
 * eventos usan su variante si existe y el punto general si no.</p>
 */
public final class SetPointSubCommand extends PlayerSubCommand {

    private final String pointKey;
    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final EventManager eventManager;
    private final MessageService messages;
    private final SoundService sounds;

    public SetPointSubCommand(String name, String pointKey, ArenaManager arenaManager,
                              SetupSessionService sessions, EventManager eventManager,
                              MessageService messages, SoundService sounds) {
        super(messages, name, Permissions.setup(name), "/evento " + name + " [evento]");
        this.pointKey = pointKey;
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.eventManager = eventManager;
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

        String eventId = null;
        if (args.length >= 1) {
            GameEvent event = eventManager.getType(args[0]).orElse(null);
            if (event == null) {
                messages.send(player, "event.event-not-found", Placeholder.unparsed("event", args[0]));
                sounds.play(player, "command-error");
                return;
            }
            eventId = event.getId();
        }

        String targetKey = eventId != null ? pointKey + "_" + eventId : pointKey;
        arena.setPoint(targetKey, ArenaLocation.from(player.getLocation()));
        arenaManager.save(arena);

        String display = messages.rawOr("arena.point-names." + pointKey, pointKey);
        if (eventId != null) {
            display = display + " <dark_gray>(<gray>"
                    + messages.rawOr("event.names." + eventId, eventId) + "<dark_gray>)";
        }
        messages.send(player, "arena.point-set",
                Placeholder.parsed("point", display),
                Placeholder.unparsed("arena", arena.getName()));
        sounds.play(player, "command-success");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String lowered = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (GameEvent event : eventManager.getTypes()) {
            if (event.getId().startsWith(lowered)) {
                suggestions.add(event.getId());
            }
        }
        return suggestions;
    }
}
