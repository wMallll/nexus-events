package com.nexusevents.command.sub.setup;

import com.nexusevents.command.PlayerSubCommand;
import com.nexusevents.lobby.MainLobbyService;
import com.nexusevents.message.MessageService;
import com.nexusevents.permission.Permissions;
import com.nexusevents.sound.SoundService;
import org.bukkit.entity.Player;

/**
 * Subcomando {@code /evento setmainlobby}.
 *
 * <p>Fija el lobby global del servidor: el punto donde apareceran los
 * jugadores la primera vez que entren. Las reconexiones mantienen el
 * comportamiento vanilla (aparecer donde se desconectaron).</p>
 */
public final class SetMainLobbySubCommand extends PlayerSubCommand {

    private final MainLobbyService mainLobby;
    private final MessageService messages;
    private final SoundService sounds;

    public SetMainLobbySubCommand(MainLobbyService mainLobby, MessageService messages, SoundService sounds) {
        super(messages, "setmainlobby", Permissions.setup("setmainlobby"),
                "/evento setmainlobby", "setgloballobby");
        this.mainLobby = mainLobby;
        this.messages = messages;
        this.sounds = sounds;
    }

    @Override
    protected void executePlayer(Player player, String[] args) {
        mainLobby.setLobby(player.getLocation());
        messages.send(player, "arena.mainlobby-set");
        sounds.play(player, "command-success");
    }
}
