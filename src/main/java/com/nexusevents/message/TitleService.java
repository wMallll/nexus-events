package com.nexusevents.message;

import com.nexusevents.configuration.model.TitleConfig;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Servicio de titulos y actionbars.
 *
 * <p>Se apoya en Adventure a traves de {@link MessageService}, lo que
 * garantiza funcionamiento desde Minecraft 1.9 sin NMS. Los textos
 * soportan legacy, HEX, MiniMessage y placeholders dinamicos, y los
 * tiempos de animacion provienen del {@link TitleConfig}.</p>
 */
public final class TitleService {

    private static final long MILLIS_PER_TICK = 50L;

    private final MessageService messages;

    public TitleService(MessageService messages) {
        this.messages = messages;
    }

    /**
     * Muestra al jugador el titulo configurado con sus tiempos.
     * Si el titulo esta desactivado, no hace nada.
     *
     * @param player    jugador receptor.
     * @param config    titulo configurado.
     * @param resolvers placeholders dinamicos.
     */
    public void showTitle(Player player, TitleConfig config, TagResolver... resolvers) {
        if (config == null || !config.isEnabled()) {
            return;
        }
        Title title = Title.title(
                messages.parse(config.getTitle(), resolvers),
                messages.parse(config.getSubtitle(), resolvers),
                Title.Times.times(
                        ticksToDuration(config.getFadeInTicks()),
                        ticksToDuration(config.getStayTicks()),
                        ticksToDuration(config.getFadeOutTicks())
                )
        );
        messages.audience(player).showTitle(title);
    }

    /**
     * Envia una actionbar al jugador.
     *
     * @param player    jugador receptor.
     * @param raw       texto crudo (legacy/HEX/MiniMessage).
     * @param resolvers placeholders dinamicos.
     */
    public void sendActionBar(Player player, String raw, TagResolver... resolvers) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        messages.audience(player).sendActionBar(messages.parse(raw, resolvers));
    }

    /**
     * Limpia el titulo visible del jugador.
     *
     * @param player jugador receptor.
     */
    public void clearTitle(Player player) {
        messages.audience(player).clearTitle();
    }

    private Duration ticksToDuration(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * MILLIS_PER_TICK);
    }
}
