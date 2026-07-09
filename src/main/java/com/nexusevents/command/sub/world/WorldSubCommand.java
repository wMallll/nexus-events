package com.nexusevents.command.sub.world;

import com.nexusevents.command.SubCommand;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import com.nexusevents.world.WorldDefinition;
import com.nexusevents.world.WorldService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Subcomando {@code /evento world} (gestor de mundos estilo Multiverse).
 *
 * <p>Acciones: {@code create (nombre) [normal|nether|end]},
 * {@code tp (nombre)}, {@code list}, {@code load (nombre)},
 * {@code set (nombre) (opcion) (valor)}, {@code setspawn [nombre]} y
 * {@code delete (nombre) confirm}.</p>
 */
public final class WorldSubCommand extends SubCommand {

    private final WorldService worlds;
    private final MessageService messages;
    private final SoundService sounds;

    public WorldSubCommand(WorldService worlds, MessageService messages, SoundService sounds) {
        super("world", Permissions.WORLD, "/evento world", "mundo", "mundos");
        this.worlds = worlds;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            messages.send(sender, "world.usage");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":
                create(sender, args);
                break;
            case "tp":
                teleport(sender, args);
                break;
            case "list":
                list(sender);
                break;
            case "load":
                load(sender, args);
                break;
            case "set":
                set(sender, args);
                break;
            case "setspawn":
                setSpawn(sender, args);
                break;
            case "delete":
                delete(sender, args);
                break;
            default:
                messages.send(sender, "world.usage");
                sounds.play(sender, "command-error");
                break;
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "world.usage");
            sounds.play(sender, "command-error");
            return;
        }
        World.Environment environment = parseEnvironment(args.length >= 3 ? args[2] : "normal");
        if (environment == null) {
            messages.send(sender, "world.invalid-environment");
            sounds.play(sender, "command-error");
            return;
        }
        String name = args[1];
        messages.send(sender, "world.creating", Placeholder.unparsed("world", name));
        WorldService.CreateResult result = worlds.create(name, environment);
        switch (result) {
            case SUCCESS:
                messages.send(sender, "world.created", Placeholder.unparsed("world", name));
                sounds.play(sender, "command-success");
                if (sender instanceof Player) {
                    teleportTo((Player) sender, Bukkit.getWorld(name));
                }
                break;
            case ALREADY_EXISTS:
                messages.send(sender, "world.exists", Placeholder.unparsed("world", name));
                sounds.play(sender, "command-error");
                break;
            case INVALID_NAME:
                messages.send(sender, "world.invalid-name");
                sounds.play(sender, "command-error");
                break;
            default:
                messages.send(sender, "world.create-failed", Placeholder.unparsed("world", name));
                sounds.play(sender, "command-error");
                break;
        }
    }

    private World.Environment parseEnvironment(String raw) {
        switch (raw.toLowerCase(Locale.ROOT)) {
            case "normal":
                return World.Environment.NORMAL;
            case "nether":
                return World.Environment.NETHER;
            case "end":
            case "the_end":
                return World.Environment.THE_END;
            default:
                return null;
        }
    }

    private void teleport(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || args.length < 2) {
            if (player != null) {
                messages.send(sender, "world.usage");
                sounds.play(sender, "command-error");
            }
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            world = worlds.load(args[1]).orElse(null);
        }
        if (world == null) {
            messages.send(sender, "world.not-found", Placeholder.unparsed("world", args[1]));
            sounds.play(sender, "command-error");
            return;
        }
        teleportTo(player, world);
    }

    private void teleportTo(Player player, World world) {
        if (world == null) {
            return;
        }
        Location spawn = world.getSpawnLocation().add(0.5, 0.1, 0.5);
        player.teleport(spawn);
        messages.send(player, "world.teleported", Placeholder.unparsed("world", world.getName()));
        sounds.play(player, "command-success");
    }

    private void list(CommandSender sender) {
        if (worlds.getDefinitions().isEmpty()) {
            messages.send(sender, "world.list-empty");
            return;
        }
        messages.send(sender, "world.list-header",
                Placeholder.unparsed("count", String.valueOf(worlds.getDefinitions().size())));
        for (WorldDefinition definition : worlds.getDefinitions()) {
            boolean loaded = Bukkit.getWorld(definition.getName()) != null;
            messages.send(sender, "world.list-entry",
                    Placeholder.unparsed("world", definition.getName()),
                    Placeholder.unparsed("environment", definition.getEnvironment().name()),
                    Placeholder.parsed("status", messages.rawOr(
                            loaded ? "world.status-loaded" : "world.status-unloaded", "?")));
        }
    }

    private void load(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "world.usage");
            sounds.play(sender, "command-error");
            return;
        }
        if (!worlds.isPluginWorld(args[1])) {
            messages.send(sender, "world.not-found", Placeholder.unparsed("world", args[1]));
            sounds.play(sender, "command-error");
            return;
        }
        if (worlds.load(args[1]).isPresent()) {
            messages.send(sender, "world.loaded", Placeholder.unparsed("world", args[1]));
            sounds.play(sender, "command-success");
        } else {
            messages.send(sender, "world.load-failed", Placeholder.unparsed("world", args[1]));
            sounds.play(sender, "command-error");
        }
    }

    private void set(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, "world.set-usage", Placeholder.unparsed("options",
                    String.join(", ", WorldDefinition.SETTING_KEYS)));
            sounds.play(sender, "command-error");
            return;
        }
        if (!worlds.isPluginWorld(args[1])) {
            messages.send(sender, "world.not-found", Placeholder.unparsed("world", args[1]));
            sounds.play(sender, "command-error");
            return;
        }
        if (worlds.setSetting(args[1], args[2], args[3])) {
            messages.send(sender, "world.setting-updated",
                    Placeholder.unparsed("world", args[1]),
                    Placeholder.unparsed("setting", args[2].toLowerCase(Locale.ROOT)),
                    Placeholder.unparsed("value", args[3].toLowerCase(Locale.ROOT)));
            sounds.play(sender, "command-success");
        } else {
            messages.send(sender, "world.set-usage", Placeholder.unparsed("options",
                    String.join(", ", WorldDefinition.SETTING_KEYS)));
            sounds.play(sender, "command-error");
        }
    }

    private void setSpawn(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        String name = args.length >= 2 ? args[1] : player.getWorld().getName();
        if (!worlds.isPluginWorld(name) || !player.getWorld().getName().equalsIgnoreCase(name)) {
            messages.send(sender, "world.spawn-must-stand");
            sounds.play(sender, "command-error");
            return;
        }
        Location at = player.getLocation();
        player.getWorld().setSpawnLocation(at.getBlockX(), at.getBlockY(), at.getBlockZ());
        messages.send(sender, "world.spawn-set", Placeholder.unparsed("world", player.getWorld().getName()));
        sounds.play(sender, "command-success");
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "world.usage");
            sounds.play(sender, "command-error");
            return;
        }
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            messages.send(sender, "world.delete-confirm", Placeholder.unparsed("world", args[1]));
            return;
        }
        WorldService.DeleteResult result = worlds.delete(args[1]);
        switch (result) {
            case SUCCESS:
                messages.send(sender, "world.deleted", Placeholder.unparsed("world", args[1]));
                sounds.play(sender, "command-success");
                break;
            case NOT_FOUND:
                messages.send(sender, "world.not-found", Placeholder.unparsed("world", args[1]));
                sounds.play(sender, "command-error");
                break;
            case MAIN_WORLD:
                messages.send(sender, "world.main-world");
                sounds.play(sender, "command-error");
                break;
            default:
                messages.send(sender, "world.delete-failed", Placeholder.unparsed("world", args[1]));
                sounds.play(sender, "command-error");
                break;
        }
    }

    private Player asPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        messages.send(sender, "world.player-only");
        return null;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "tp", "list", "load", "set", "setspawn", "delete"), args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("create") && !args[0].equalsIgnoreCase("list")) {
            List<String> names = new ArrayList<>();
            for (WorldDefinition definition : worlds.getDefinitions()) {
                names.add(definition.getName());
            }
            return filter(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return filter(Arrays.asList("normal", "nether", "end"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(WorldDefinition.SETTING_KEYS, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            if (args[2].equalsIgnoreCase("difficulty")) {
                return filter(Arrays.asList("peaceful", "easy", "normal", "hard"), args[3]);
            }
            if (args[2].equalsIgnoreCase("time")) {
                return filter(Arrays.asList("0", "6000", "13000", "18000"), args[3]);
            }
            return filter(Arrays.asList("true", "false"), args[3]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("delete")) {
            return Collections.singletonList("confirm");
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
