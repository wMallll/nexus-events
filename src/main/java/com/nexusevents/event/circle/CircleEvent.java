package com.nexusevents.event.circle;

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
 * Definicion del evento "El Circulo que se cierra".
 *
 * <p>Requiere el punto central configurado con {@code /evento setcircle}.
 * La configuracion se parsea al crear cada sesion, por lo que
 * {@code /evento reload} aplica los cambios a partir de la siguiente
 * partida.</p>
 */
public final class CircleEvent implements GameEvent {

    public static final String ID = "circle";
    public static final String CONFIG_FILE = "events/circle.yml";

    private final ConfigManager configManager;
    private final Logger logger;

    public CircleEvent(ConfigManager configManager, Logger logger) {
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
        return Collections.singletonList(ArenaKeys.CIRCLE_CENTER);
    }

    @Override
    public List<String> getRequiredRegions() {
        return Collections.emptyList();
    }

    @Override
    public EventSession createSession(EventContext context, Arena arena) {
        CircleConfig config = CircleConfig.parse(configManager.getFile(CONFIG_FILE).get(), logger);
        return new CircleSession(context, this, arena, config);
    }
}
