package com.nexusevents.event.parkour;

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
 * Definicion del evento Parkour Colapsable.
 *
 * <p>Requiere la region del recorrido configurada con
 * {@code /evento setparkour}. El spawn de la arena marca el inicio del
 * recorrido: la onda de colapso avanza desde alli. La configuracion se
 * parsea al crear cada sesion, por lo que {@code /evento reload}
 * aplica los cambios a partir de la siguiente partida.</p>
 */
public final class ParkourEvent implements GameEvent {

    public static final String ID = "parkour";
    public static final String CONFIG_FILE = "events/parkour.yml";

    private final ConfigManager configManager;
    private final Logger logger;

    public ParkourEvent(ConfigManager configManager, Logger logger) {
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
        return Collections.emptyList();
    }

    @Override
    public List<String> getRequiredRegions() {
        return Collections.singletonList(ArenaKeys.PARKOUR_FRAGMENT_PREFIX + 1);
    }

    @Override
    public EventSession createSession(EventContext context, Arena arena) {
        ParkourConfig config = ParkourConfig.parse(configManager.getFile(CONFIG_FILE).get(), logger);
        return new ParkourSession(context, this, arena, config);
    }
}
