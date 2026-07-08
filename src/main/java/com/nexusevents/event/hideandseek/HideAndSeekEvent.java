package com.nexusevents.event.hideandseek;

import com.nexusevents.arena.Arena;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.event.EventContext;
import com.nexusevents.event.EventSession;
import com.nexusevents.event.GameEvent;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Definicion del evento "Escondete si puedes".
 *
 * <p>Registra su propio archivo de configuracion
 * ({@code events/hide-and-seek.yml}) y parsea la configuracion al crear
 * cada sesion, de modo que {@code /evento reload} aplica los cambios a
 * partir de la siguiente partida.</p>
 */
public final class HideAndSeekEvent implements GameEvent {

    public static final String ID = "hide-and-seek";
    public static final String CONFIG_FILE = "events/hide-and-seek.yml";

    private final ConfigManager configManager;
    private final Logger logger;

    public HideAndSeekEvent(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
        configManager.register(CONFIG_FILE);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getRequiredPoints() {
        return Collections.singletonList(ArenaKeys.HUNTER_SPAWN);
    }

    @Override
    public List<String> getRequiredRegions() {
        return Collections.emptyList();
    }

    @Override
    public EventSession createSession(EventContext context, Arena arena) {
        HideAndSeekConfig config = HideAndSeekConfig.parse(
                configManager.getFile(CONFIG_FILE).get(), logger);
        return new HideAndSeekSession(context, this, arena, config);
    }
}
