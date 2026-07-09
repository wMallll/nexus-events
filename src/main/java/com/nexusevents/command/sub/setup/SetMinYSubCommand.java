package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.GameEvent;
import com.nexusevents.lobby.MainLobbyService;
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
 * Subcomando {@code /evento setminy [evento|main]}.
 *
 * <p>Fija la altura minima de seguridad usando la Y ACTUAL del
 * jugador: quien caiga debajo vuelve al lobby correspondiente. Sin
 * argumento define la general de la arena; con un id de evento, la
 * variante especifica (con fallback a la general); con {@code main},
 * la del lobby global del servidor.</p>
 */
public final class SetMinYSubCommand extends PlayerSubCommand {

    private static final String MAIN_KEYWORD = "main";

    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final EventManager eventManager;
    private final MainLobbyService mainLobby;
    private final MessageService messages;
    private final SoundService sounds;

    public SetMinYSubCommand(ArenaManager arenaManager, SetupSessionService sessions,
                             EventManager eventManager, MainLobbyService mainLobby,
                             MessageService messages, SoundService sounds) {
        super(messages, "setminy", Permissions.setup("setminy"),
                "/evento setminy [evento|main]", "setalturaminima", "setminheight");
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.eventManager = eventManager;
        this.mainLobby = mainLobby;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        int minY = player.getLocation().getBlockY();
        if (args.length >= 1 && args[0].equalsIgnoreCase(MAIN_KEYWORD)) {
            mainLobby.setMinY(minY);
            confirm(player, minY, messages.rawOr("arena.miny-scope-main", "Lobby global"));
            return;
        }

        Arena arena = sessions.resolveSelected(player, arenaManager).orElse(null);
        if (arena == null) {
            messages.send(player, "arena.none-selected");
            sounds.play(player, "command-error");
            return;
        }

        String propertyKey = ArenaKeys.MIN_Y;
        String scope = messages.rawOr("arena.miny-scope-general", "general de la arena");
        if (args.length >= 1) {
            GameEvent event = eventManager.getType(args[0]).orElse(null);
            if (event == null) {
                messages.send(player, "event.event-not-found", Placeholder.unparsed("event", args[0]));
                sounds.play(player, "command-error");
                return;
            }
            propertyKey = ArenaKeys.MIN_Y + "_" + event.getId();
            scope = messages.rawOr("event.names." + event.getId(), event.getId());
        }
        arena.setProperty(propertyKey, String.valueOf(minY));
        arenaManager.save(arena);
        confirm(player, minY, scope);
    }

    private void confirm(Player player, int minY, String scope) {
        messages.send(player, "arena.miny-set",
                Placeholder.unparsed("y", String.valueOf(minY)),
                Placeholder.parsed("scope", scope));
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
        if (MAIN_KEYWORD.startsWith(lowered)) {
            suggestions.add(MAIN_KEYWORD);
        }
        return suggestions;
    }
}
