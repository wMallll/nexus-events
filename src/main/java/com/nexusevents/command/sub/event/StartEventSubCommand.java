package com.nexusevents.command.sub.event;

import com.nexusevents.command.SubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.GameEvent;
import com.nexusevents.event.StartResult;
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
 * Subcomando {@code /evento start (evento) (arena)}.
 *
 * <p>Inicia un evento sobre una arena, informando con precision el
 * motivo de cualquier fallo (evento inexistente, arena ocupada o
 * incompleta, con el detalle exacto de lo que falta configurar).</p>
 */
public final class StartEventSubCommand extends SubCommand {

    private final EventManager eventManager;
    private final MessageService messages;
    private final SoundService sounds;

    public StartEventSubCommand(EventManager eventManager, MessageService messages, SoundService sounds) {
        super("start", Permissions.START, "/evento start (evento) (arena)", "iniciar");
        this.eventManager = eventManager;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "event.start-usage");
            sounds.play(sender, "command-error");
            return;
        }
        StartResult result = eventManager.start(args[0], args[1]);
        switch (result.getStatus()) {
            case SUCCESS:
                messages.send(sender, "event.started-by",
                        Placeholder.parsed("event", displayName(args[0])),
                        Placeholder.unparsed("arena", args[1]));
                sounds.play(sender, "command-success");
                break;
            case EVENT_NOT_FOUND:
                messages.send(sender, "event.event-not-found", Placeholder.unparsed("event", args[0]));
                sounds.play(sender, "command-error");
                break;
            case ARENA_NOT_FOUND:
                messages.send(sender, "arena.not-found", Placeholder.unparsed("arena", args[1]));
                sounds.play(sender, "command-error");
                break;
            case ARENA_IN_USE:
                messages.send(sender, "event.arena-in-use", Placeholder.unparsed("arena", args[1]));
                sounds.play(sender, "command-error");
                break;
            case ARENA_INCOMPLETE:
                messages.send(sender, "event.arena-incomplete",
                        Placeholder.unparsed("arena", args[1]),
                        Placeholder.unparsed("missing", result.getDetail()));
                sounds.play(sender, "command-error");
                break;
            default:
                break;
        }
    }

    private String displayName(String eventId) {
        return messages.rawOr("event.names." + eventId.toLowerCase(Locale.ROOT), eventId);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String lowered = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (GameEvent type : eventManager.getTypes()) {
                if (type.getId().startsWith(lowered)) {
                    suggestions.add(type.getId());
                }
            }
            return suggestions;
        }
        if (args.length == 2) {
            String lowered = args[1].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (String name : eventManager.getContext().getArenas().getNames()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                    suggestions.add(name);
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }
}
