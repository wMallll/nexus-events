package com.nexusevents.command.sub;

import com.nexusevents.command.SubCommand;
import com.nexusevents.compatibility.ServerVersion;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Subcomando {@code /evento version}.
 *
 * <p>Informa la version del plugin, la version del servidor y la
 * plataforma detectada por la capa de compatibilidad.</p>
 */
public final class VersionSubCommand extends SubCommand {

    private final JavaPlugin plugin;
    private final MessageService messages;

    public VersionSubCommand(JavaPlugin plugin, MessageService messages) {
        super("version", Permissions.VERSION, "/evento version", "ver", "info");
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ServerVersion version = ServerVersion.get();
        messages.send(sender, "general.version",
                Placeholder.unparsed("version", plugin.getDescription().getVersion()),
                Placeholder.unparsed("server", version.getDisplayName()),
                Placeholder.unparsed("platform", version.isPaper() ? "Paper" : "Spigot/Bukkit"));
    }
}
