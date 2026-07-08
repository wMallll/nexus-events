package com.nexusevents.command.sub.event;

import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import org.bukkit.entity.Player;

/**
 * Subcomando {@code /evento leave}.
 *
 * <p>Saca al jugador del evento en el que participa, restaurando por
 * completo su estado previo (inventario, posicion, modo de juego, etc.).</p>
 */
public final class LeaveEventSubCommand extends PlayerSubCommand {

    private final EventManager eventManager;
    private final MessageService messages;
    private final SoundService sounds;

    public LeaveEventSubCommand(EventManager eventManager, MessageService messages, SoundService sounds) {
        super(messages, "leave", Permissions.LEAVE, "/evento leave", "salir");
        this.eventManager = eventManager;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (!eventManager.leave(player)) {
            messages.send(player, "event.not-in");
            sounds.play(player, "command-error");
            return;
        }
        messages.send(player, "event.left");
    }
}
