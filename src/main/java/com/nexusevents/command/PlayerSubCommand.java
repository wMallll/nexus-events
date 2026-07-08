package com.nexusevents.command;

import com.nexusevents.message.MessageService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Subcomando que solo puede ejecutar un jugador dentro del juego
 * (por ejemplo, los comandos de setup que usan su posicion fisica).
 *
 * <p>Centraliza la validacion y el mensaje de error, evitando repetir
 * el chequeo en cada comando.</p>
 */
public abstract class PlayerSubCommand extends SubCommand {

    private final MessageService playerOnlyMessages;

    protected PlayerSubCommand(MessageService messages, String name, String permission,
                               String usage, String... aliases) {
        super(name, permission, usage, aliases);
        this.playerOnlyMessages = messages;
    }

    @Override
    public final void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            playerOnlyMessages.send(sender, "general.player-only");
            return;
        }
        executePlayer((Player) sender, args);
    }

    /**
     * Ejecuta el subcomando con el jugador ya validado.
     *
     * @param player jugador emisor.
     * @param args   argumentos sin incluir el nombre del subcomando.
     */
    protected abstract void executePlayer(Player player, String[] args);
}
