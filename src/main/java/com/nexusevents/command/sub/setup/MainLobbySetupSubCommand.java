package com.nexusevents.command.sub.setup;

import com.nexusevents.arena.ArenaLocation;
import com.nexusevents.arena.Region;
import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.lobby.LobbyProtectionSettings;
import com.nexusevents.lobby.MainLobbyService;
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
 * Subcomando {@code /evento mainlobby} para el armado completo del
 * lobby global: region protegida (pos1 + pos2, se guarda al tener
 * ambas), protecciones toggleables, altura minima e info.
 */
public final class MainLobbySetupSubCommand extends PlayerSubCommand {

    private final MainLobbyService lobby;
    private final MessageService messages;
    private final SoundService sounds;

    private final Map<UUID, ArenaLocation> firstCorner = new HashMap<>();
    private final Map<UUID, ArenaLocation> secondCorner = new HashMap<>();

    public MainLobbySetupSubCommand(MainLobbyService lobby, MessageService messages, SoundService sounds) {
        super(messages, "mainlobby", Permissions.setup("mainlobby"),
                "/evento mainlobby (pos1|pos2|removeregion|set (opción) (valor)|setminy|info)",
                "lobbyglobal");
        this.lobby = lobby;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        if (args.length < 1) {
            messages.send(player, "mainlobby.usage");
            sounds.play(player, "command-error");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pos1":
                markCorner(player, firstCorner, "1");
                break;
            case "pos2":
                markCorner(player, secondCorner, "2");
                break;
            case "removeregion":
                lobby.removeRegion();
                messages.send(player, "mainlobby.region-removed");
                sounds.play(player, "command-success");
                break;
            case "set":
                applySetting(player, args);
                break;
            case "setminy":
                lobby.setMinY(player.getLocation().getBlockY());
                messages.send(player, "arena.miny-set",
                        Placeholder.unparsed("y", String.valueOf(player.getLocation().getBlockY())),
                        Placeholder.parsed("scope", messages.rawOr("arena.miny-scope-main", "Lobby global")));
                sounds.play(player, "command-success");
                break;
            case "info":
                sendInfo(player);
                break;
            default:
                messages.send(player, "mainlobby.usage");
                sounds.play(player, "command-error");
                break;
        }
    }

    private void markCorner(Player player, Map<UUID, ArenaLocation> store, String corner) {
        store.put(player.getUniqueId(), ArenaLocation.from(player.getLocation()));
        messages.send(player, "mainlobby.pos-set", Placeholder.unparsed("corner", corner));
        sounds.play(player, "command-success");
        trySaveRegion(player);
    }

    /**
     * Con ambas esquinas marcadas (y en el mismo mundo), la region se
     * guarda automaticamente y la seleccion se limpia.
     */
    private void trySaveRegion(Player player) {
        ArenaLocation first = firstCorner.get(player.getUniqueId());
        ArenaLocation second = secondCorner.get(player.getUniqueId());
        if (first == null || second == null) {
            return;
        }
        if (!first.getWorldName().equals(second.getWorldName())) {
            messages.send(player, "mainlobby.different-worlds");
            sounds.play(player, "command-error");
            return;
        }
        Region region = Region.of(first, second);
        lobby.setRegion(region);
        firstCorner.remove(player.getUniqueId());
        secondCorner.remove(player.getUniqueId());
        messages.send(player, "mainlobby.region-set",
                Placeholder.unparsed("dimensions", region.describe()));
        sounds.play(player, "command-success");
    }

    private void applySetting(Player player, String[] args) {
        if (args.length < 3 || !lobby.setProtection(args[1], args[2])) {
            messages.send(player, "mainlobby.set-usage", Placeholder.unparsed("options",
                    String.join(", ", LobbyProtectionSettings.SETTING_KEYS)));
            sounds.play(player, "command-error");
            return;
        }
        messages.send(player, "mainlobby.setting-updated",
                Placeholder.unparsed("setting", args[1].toLowerCase(Locale.ROOT)),
                Placeholder.unparsed("value", args[2].toLowerCase(Locale.ROOT)));
        sounds.play(player, "command-success");
    }

    private void sendInfo(Player player) {
        messages.send(player, "mainlobby.info-header");
        messages.send(player, "mainlobby.info-spawn", Placeholder.parsed("state",
                messages.rawOr(lobby.getLobby().isPresent()
                        ? "mainlobby.state-set" : "mainlobby.state-unset", "?")));
        messages.send(player, "mainlobby.info-region", Placeholder.parsed("state",
                lobby.getRegion().map(Region::describe).map(dimensions -> "<yellow>" + dimensions)
                        .orElseGet(() -> messages.rawOr("mainlobby.state-unset", "sin definir"))));
        messages.send(player, "mainlobby.info-miny", Placeholder.parsed("state",
                lobby.getMinY().map(minY -> "<yellow>Y=" + minY)
                        .orElseGet(() -> messages.rawOr("mainlobby.state-unset", "sin definir"))));
        for (String key : LobbyProtectionSettings.SETTING_KEYS) {
            messages.send(player, "mainlobby.info-protection",
                    Placeholder.unparsed("setting", key),
                    Placeholder.parsed("state", messages.rawOr(
                            lobby.getProtection().isEnabled(key)
                                    ? "lockout.state-on" : "lockout.state-off", "?")));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("pos1", "pos2", "removeregion", "set", "setminy", "info"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filter(LobbyProtectionSettings.SETTING_KEYS, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(Arrays.asList("true", "false"), args[2]);
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
