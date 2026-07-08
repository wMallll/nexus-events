package com.nexusevents.command.sub.event;

import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.EventSession;
import com.nexusevents.event.JoinResult;
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
 * Subcomando {@code /evento join [arena]}.
 *
 * <p>Une al jugador al evento activo. Si hay varios eventos en
 * simultaneo, debe indicar la arena.</p>
 */
public final class JoinEventSubCommand extends PlayerSubCommand {

    private final EventManager eventManager;
    private final MessageService messages;
    private final SoundService sounds;

    public JoinEventSubCommand(EventManager eventManager, MessageService messages, SoundService sounds) {
        super(messages, "join", Permissions.JOIN, "/evento join [arena]", "entrar", "unirme");
        this.eventManager = eventManager;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        String arenaName = args.length >= 1 ? args[0] : null;
        JoinResult result = eventManager.join(player, arenaName);
        switch (result) {
            case SUCCESS:
                // La sesion ya envia los mensajes y sonidos de bienvenida.
                break;
            case NONE_ACTIVE:
                messages.send(player, "event.none-active");
                sounds.play(player, "command-error");
                break;
            case NOT_FOUND:
                messages.send(player, "event.none-active");
                sounds.play(player, "command-error");
                break;
            case AMBIGUOUS:
                messages.send(player, "event.specify-arena",
                        Placeholder.unparsed("usage", getUsage()));
                sounds.play(player, "command-error");
                break;
            case ALREADY_IN:
                messages.send(player, "event.already-in");
                sounds.play(player, "command-error");
                break;
            case FULL:
                messages.send(player, "event.full");
                sounds.play(player, "command-error");
                break;
            case ALREADY_STARTED:
                messages.send(player, "event.already-started");
                sounds.play(player, "command-error");
                break;
            default:
                break;
        }
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
