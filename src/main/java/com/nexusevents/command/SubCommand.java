package com.nexusevents.command;

import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Clase base de todo subcomando de {@code /evento}.
 *
 * <p>Cada subcomando declara su propio permiso, cumpliendo el requisito
 * de permisos granulares compatibles con LuckPerms. Las descripciones se
 * resuelven desde {@code messages.yml} (ruta
 * {@code help.descriptions.<nombre>}) para mantenerlas configurables.</p>
 */
public abstract class SubCommand {

    private final String name;
    private final String permission;
    private final String usage;
    private final List<String> aliases;

    protected SubCommand(String name, String permission, String usage, String... aliases) {
        this.name = name;
        this.permission = permission;
        this.usage = usage;
        this.aliases = Collections.unmodifiableList(Arrays.asList(aliases));
    }

    /**
     * Ejecuta el subcomando.
     *
     * @param sender emisor del comando.
     * @param args   argumentos sin incluir el nombre del subcomando.
     */
    public abstract void execute(CommandSender sender, String[] args);

    /**
     * Provee sugerencias de autocompletado para los argumentos propios
     * del subcomando. Por defecto no sugiere nada.
     *
     * @param sender emisor del comando.
     * @param args   argumentos sin incluir el nombre del subcomando.
     * @return lista de sugerencias.
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public String getUsage() {
        return usage;
    }

    public List<String> getAliases() {
        return aliases;
    }
}
