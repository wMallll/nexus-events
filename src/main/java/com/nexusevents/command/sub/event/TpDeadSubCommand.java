package com.nexusevents.command.sub.event;

import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.EventSession;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Subcomando {@code /evento tpdead}: teletransporta a todos los
 * eliminados online de todas las sesiones activas hasta el ejecutor,
 * sin importar el mundo en que este.
 */
public final class TpDeadSubCommand extends PlayerSubCommand {

    private final EventManager eventManager;
    private final MessageService messages;
    private final SoundService sounds;

    public TpDeadSubCommand(EventManager eventManager, MessageService messages, SoundService sounds) {
        super(messages, "tpdead", Permissions.TP_DEAD, "/evento tpdead", "tpmuertos", "tpeliminados");
        this.eventManager = eventManager;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        int count = 0;
        for (EventSession session : eventManager.getSessions()) {
            for (UUID id : session.getEliminated()) {
                Player eliminated = Bukkit.getPlayer(id);
                if (eliminated != null && eliminated.isOnline()) {
                    eliminated.teleport(player.getLocation());
                    count++;
                }
            }
        }
        if (count == 0) {
            messages.send(player, "moderation.tpdead-none");
            sounds.play(player, "command-error");
            return;
        }
        messages.send(player, "moderation.tpdead-done",
                Placeholder.unparsed("count", String.valueOf(count)));
        sounds.play(player, "command-success");
    }
}
