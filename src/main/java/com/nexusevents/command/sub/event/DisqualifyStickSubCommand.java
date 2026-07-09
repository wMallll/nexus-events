package com.nexusevents.command.sub.event;

import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.moderation.DisqualifyStick;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import org.bukkit.entity.Player;

/**
 * Subcomando {@code /evento dqstick}: entrega el Palo Descalificador.
 */
public final class DisqualifyStickSubCommand extends PlayerSubCommand {

    private final MessageService messages;
    private final SoundService sounds;

    public DisqualifyStickSubCommand(MessageService messages, SoundService sounds) {
        super(messages, "dqstick", Permissions.DQ_STICK, "/evento dqstick", "palo", "descalificador");
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        player.getInventory().addItem(DisqualifyStick.create());
        messages.send(player, "moderation.stick-given");
        sounds.play(player, "command-success");
    }
}
