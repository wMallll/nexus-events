package com.nexusevents.command.sub.event;

import com.nexusevents.command.SubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.EventSession;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Subcomando {@code /evento disqualify (jugador)}: descalifica a un
 * participante vivo, que pasa a espectador con el pipeline estandar
 * (invisibilidad, vanish, HUD y modo torneo si esta activo).
 */
public final class DisqualifySubCommand extends SubCommand {

    private final EventManager eventManager;
    private final MessageService messages;
    private final SoundService sounds;

    public DisqualifySubCommand(EventManager eventManager, MessageService messages, SoundService sounds) {
        super("disqualify", Permissions.DISQUALIFY, "/evento disqualify (jugador)", "descalificar", "dq");
        this.eventManager = eventManager;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            messages.send(sender, "moderation.disqualify-usage");
            sounds.play(sender, "command-error");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(sender, "moderation.player-not-found",
                    Placeholder.unparsed("player", args[0]));
            sounds.play(sender, "command-error");
            return;
        }
        EventSession session = eventManager.getSessionByPlayer(target.getUniqueId()).orElse(null);
        if (session == null) {
            messages.send(sender, "moderation.not-in-event",
                    Placeholder.unparsed("player", target.getName()));
            sounds.play(sender, "command-error");
            return;
        }
        if (!session.isAlive(target.getUniqueId())) {
            messages.send(sender, "moderation.already-eliminated",
                    Placeholder.unparsed("player", target.getName()));
            sounds.play(sender, "command-error");
            return;
        }
        session.eliminate(target);
        messages.send(sender, "moderation.disqualified",
                Placeholder.unparsed("player", target.getName()));
        messages.send(target, "moderation.disqualified-target");
        sounds.play(sender, "command-success");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String lowered = args[0].toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (EventSession session : eventManager.getSessions()) {
            for (UUID id : session.getAlive()) {
                Player alive = Bukkit.getPlayer(id);
                if (alive != null && alive.getName().toLowerCase(Locale.ROOT).startsWith(lowered)) {
                    names.add(alive.getName());
                }
            }
        }
        return names;
    }
}
