package com.nexusevents.command.sub.event;

import com.nexusevents.command.SubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.EventSession;
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
 * Subcomando {@code /evento stop [arena]}.
 *
 * <p>Detiene el evento activo en la arena indicada. Si hay un unico
 * evento activo, la arena es opcional.</p>
 */
public final class StopEventSubCommand extends SubCommand {

    private final EventManager eventManager;
    private final MessageService messages;
    private final SoundService sounds;

    public StopEventSubCommand(EventManager eventManager, MessageService messages, SoundService sounds) {
        super("stop", Permissions.STOP, "/evento stop [arena]", "detener");
        this.eventManager = eventManager;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String arenaName = resolveArenaName(args);
        if (arenaName == null) {
            if (eventManager.getSessions().isEmpty()) {
                messages.send(sender, "event.none-active");
            } else {
                messages.send(sender, "event.specify-arena",
                        Placeholder.unparsed("usage", getUsage()));
            }
            sounds.play(sender, "command-error");
            return;
        }
        if (!eventManager.stop(arenaName)) {
            messages.send(sender, "event.none-active");
            sounds.play(sender, "command-error");
            return;
        }
        messages.send(sender, "event.stopped", Placeholder.unparsed("arena", arenaName));
        sounds.play(sender, "command-success");
    }

    private String resolveArenaName(String[] args) {
        if (args.length >= 1) {
            return args[0];
        }
        if (eventManager.getSessions().size() == 1) {
            return eventManager.getSessions().iterator().next().getArena().getName();
        }
        return null;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String lowered = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (EventSession session : eventManager.getSessions()) {
            String name = session.getArena().getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}
