package com.nexusevents.command.sub.event;

import com.nexusevents.command.SubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.moderation.CollisionService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Subcomando {@code /evento collision (on|off)}: interruptor GLOBAL de
 * la colision entre jugadores en todo el servidor. Sin argumentos,
 * muestra el estado actual.
 */
public final class CollisionSubCommand extends SubCommand {

    private final CollisionService collision;
    private final MessageService messages;
    private final SoundService sounds;

    public CollisionSubCommand(CollisionService collision, MessageService messages, SoundService sounds) {
        super("collision", Permissions.COLLISION, "/evento collision (on|off)",
                "colision", "colisiones");
        this.collision = collision;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            messages.send(sender, "collision.status", Placeholder.parsed("state",
                    messages.rawOr(collision.isDisabled()
                            ? "collision.state-off" : "collision.state-on", "?")));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "off":
                collision.setDisabled(true);
                messages.broadcast("collision.disabled");
                sounds.play(sender, "command-success");
                break;
            case "on":
                collision.setDisabled(false);
                messages.broadcast("collision.enabled");
                sounds.play(sender, "command-success");
                break;
            default:
                messages.send(sender, "collision.usage");
                sounds.play(sender, "command-error");
                break;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String lowered = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        for (String option : Arrays.asList("on", "off")) {
            if (option.startsWith(lowered)) {
                options.add(option);
            }
        }
        return options;
    }
}
