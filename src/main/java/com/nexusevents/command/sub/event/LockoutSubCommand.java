package com.nexusevents.command.sub.event;

import com.nexusevents.command.SubCommand;
import com.nexusevents.lockout.LockoutService;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Subcomando {@code /evento lockout (on|off|clear)}.
 *
 * <p>Controla el modo torneo: con {@code on}, los eliminados quedan
 * bloqueados (sin comandos, sin reingreso al servidor, sin unirse a
 * eventos) hasta que un admin ejecute {@code clear}. Con {@code off}
 * (ideal para pruebas), los eliminados pueden seguir participando.
 * Sin argumentos muestra el estado actual.</p>
 */
public final class LockoutSubCommand extends SubCommand {

    private final LockoutService lockouts;
    private final MessageService messages;
    private final SoundService sounds;

    public LockoutSubCommand(LockoutService lockouts, MessageService messages, SoundService sounds) {
        super("lockout", Permissions.LOCKOUT, "/evento lockout (on|off|clear)", "torneo");
        this.lockouts = lockouts;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendStatus(sender);
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on":
                lockouts.setEnabled(true);
                messages.send(sender, "lockout.enabled");
                sounds.play(sender, "command-success");
                break;
            case "off":
                lockouts.setEnabled(false);
                messages.send(sender, "lockout.disabled");
                sounds.play(sender, "command-success");
                break;
            case "clear":
                int count = lockouts.clear();
                messages.send(sender, "lockout.cleared",
                        Placeholder.unparsed("count", String.valueOf(count)));
                sounds.play(sender, "command-success");
                break;
            default:
                messages.send(sender, "lockout.usage");
                sounds.play(sender, "command-error");
                break;
        }
    }

    private void sendStatus(CommandSender sender) {
        messages.send(sender, "lockout.status",
                Placeholder.parsed("state", messages.rawOr(
                        lockouts.isEnabled() ? "lockout.state-on" : "lockout.state-off", "?")),
                Placeholder.unparsed("count", String.valueOf(lockouts.getLockedCount())));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "clear");
        }
        return Collections.emptyList();
    }
}
