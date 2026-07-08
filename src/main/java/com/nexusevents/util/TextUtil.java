package com.nexusevents.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.regex.Pattern;

/**
 * Motor de texto del plugin.
 *
 * <p>Unifica los tres formatos soportados en un unico pipeline:
 * primero convierte los codigos legacy ({@code &a}, {@code §b}) y los
 * colores HEX ({@code &#RRGGBB}) a tags de MiniMessage, y luego
 * deserializa el resultado con MiniMessage. Esto permite mezclar los
 * tres formatos dentro de un mismo mensaje configurado en YAML.</p>
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Pattern HEX_PATTERN = Pattern.compile("[&\u00A7]#([0-9A-Fa-f]{6})");

    private TextUtil() {
        throw new UnsupportedOperationException("Clase de utilidad: no debe instanciarse.");
    }

    /**
     * Convierte una cadena configurada por el usuario en un componente
     * Adventure, aceptando legacy, HEX y MiniMessage combinados.
     *
     * @param input     texto crudo desde la configuracion.
     * @param resolvers placeholders de MiniMessage a aplicar.
     * @return componente listo para enviarse a un jugador o consola.
     */
    public static Component parse(String input, TagResolver... resolvers) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(input), resolvers);
    }

    /**
     * Convierte una cadena configurada a texto legacy con codigos de
     * seccion, necesario para APIs de Bukkit que solo aceptan String
     * (nombres y lore de items en versiones antiguas).
     *
     * @param input     texto crudo desde la configuracion.
     * @param resolvers placeholders de MiniMessage a aplicar.
     * @return texto en formato legacy.
     */
    public static String toLegacy(String input, TagResolver... resolvers) {
        return LEGACY.serialize(parse(input, resolvers));
    }

    /**
     * Convierte codigos legacy y HEX de una cadena a sus tags MiniMessage
     * equivalentes, dejando intactos los tags MiniMessage existentes.
     *
     * @param input texto crudo.
     * @return texto expresado unicamente en sintaxis MiniMessage.
     */
    public static String legacyToMiniMessage(String input) {
        String withHex = HEX_PATTERN.matcher(input).replaceAll("<#$1>");
        StringBuilder builder = new StringBuilder(withHex.length());
        for (int i = 0; i < withHex.length(); i++) {
            char current = withHex.charAt(i);
            if ((current == '&' || current == '\u00A7') && i + 1 < withHex.length()) {
                String tag = legacyTag(Character.toLowerCase(withHex.charAt(i + 1)));
                if (tag != null) {
                    builder.append(tag);
                    i++;
                    continue;
                }
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static String legacyTag(char code) {
        switch (code) {
            case '0': return "<black>";
            case '1': return "<dark_blue>";
            case '2': return "<dark_green>";
            case '3': return "<dark_aqua>";
            case '4': return "<dark_red>";
            case '5': return "<dark_purple>";
            case '6': return "<gold>";
            case '7': return "<gray>";
            case '8': return "<dark_gray>";
            case '9': return "<blue>";
            case 'a': return "<green>";
            case 'b': return "<aqua>";
            case 'c': return "<red>";
            case 'd': return "<light_purple>";
            case 'e': return "<yellow>";
            case 'f': return "<white>";
            case 'k': return "<obfuscated>";
            case 'l': return "<bold>";
            case 'm': return "<strikethrough>";
            case 'n': return "<underlined>";
            case 'o': return "<italic>";
            case 'r': return "<reset>";
            default: return null;
        }
    }
}
