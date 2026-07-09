package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Subcomando {@code /evento parkour} para el armado del recorrido por
 * fragmentos (islas) ordenados.
 *
 * <p>Flujo: {@code pos1} y {@code pos2} marcan las esquinas del
 * fragmento en tu posicion; {@code add} lo guarda como el siguiente de
 * la secuencia y limpia la seleccion; {@code remove (n)} deshace el
 * fragmento n (renumerando el resto); {@code list} y {@code clear}
 * completan la gestion. Los fragmentos se desintegran en la partida en
 * el orden en que fueron agregados.</p>
 */
public final class ParkourSetupSubCommand extends PlayerSubCommand {

    private final ArenaManager arenaManager;
    private final SetupSessionService sessions;
    private final MessageService messages;
    private final SoundService sounds;

    private final Map<UUID, ArenaLocation> firstCorner = new HashMap<>();
    private final Map<UUID, ArenaLocation> secondCorner = new HashMap<>();

    public ParkourSetupSubCommand(ArenaManager arenaManager, SetupSessionService sessions,
                                  MessageService messages, SoundService sounds) {
        super(messages, "parkour", Permissions.setup("parkour"),
                "/evento parkour (pos1|pos2|add|remove (n)|list|clear)");
        this.arenaManager = arenaManager;
        this.sessions = sessions;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (args.length < 1) {
            messages.send(player, "parkour-setup.usage");
            sounds.play(player, "command-error");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pos1":
                firstCorner.put(player.getUniqueId(), ArenaLocation.from(player.getLocation()));
                messages.send(player, "parkour-setup.pos-set", Placeholder.unparsed("corner", "1"));
                sounds.play(player, "command-success");
                break;
            case "pos2":
                secondCorner.put(player.getUniqueId(), ArenaLocation.from(player.getLocation()));
                messages.send(player, "parkour-setup.pos-set", Placeholder.unparsed("corner", "2"));
                sounds.play(player, "command-success");
                break;
            case "add":
                addFragment(player);
                break;
            case "remove":
                removeFragment(player, args);
                break;
            case "list":
                listFragments(player);
                break;
            case "clear":
                clearFragments(player);
                break;
            default:
                messages.send(player, "parkour-setup.usage");
                sounds.play(player, "command-error");
                break;
        }
    }

    private void addFragment(Player player) {
        Arena arena = requireArena(player);
        if (arena == null) {
            return;
        }
        ArenaLocation first = firstCorner.get(player.getUniqueId());
        ArenaLocation second = secondCorner.get(player.getUniqueId());
        if (first == null || second == null) {
            messages.send(player, "parkour-setup.need-positions");
            sounds.play(player, "command-error");
            return;
        }
        if (!first.getWorldName().equals(second.getWorldName())) {
            messages.send(player, "parkour-setup.different-worlds");
            sounds.play(player, "command-error");
            return;
        }
        Region fragment = Region.of(first, second);
        int index = nextIndex(arena);
        arena.setRegion(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + index, fragment);
        arenaManager.save(arena);
        firstCorner.remove(player.getUniqueId());
        secondCorner.remove(player.getUniqueId());
        messages.send(player, "parkour-setup.fragment-added",
                Placeholder.unparsed("index", String.valueOf(index)),
                Placeholder.unparsed("dimensions", fragment.describe()),
                Placeholder.unparsed("arena", arena.getName()));
        sounds.play(player, "command-success");
    }

    private void removeFragment(Player player, String[] args) {
        Arena arena = requireArena(player);
        if (arena == null) {
            return;
        }
        List<Region> fragments = loadFragments(arena);
        Integer index = args.length >= 2 ? parseIndex(args[1], fragments.size()) : null;
        if (index == null) {
            messages.send(player, "parkour-setup.fragment-not-found",
                    Placeholder.unparsed("count", String.valueOf(fragments.size())));
            sounds.play(player, "command-error");
            return;
        }
        fragments.remove(index - 1);
        rewriteFragments(arena, fragments);
        arenaManager.save(arena);
        messages.send(player, "parkour-setup.fragment-removed",
                Placeholder.unparsed("index", String.valueOf(index)),
                Placeholder.unparsed("count", String.valueOf(fragments.size())));
        sounds.play(player, "command-success");
    }

    private void listFragments(Player player) {
        Arena arena = requireArena(player);
        if (arena == null) {
            return;
        }
        List<Region> fragments = loadFragments(arena);
        if (fragments.isEmpty()) {
            messages.send(player, "parkour-setup.list-empty");
            return;
        }
        messages.send(player, "parkour-setup.list-header",
                Placeholder.unparsed("count", String.valueOf(fragments.size())),
                Placeholder.unparsed("arena", arena.getName()));
        for (int i = 0; i < fragments.size(); i++) {
            messages.send(player, "parkour-setup.list-entry",
                    Placeholder.unparsed("index", String.valueOf(i + 1)),
                    Placeholder.unparsed("dimensions", fragments.get(i).describe()));
        }
    }

    private void clearFragments(Player player) {
        Arena arena = requireArena(player);
        if (arena == null) {
            return;
        }
        List<Region> fragments = loadFragments(arena);
        rewriteFragments(arena, Collections.emptyList());
        arenaManager.save(arena);
        messages.send(player, "parkour-setup.cleared",
                Placeholder.unparsed("count", String.valueOf(fragments.size())));
        sounds.play(player, "command-success");
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Arena requireArena(Player player) {
        Arena arena = sessions.resolveSelected(player, arenaManager).orElse(null);
        if (arena == null) {
            messages.send(player, "arena.none-selected");
            sounds.play(player, "command-error");
        }
        return arena;
    }

    private int nextIndex(Arena arena) {
        int index = 1;
        while (arena.hasRegion(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + index)) {
            index++;
        }
        return index;
    }

    private List<Region> loadFragments(Arena arena) {
        List<Region> fragments = new ArrayList<>();
        int index = 1;
        Region fragment;
        while ((fragment = arena.getRegion(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + index).orElse(null)) != null) {
            fragments.add(fragment);
            index++;
        }
        return fragments;
    }

    private void rewriteFragments(Arena arena, List<Region> fragments) {
        int index = 1;
        while (arena.hasRegion(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + index)) {
            arena.removeRegion(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + index);
            index++;
        }
        for (int i = 0; i < fragments.size(); i++) {
            arena.setRegion(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + (i + 1), fragments.get(i));
        }
    }

    private Integer parseIndex(String raw, int max) {
        try {
            int index = Integer.parseInt(raw.trim());
            if (index < 1 || index > max) {
                return null;
            }
            return index;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (String option : new String[]{"pos1", "pos2", "add", "remove", "list", "clear"}) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    options.add(option);
                }
            }
            return options;
        }
        return Collections.emptyList();
    }
}
