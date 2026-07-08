package com.nexusevents.event.pixelparty;

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
 * Definicion del evento Pixel Party.
 *
 * <p>Requiere la region de plataforma configurada con
 * {@code /evento setpixelparty}. La configuracion se parsea al crear
 * cada sesion, por lo que {@code /evento reload} aplica los cambios a
 * partir de la siguiente partida.</p>
 */
public final class PixelPartyEvent implements GameEvent {

    public static final String ID = "pixel-party";
    public static final String CONFIG_FILE = "events/pixel-party.yml";

    private final ConfigManager configManager;
    private final Logger logger;

    public PixelPartyEvent(ConfigManager configManager, Logger logger) {
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
        return Collections.singletonList(ArenaKeys.REGION_PIXEL_PARTY);
    }

    @Override
    public EventSession createSession(EventContext context, Arena arena) {
        PixelPartyConfig config = PixelPartyConfig.parse(
                configManager.getFile(CONFIG_FILE).get(), logger);
        return new PixelPartySession(context, this, arena, config);
    }
}
