package com.nexusevents.moderation;

import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.configuration.ConfigurationFile;
import com.nexusevents.manager.Manager;
import com.nexusevents.scheduler.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Colision entre jugadores a nivel SERVIDOR: un unico interruptor
 * global, persistente entre reinicios ({@code collision.yml}).
 *
 * <p>Con la colision desactivada, todos los jugadores se atraviesan
 * entre si (se aplica al instante y a cada uno que entra). Al
 * reactivarla, se restaura unicamente a quienes este servicio toco,
 * sin pisar a otros plugins. Los participantes de eventos quedan
 * siempre exentos: sus sesiones gestionan la colision de los
 * espectadores.</p>
 */
public final class CollisionService implements Manager {

    public static final String FILE = "collision.yml";
    private static final String PATH = "disabled";
    private static final long PERIOD_TICKS = 20L;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final TaskScheduler scheduler;
    private final Set<UUID> suppressed = new HashSet<>();

    private Predicate<UUID> participantCheck = playerId -> false;
    private boolean disabled;
    private BukkitTask task;

    public CollisionService(JavaPlugin plugin, ConfigManager configManager, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scheduler = scheduler;
    }

    /**
     * Define el filtro de participantes de eventos (exentos del
     * interruptor global).
     *
     * @param participantCheck predicado por UUID.
     */
    public void setParticipantCheck(Predicate<UUID> participantCheck) {
        this.participantCheck = participantCheck;
    }

    @Override
    public String getName() {
        return "Colision";
    }

    @Override
    public void enable() {
        configManager.register(FILE);
        this.disabled = configManager.getFile(FILE).get().getBoolean(PATH, false);
        this.task = scheduler.syncTimer(this::enforce, PERIOD_TICKS, PERIOD_TICKS);
        if (disabled) {
            plugin.getLogger().info("Colision entre jugadores: DESACTIVADA en todo el servidor.");
        }
    }

    @Override
    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        restoreAll();
    }

    /**
     * Indica si la colision esta desactivada en el servidor.
     *
     * @return true si los jugadores se atraviesan.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Activa o desactiva la colision global, aplicandolo de inmediato
     * a todos los online y persistiendo el estado.
     *
     * @param disabled true para que los jugadores se atraviesan.
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        ConfigurationFile file = configManager.getFile(FILE);
        file.get().set(PATH, disabled);
        file.save();
        if (disabled) {
            enforce();
        } else {
            restoreAll();
        }
    }

    /**
     * Con la colision desactivada, garantiza cada segundo que todos
     * los no-participantes online (incluidos recien llegados) esten
     * sin colision.
     */
    private void enforce() {
        if (!disabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (participantCheck.test(id)) {
                continue;
            }
            if (suppressed.add(id)) {
                player.setCollidable(false);
            }
        }
        suppressed.removeIf(id -> Bukkit.getPlayer(id) == null);
    }

    /**
     * Restaura la colision unicamente a los jugadores que este
     * servicio modifico.
     */
    private void restoreAll() {
        for (UUID id : suppressed) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && !participantCheck.test(id)) {
                player.setCollidable(true);
            }
        }
        suppressed.clear();
    }
}
