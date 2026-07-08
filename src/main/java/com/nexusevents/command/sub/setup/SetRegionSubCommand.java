package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.Region;
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
import java.util.Optional;

/**
 * Comando generico de setup de regiones.
 *
 * <p>Una unica clase implementa {@code setpixelparty} y
 * {@code setparkour}: cada instancia se asocia a una clave de region
 * distinta. El administrador marca las dos esquinas parado en el lugar
 * ({@code 1} y {@code 2}, en cualquier orden); al completar la segunda
 * se valida el mundo, se normaliza la region y se persiste.</p>
 */
public final class SetRegionSubCommand extends PlayerSubCommand {

    private final String regionKey;
    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final MessageService messages;
    private final SoundService sounds;

    public SetRegionSubCommand(String name, String regionKey, ArenaManager arenaManager,
                               SetupSessionService sessions, MessageService messages, SoundService sounds) {
        super(messages, name, Permissions.setup(name), "/evento " + name + " (1|2)");
        this.regionKey = regionKey;
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        int corner = parseCorner(args);
        if (corner == -1) {
            messages.send(player, "arena.region-usage", Placeholder.unparsed("usage", getUsage()));
            sounds.play(player, "command-error");
            return;
        }
        Arena arena = sessions.resolveSelected(player, arenaManager).orElse(null);
        if (arena == null) {
            messages.send(player, "arena.none-selected");
            sounds.play(player, "command-error");
            return;
        }

        ArenaLocation marked = ArenaLocation.from(player.getLocation());
        sessions.setPendingCorner(player.getUniqueId(), regionKey, corner, marked);

        int otherCorner = corner == 1 ? 2 : 1;
        Optional<ArenaLocation> other = sessions.getPendingCorner(player.getUniqueId(), regionKey, otherCorner);
        if (!other.isPresent()) {
            messages.send(player, "arena.region-corner-set",
                    Placeholder.unparsed("corner", String.valueOf(corner)),
                    Placeholder.parsed("region", regionDisplayName()));
            sounds.play(player, "command-success");
            return;
        }

        completeRegion(player, arena, marked, other.get());
    }

    private void completeRegion(Player player, Arena arena, ArenaLocation marked, ArenaLocation other) {
        try {
            Region region = Region.of(marked, other);
            arena.setRegion(regionKey, region);
            arenaManager.save(arena);
            sessions.clearPendingCorners(player.getUniqueId(), regionKey);
            messages.send(player, "arena.region-set",
                    Placeholder.parsed("region", regionDisplayName()),
                    Placeholder.unparsed("arena", arena.getName()),
                    Placeholder.unparsed("dimensions", region.describe()));
            sounds.play(player, "command-success");
        } catch (IllegalArgumentException exception) {
            messages.send(player, "arena.region-world-mismatch");
            sounds.play(player, "command-error");
        }
    }

    private int parseCorner(String[] args) {
        if (args.length < 1) {
            return -1;
        }
        if (args[0].equals("1")) {
            return 1;
        }
        if (args[0].equals("2")) {
            return 2;
        }
        return -1;
    }

    private String regionDisplayName() {
        return messages.rawOr("arena.region-names." + regionKey, regionKey);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("1", "2");
        }
        return Collections.emptyList();
    }
}
