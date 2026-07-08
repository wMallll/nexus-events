package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.ArenaManager;
import com.nexusevents.command.SubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/**
 * Subcomando {@code /evento save}.
 *
 * <p>Fuerza el guardado en disco de todas las arenas. El plugin ya
 * persiste automaticamente cada cambio de setup; este comando existe
 * como red de seguridad explicita para los administradores.</p>
 */
public final class SaveSubCommand extends SubCommand {

    private final ArenaManager arenaManager;
    private final MessageService messages;
    private final SoundService sounds;

    public SaveSubCommand(ArenaManager arenaManager, MessageService messages, SoundService sounds) {
        super("save", Permissions.SETUP_SAVE, "/evento save", "guardar");
        this.arenaManager = arenaManager;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int count = arenaManager.saveAll();
        messages.send(sender, "arena.saved-all", Placeholder.unparsed("count", String.valueOf(count)));
        sounds.play(sender, "command-success");
    }
}
