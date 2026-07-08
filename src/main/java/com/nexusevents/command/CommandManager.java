package com.nexusevents.command;

import com.nexusevents.command.sub.HelpSubCommand;
import com.nexusevents.command.sub.ReloadSubCommand;
import com.nexusevents.command.sub.VersionSubCommand;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.ManagerRegistry;
import com.nexusevents.message.MessageService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dispatcher del comando raiz {@code /evento}.
 *
 * <p>Resuelve subcomandos por nombre o alias, valida permisos por
 * subcomando y delega el autocompletado. Agregar un nuevo subcomando en
 * fases futuras se reduce a instanciarlo y registrarlo.</p>
 */
public final class CommandManager implements Manager, CommandExecutor, TabCompleter {

    private static final String ROOT_COMMAND = "evento";

    private final JavaPlugin plugin;
    private final ManagerRegistry managerRegistry;
    private final MessageService messages;

    private final Map<String, SubCommand> byName = new LinkedHashMap<>();
    private final Map<String, SubCommand> lookup = new HashMap<>();

    public CommandManager(JavaPlugin plugin, ManagerRegistry managerRegistry, MessageService messages) {
        this.plugin = plugin;
        this.managerRegistry = managerRegistry;
        this.messages = messages;
    }

    @Override
    public String getName() {
        return "Comandos";
    }

    @Override
    public void enable() {
        registerDefaults();
        PluginCommand command = plugin.getCommand(ROOT_COMMAND);
        if (command == null) {
            plugin.getLogger().severe("El comando '" + ROOT_COMMAND + "' no esta definido en plugin.yml.");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public void disable() {
        byName.clear();
        lookup.clear();
    }

    private void registerDefaults() {
        register(new HelpSubCommand(this, messages));
        register(new ReloadSubCommand(managerRegistry, messages));
        register(new VersionSubCommand(plugin, messages));
    }

    /**
     * Registra un subcomando bajo su nombre y todos sus aliases.
     *
     * @param subCommand subcomando a registrar.
     */
    public void register(SubCommand subCommand) {
        String key = subCommand.getName().toLowerCase(Locale.ROOT);
        byName.put(key, subCommand);
        lookup.put(key, subCommand);
        for (String alias : subCommand.getAliases()) {
            lookup.put(alias.toLowerCase(Locale.ROOT), subCommand);
        }
    }

    /**
     * Devuelve los subcomandos registrados (sin duplicados por alias).
     *
     * @return subcomandos registrados en orden de registro.
     */
    public Collection<SubCommand> getSubCommands() {
        return Collections.unmodifiableCollection(byName.values());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            dispatchHelp(sender);
            return true;
        }

        SubCommand subCommand = lookup.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            messages.send(sender, "general.unknown-command", Placeholder.unparsed("input", args[0]));
            return true;
        }
        if (!hasPermission(sender, subCommand)) {
            messages.send(sender, "general.no-permission");
            return true;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    private void dispatchHelp(CommandSender sender) {
        SubCommand help = lookup.get("help");
        if (help != null && hasPermission(sender, help)) {
            help.execute(sender, new String[0]);
            return;
        }
        messages.send(sender, "general.no-permission");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return suggestSubCommands(sender, args[0]);
        }
        SubCommand subCommand = lookup.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand != null && hasPermission(sender, subCommand)) {
            return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        return Collections.emptyList();
    }

    private List<String> suggestSubCommands(CommandSender sender, String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (SubCommand subCommand : byName.values()) {
            if (hasPermission(sender, subCommand) && subCommand.getName().startsWith(lowered)) {
                suggestions.add(subCommand.getName());
            }
        }
        return suggestions;
    }

    private boolean hasPermission(CommandSender sender, SubCommand subCommand) {
        String permission = subCommand.getPermission();
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }
}
