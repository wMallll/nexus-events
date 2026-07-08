package com.nexusevents.command.sub.event;

import com.nexusevents.command.SubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.EventSession;
import com.nexusevents.event.GameEvent;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/**
 * Subcomando {@code /evento events}.
 *
 * <p>Lista los tipos de evento disponibles y las sesiones activas con
 * su arena, estado y cantidad de jugadores.</p>
 */
public final class EventsSubCommand extends SubCommand {

    private final EventManager eventManager;
    private final MessageService messages;

    public EventsSubCommand(EventManager eventManager, MessageService messages) {
        super("events", Permissions.EVENTS_LIST, "/evento events", "lista");
        this.eventManager = eventManager;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (eventManager.getTypes().isEmpty()) {
            messages.send(sender, "event.types-empty");
        } else {
            messages.send(sender, "event.types-header",
                    Placeholder.unparsed("count", String.valueOf(eventManager.getTypes().size())));
            for (GameEvent type : eventManager.getTypes()) {
                messages.send(sender, "event.types-entry",
                        Placeholder.unparsed("id", type.getId()),
                        Placeholder.parsed("name", messages.rawOr("event.names." + type.getId(), type.getId())));
            }
        }

        messages.send(sender, "event.sessions-header");
        if (eventManager.getSessions().isEmpty()) {
            messages.send(sender, "event.sessions-none");
            return;
        }
        for (EventSession session : eventManager.getSessions()) {
            messages.send(sender, "event.sessions-entry",
                    Placeholder.unparsed("arena", session.getArena().getName()),
                    Placeholder.parsed("event", session.displayName()),
                    Placeholder.parsed("state", messages.rawOr("event.states." + session.getState().getKey(),
                            session.getState().getKey())),
                    Placeholder.unparsed("players", String.valueOf(session.getAliveCount())));
        }
    }
}
