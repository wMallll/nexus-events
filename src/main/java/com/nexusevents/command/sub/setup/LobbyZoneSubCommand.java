package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.Region;
import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.GameEvent;
import com.nexusevents.lobby.LobbyProtectionSettings;
import com.nexusevents.lobby.LobbyZoneService;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Subcomando {@code /evento lobbyzone (evento|general) (arena)
 * (accion)}: zonas de lobby explicitas por evento y arena, con el
 * alcance {@code general} como fallback.
 *
 * <p>Ejemplos: {@code /evento lobbyzone parkour test pos1},
 * {@code /evento lobbyzone general test set no-damage true}.</p>
 */
public final class LobbyZoneSubCommand extends PlayerSubCommand {

    private final LobbyZoneService zones;
    private final ArenaManager arenaManager;
    private final EventManager eventManager;
    private final MessageService messages;
    private final SoundService sounds;

    /** Esquinas marcadas por jugador y por alcance (arena|scope). */
    private final Map<UUID, Map<String, ArenaLocation>> firstCorner = new HashMap<>();
    private final Map<UUID, Map<String, ArenaLocation>> secondCorner = new HashMap<>();

    public LobbyZoneSubCommand(LobbyZoneService zones, ArenaManager arenaManager,
                               EventManager eventManager, MessageService messages,
                               SoundService sounds) {
        super(messages, "lobbyzone", Permissions.setup("lobbyzone"),
                "/evento lobbyzone (evento|general) (arena) (pos1|pos2|removeregion|set|info)",
                "zonalobby");
        this.zones = zones;
        this.arenaManager = arenaManager;
        this.eventManager = eventManager;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (args.length < 3) {
            messages.send(player, "lobbyzone.usage");
            sounds.play(player, "command-error");
            return;
        }
        String scope = resolveScope(player, args[0]);
        if (scope == null) {
            return;
        }
        Arena arena = arenaManager.get(args[1]).orElse(null);
        if (arena == null) {
            messages.send(player, "arena.not-found", Placeholder.unparsed("arena", args[1]));
            sounds.play(player, "command-error");
            return;
        }
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "pos1":
                markCorner(player, arena, scope, firstCorner, "1");
                break;
            case "pos2":
                markCorner(player, arena, scope, secondCorner, "2");
                break;
            case "removeregion":
                if (zones.removeZone(arena.getName(), scope)) {
                    messages.send(player, "lobbyzone.region-removed",
                            Placeholder.unparsed("arena", arena.getName()),
                            Placeholder.parsed("scope", displayScope(scope)));
                    sounds.play(player, "command-success");
                } else {
                    messages.send(player, "lobbyzone.no-region",
                            Placeholder.unparsed("arena", arena.getName()),
                            Placeholder.parsed("scope", displayScope(scope)));
                    sounds.play(player, "command-error");
                }
                break;
            case "set":
                applySetting(player, arena, scope, args);
                break;
            case "info":
                sendInfo(player, arena, scope);
                break;
            default:
                messages.send(player, "lobbyzone.usage");
                sounds.play(player, "command-error");
                break;
        }
    }

    /**
     * Valida el alcance: {@code general} o un id de evento existente.
     */
    private String resolveScope(Player player, String raw) {
        String lowered = raw.toLowerCase(Locale.ROOT);
        if (lowered.equals(LobbyZoneService.GENERAL_SCOPE)) {
            return LobbyZoneService.GENERAL_SCOPE;
        }
        GameEvent event = eventManager.getType(lowered).orElse(null);
        if (event == null) {
            messages.send(player, "lobbyzone.invalid-scope", Placeholder.unparsed("scope", raw));
            sounds.play(player, "command-error");
            return null;
        }
        return event.getId();
    }

    private String displayScope(String scope) {
        if (LobbyZoneService.GENERAL_SCOPE.equals(scope)) {
            return messages.rawOr("lobbyzone.scope-general", "General de la arena");
        }
        return messages.rawOr("event.names." + scope, scope);
    }

    private String selectionKey(Arena arena, String scope) {
        return arena.getName().toLowerCase(Locale.ROOT) + "|" + scope;
    }

    private void markCorner(Player player, Arena arena, String scope,
                            Map<UUID, Map<String, ArenaLocation>> store, String corner) {
        store.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
                .put(selectionKey(arena, scope), ArenaLocation.from(player.getLocation()));
        messages.send(player, "lobbyzone.pos-set",
                Placeholder.unparsed("corner", corner),
                Placeholder.parsed("scope", displayScope(scope)),
                Placeholder.unparsed("arena", arena.getName()));
        sounds.play(player, "command-success");
        trySaveRegion(player, arena, scope);
    }

    private void trySaveRegion(Player player, Arena arena, String scope) {
        String key = selectionKey(arena, scope);
        ArenaLocation first = firstCorner.getOrDefault(player.getUniqueId(), Collections.emptyMap()).get(key);
        ArenaLocation second = secondCorner.getOrDefault(player.getUniqueId(), Collections.emptyMap()).get(key);
        if (first == null || second == null) {
            return;
        }
        if (!first.getWorldName().equals(second.getWorldName())) {
            messages.send(player, "lobbyzone.different-worlds");
            sounds.play(player, "command-error");
            return;
        }
        Region region = Region.of(first, second);
        zones.setRegion(arena.getName(), scope, region);
        firstCorner.get(player.getUniqueId()).remove(key);
        secondCorner.get(player.getUniqueId()).remove(key);
        messages.send(player, "lobbyzone.region-set",
                Placeholder.unparsed("arena", arena.getName()),
                Placeholder.parsed("scope", displayScope(scope)),
                Placeholder.unparsed("dimensions", region.describe()));
        sounds.play(player, "command-success");
    }

    private void applySetting(Player player, Arena arena, String scope, String[] args) {
        if (args.length >= 5 && !zones.getZone(arena.getName(), scope).isPresent()) {
            messages.send(player, "lobbyzone.no-region",
                    Placeholder.unparsed("arena", arena.getName()),
                    Placeholder.parsed("scope", displayScope(scope)));
            sounds.play(player, "command-error");
            return;
        }
        if (args.length < 5 || !zones.setProtection(arena.getName(), scope, args[3], args[4])) {
            messages.send(player, "lobbyzone.set-usage", Placeholder.unparsed("options",
                    String.join(", ", LobbyProtectionSettings.SETTING_KEYS)));
            sounds.play(player, "command-error");
            return;
        }
        messages.send(player, "lobbyzone.setting-updated",
                Placeholder.unparsed("arena", arena.getName()),
                Placeholder.parsed("scope", displayScope(scope)),
                Placeholder.unparsed("setting", args[3].toLowerCase(Locale.ROOT)),
                Placeholder.unparsed("value", args[4].toLowerCase(Locale.ROOT)));
        sounds.play(player, "command-success");
    }

    private void sendInfo(Player player, Arena arena, String scope) {
        LobbyZoneService.LobbyZone zone = zones.getZone(arena.getName(), scope).orElse(null);
        messages.send(player, "lobbyzone.info-header",
                Placeholder.unparsed("arena", arena.getName()),
                Placeholder.parsed("scope", displayScope(scope)));
        messages.send(player, "lobbyzone.info-region", Placeholder.parsed("state",
                zone != null ? "<yellow>" + zone.getRegion().describe()
                        : messages.rawOr("mainlobby.state-unset", "sin definir")));
        if (zone == null) {
            return;
        }
        for (String key : LobbyProtectionSettings.SETTING_KEYS) {
            messages.send(player, "mainlobby.info-protection",
                    Placeholder.unparsed("setting", key),
                    Placeholder.parsed("state", messages.rawOr(
                            zone.getSettings().isEnabled(key)
                                    ? "lockout.state-on" : "lockout.state-off", "?")));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> scopes = new ArrayList<>();
            scopes.add(LobbyZoneService.GENERAL_SCOPE);
            for (GameEvent event : eventManager.getTypes()) {
                scopes.add(event.getId());
            }
            return filter(scopes, args[0]);
        }
        if (args.length == 2) {
            return filter(new ArrayList<>(arenaManager.getNames()), args[1]);
        }
        if (args.length == 3) {
            return filter(Arrays.asList("pos1", "pos2", "removeregion", "set", "info"), args[2]);
        }
        if (args.length == 4 && args[2].equalsIgnoreCase("set")) {
            return filter(LobbyProtectionSettings.SETTING_KEYS, args[3]);
        }
        if (args.length == 5 && args[2].equalsIgnoreCase("set")) {
            return filter(Arrays.asList("true", "false"), args[4]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
