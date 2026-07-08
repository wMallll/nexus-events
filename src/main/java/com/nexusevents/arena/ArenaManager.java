package com.nexusevents.arena;

import com.nexusevents.manager.Manager;
import com.nexusevents.manager.Reloadable;
import com.nexusevents.storage.ArenaStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Administra el ciclo de vida de todas las arenas.
 *
 * <p>Las arenas se cargan al arranque, se guardan automaticamente tras
 * cada modificacion de setup y se consultan por nombre sin distinguir
 * mayusculas. La persistencia se delega en {@link ArenaStorage}.</p>
 */
public final class ArenaManager implements Manager, Reloadable {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");

    private final JavaPlugin plugin;
    private final ArenaStorage storage;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(JavaPlugin plugin, ArenaStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @Override
    public String getName() {
        return "Arenas";
    }

    @Override
    public void enable() {
        loadFromStorage();
    }

    @Override
    public void disable() {
        saveAll();
        arenas.clear();
    }

    @Override
    public void reload() {
        arenas.clear();
        loadFromStorage();
    }

    private void loadFromStorage() {
        for (Arena arena : storage.loadAll()) {
            arenas.put(keyOf(arena.getName()), arena);
        }
        plugin.getLogger().info("Cargadas " + arenas.size() + " arenas.");
    }

    /**
     * Valida si un nombre de arena es aceptable
     * (letras, numeros, guion y guion bajo; 1 a 32 caracteres).
     *
     * @param name nombre propuesto.
     * @return true si el nombre es valido.
     */
    public boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    public boolean exists(String name) {
        return arenas.containsKey(keyOf(name));
    }

    /**
     * Crea una arena nueva y la persiste inmediatamente.
     *
     * @param name nombre validado y no existente.
     * @return arena creada.
     */
    public Arena create(String name) {
        Arena arena = new Arena(name);
        arenas.put(keyOf(name), arena);
        storage.save(arena);
        return arena;
    }

    /**
     * Elimina una arena y su persistencia.
     *
     * @param name nombre de la arena.
     * @return true si existia y fue eliminada.
     */
    public boolean delete(String name) {
        Arena removed = arenas.remove(keyOf(name));
        if (removed == null) {
            return false;
        }
        storage.delete(removed);
        return true;
    }

    /**
     * Obtiene una arena por nombre (sin distinguir mayusculas).
     *
     * @param name nombre de la arena.
     * @return arena, si existe.
     */
    public Optional<Arena> get(String name) {
        return Optional.ofNullable(arenas.get(keyOf(name)));
    }

    /**
     * Persiste una arena puntual (se invoca tras cada cambio de setup).
     *
     * @param arena arena a guardar.
     */
    public void save(Arena arena) {
        storage.save(arena);
    }

    /**
     * Persiste todas las arenas.
     *
     * @return cantidad de arenas guardadas.
     */
    public int saveAll() {
        for (Arena arena : arenas.values()) {
            storage.save(arena);
        }
        return arenas.size();
    }

    public Collection<Arena> getAll() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    /**
     * Nombres de todas las arenas, para listados y tab-complete.
     *
     * @return nombres originales de las arenas.
     */
    public List<String> getNames() {
        return arenas.values().stream().map(Arena::getName).collect(Collectors.toList());
    }

    private String keyOf(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
