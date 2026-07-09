package com.nexusevents.placeholder;

import com.nexusevents.event.EventManager;
import com.nexusevents.event.EventSession;
import com.nexusevents.message.MessageService;
import com.nexusevents.util.TextUtil;
import com.nexusevents.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

/**
 * Expansion de PlaceholderAPI del plugin.
 *
 * <p>Se registra unicamente si PlaceholderAPI esta instalado (la clase
 * no se carga en caso contrario). Placeholders disponibles:</p>
 * <ul>
 *   <li>{@code %nexusevents_active%} - cantidad de eventos activos.</li>
 *   <li>{@code %nexusevents_event%} - nombre del evento del jugador.</li>
 *   <li>{@code %nexusevents_arena%} - arena del evento del jugador.</li>
 *   <li>{@code %nexusevents_state%} - estado del evento del jugador.</li>
 *   <li>{@code %nexusevents_alive%} - vivos en el evento del jugador.</li>
 *   <li>{@code %nexusevents_spectators%} - eliminados en su evento.</li>
 *   <li>{@code %nexusevents_time%} - tiempo visible de su evento.</li>
 * </ul>
 */
public final class NexusEventsExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final EventManager eventManager;
    private final MessageService messages;

    public NexusEventsExpansion(JavaPlugin plugin, EventManager eventManager, MessageService messages) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.messages = messages;
    }

    @Override
    public String getIdentifier() {
        return "nexusevents";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (key.equals("active")) {
            return String.valueOf(eventManager.getSessions().size());
        }
        if (player == null) {
            return "";
        }
        EventSession session = eventManager.getSessionByPlayer(player.getUniqueId()).orElse(null);
        if (session == null) {
            return "";
        }
        switch (key) {
            case "event":
                return TextUtil.toLegacy(session.displayName());
            case "arena":
                return session.getArena().getName();
            case "state":
                return TextUtil.toLegacy(messages.rawOr(
                        "event.states." + session.getState().getKey(), session.getState().getKey()));
            case "alive":
                return String.valueOf(session.getAliveCount());
            case "spectators":
                return String.valueOf(session.getEliminatedCount());
            case "time":
                return TimeUtil.formatSeconds(Math.max(0, session.getElapsedSeconds()));
            default:
                return null;
        }
    }
}
