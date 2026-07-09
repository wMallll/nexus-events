package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Subcomando {@code /evento setcircle (radio)}.
 *
 * <p>Al estilo de {@code //cyl} de WorldEdit: el administrador se para
 * en el centro del piso circular y ejecuta el comando con el radio en
 * bloques. Se guardan el centro (su posicion) y el radio como propiedad
 * de la arena; el evento escanea los bloques existentes dentro de ese
 * cilindro.</p>
 */
public final class SetCircleSubCommand extends PlayerSubCommand {

    private static final int MIN_RADIUS = 3;
    private static final int MAX_RADIUS = 500;

    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final MessageService messages;
    private final SoundService sounds;

    public SetCircleSubCommand(ArenaManager arenaManager, SetupSessionService sessions,
                               MessageService messages, SoundService sounds) {
        super(messages, "setcircle", Permissions.setup("setcircle"), "/evento setcircle (radio)");
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        Arena arena = sessions.resolveSelected(player, arenaManager).orElse(null);
        if (arena == null) {
            messages.send(player, "arena.none-selected");
            sounds.play(player, "command-error");
            return;
        }
        Integer radius = parseRadius(args);
        if (radius == null) {
            messages.send(player, "arena.circle-usage",
                    Placeholder.unparsed("min", String.valueOf(MIN_RADIUS)),
                    Placeholder.unparsed("max", String.valueOf(MAX_RADIUS)));
            sounds.play(player, "command-error");
            return;
        }
        arena.setPoint(ArenaKeys.CIRCLE_CENTER, ArenaLocation.from(player.getLocation()));
        arena.setProperty(ArenaKeys.CIRCLE_RADIUS, String.valueOf(radius));
        arenaManager.save(arena);
        messages.send(player, "arena.circle-set",
                Placeholder.unparsed("radius", String.valueOf(radius)),
                Placeholder.unparsed("arena", arena.getName()));
        sounds.play(player, "command-success");
    }

    private Integer parseRadius(String[] args) {
        if (args.length < 1) {
            return null;
        }
        try {
            int radius = Integer.parseInt(args[0].trim());
            if (radius < MIN_RADIUS || radius > MAX_RADIUS) {
                return null;
            }
            return radius;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("25", "50", "100");
        }
        return Collections.emptyList();
    }
}
