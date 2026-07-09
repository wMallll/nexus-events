package com.nexusevents.message;

import com.nexusevents.configuration.model.TitleConfig;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio de titulos y actionbars.
 *
 * <p>Los titulos se envian por la API nativa de Bukkit
 * ({@code Player#sendTitle}), disponible desde 1.9 en todas las
 * versiones y plataformas: si el servidor soporta la variante con
 * tiempos de animacion (1.11+), se usa; si no, la basica. Esto evita
 * depender de mecanismos internos de terceros que se rompen con cada
 * version nueva del servidor. Las actionbars se envian via Adventure.</p>
 */
public final class TitleService {

    /** Player#sendTitle(String, String, int, int, int), presente desde 1.11. */
    private static final Method SEND_TITLE_WITH_TIMES = resolveTimedMethod();

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final AtomicBoolean warnedFallback = new AtomicBoolean(false);

    public TitleService(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    private static Method resolveTimedMethod() {
        try {
            return Player.class.getMethod("sendTitle",
                    String.class, String.class, int.class, int.class, int.class);
        } catch (NoSuchMethodException exception) {
            return null;
        }
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
        String title = messages.legacy(config.getTitle(), resolvers);
        String subtitle = messages.legacy(config.getSubtitle(), resolvers);
        if (SEND_TITLE_WITH_TIMES != null) {
            try {
                SEND_TITLE_WITH_TIMES.invoke(player, title, subtitle,
                        config.getFadeInTicks(), config.getStayTicks(), config.getFadeOutTicks());
                return;
            } catch (ReflectiveOperationException exception) {
                if (warnedFallback.compareAndSet(false, true)) {
                    plugin.getLogger().warning("sendTitle con tiempos fallo en esta version ("
                            + exception + "): se usa la variante basica.");
                }
            }
        }
        sendBasicTitle(player, title, subtitle);
    }

    @SuppressWarnings("deprecation")
    private void sendBasicTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle);
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
}
