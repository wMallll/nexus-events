package com.nexusevents.message;

import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import com.nexusevents.util.TextUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Servicio central de mensajeria del plugin.
 *
 * <p>Resuelve los mensajes desde {@code messages.yml} (valores simples o
 * listas multilinea), inyecta automaticamente el placeholder global
 * {@code <prefix>} y los envia mediante Adventure, lo que garantiza
 * soporte de MiniMessage, RGB y legacy desde Minecraft 1.9.</p>
 */
public final class MessageService implements Manager, Reloadable {

    private static final String PREFIX_PATH = "prefix";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private BukkitAudiences audiences;
    private String prefix;

    public MessageService(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "Mensajes";
    }

    @Override
    public void enable() {
        this.audiences = BukkitAudiences.create(plugin);
        loadPrefix();
    }

    @Override
    public void disable() {
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }

    @Override
    public void reload() {
        loadPrefix();
    }

    private void loadPrefix() {
        this.prefix = configManager.getMessages().getString(PREFIX_PATH, "");
    }

    /**
     * Envia al receptor el mensaje configurado en la ruta dada.
     * Si el valor es una lista YAML, envia cada linea por separado.
     *
     * @param sender    receptor (jugador o consola).
     * @param path      ruta del mensaje dentro de messages.yml.
     * @param resolvers placeholders dinamicos adicionales.
     */
    public void send(CommandSender sender, String path, TagResolver... resolvers) {
        TagResolver[] combined = withPrefix(resolvers);
        for (String line : rawLines(path)) {
            audiences.sender(sender).sendMessage(TextUtil.parse(line, combined));
        }
    }

    /**
     * Envia el mensaje configurado a todos los jugadores del servidor
     * y a la consola. Si el valor es una lista, envia cada linea.
     *
     * @param path      ruta del mensaje dentro de messages.yml.
     * @param resolvers placeholders dinamicos adicionales.
     */
    public void broadcast(String path, TagResolver... resolvers) {
        TagResolver[] combined = withPrefix(resolvers);
        for (String line : rawLines(path)) {
            audiences.all().sendMessage(TextUtil.parse(line, combined));
        }
    }

    /**
     * Devuelve la audiencia Adventure del receptor, para enviarle
     * titulos, actionbars o bossbars.
     *
     * @param sender receptor (jugador o consola).
     * @return audiencia Adventure asociada.
     */
    public Audience audience(CommandSender sender) {
        return audiences.sender(sender);
    }

    /**
     * Parsea una cadena arbitraria aplicando el prefijo global y los
     * placeholders dados, sin enviarla.
     *
     * @param raw       texto crudo.
     * @param resolvers placeholders dinamicos adicionales.
     * @return componente resultante.
     */
    public Component parse(String raw, TagResolver... resolvers) {
        return TextUtil.parse(raw, withPrefix(resolvers));
    }

    /**
     * Parsea una cadena y la devuelve en formato legacy (codigos de
     * seccion), para APIs de Bukkit que solo aceptan String: titulos
     * nativos, scoreboards y bossbars.
     *
     * @param raw       texto crudo.
     * @param resolvers placeholders dinamicos adicionales.
     * @return texto legacy resultante.
     */
    public String legacy(String raw, TagResolver... resolvers) {
        return com.nexusevents.util.TextUtil.toLegacy(raw, withPrefix(resolvers));
    }

    /**
     * Devuelve el texto crudo configurado en la ruta dada.
     *
     * @param path ruta dentro de messages.yml.
     * @return texto crudo o un aviso de mensaje faltante.
     */
    public String raw(String path) {
        String value = configManager.getMessages().getString(path);
        return value != null ? value : missing(path);
    }

    /**
     * Devuelve el texto crudo configurado en la ruta dada, o el
     * fallback si la ruta no existe.
     *
     * @param path     ruta dentro de messages.yml.
     * @param fallback texto a devolver si la ruta no existe.
     * @return texto crudo o fallback.
     */
    public String rawOr(String path, String fallback) {
        String value = configManager.getMessages().getString(path);
        return value != null ? value : fallback;
    }

    private List<String> rawLines(String path) {
        FileConfiguration messages = configManager.getMessages();
        if (messages.isList(path)) {
            List<String> lines = messages.getStringList(path);
            return lines.isEmpty() ? Collections.singletonList(missing(path)) : lines;
        }
        String value = messages.getString(path);
        return Collections.singletonList(value != null ? value : missing(path));
    }

    private String missing(String path) {
        return "<red>Mensaje no encontrado: <yellow>" + path;
    }

    private TagResolver[] withPrefix(TagResolver[] resolvers) {
        TagResolver[] combined = new TagResolver[resolvers.length + 1];
        combined[0] = Placeholder.parsed("prefix", prefix != null ? prefix : "");
        System.arraycopy(resolvers, 0, combined, 1, resolvers.length);
        return combined;
    }
}
