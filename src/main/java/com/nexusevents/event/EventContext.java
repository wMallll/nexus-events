package com.nexusevents.event;

import com.nexusevents.arena.ArenaManager;
import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.lockout.LockoutService;
import com.nexusevents.message.MessageService;
import com.nexusevents.message.TitleService;
import com.nexusevents.scheduler.TaskScheduler;
import com.nexusevents.scoreboard.ScoreboardTemplateRegistry;
import com.nexusevents.sound.SoundService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Conjunto inmutable de servicios que reciben las sesiones de evento.
 *
 * <p>Evita constructores enormes en cada evento concreto y desacopla a
 * los eventos del bootstrap: un evento solo conoce este contexto.</p>
 */
public final class EventContext {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final TaskScheduler scheduler;
    private final MessageService messages;
    private final TitleService titles;
    private final SoundService sounds;
    private final ScoreboardTemplateRegistry scoreboards;
    private final ArenaManager arenas;
    private final LockoutService lockouts;
    private final EventManager eventManager;

    EventContext(JavaPlugin plugin, ConfigManager configManager, TaskScheduler scheduler,
                 MessageService messages, TitleService titles, SoundService sounds,
                 ScoreboardTemplateRegistry scoreboards, ArenaManager arenas,
                 LockoutService lockouts, EventManager eventManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.messages = messages;
        this.titles = titles;
        this.sounds = sounds;
        this.scoreboards = scoreboards;
        this.arenas = arenas;
        this.lockouts = lockouts;
        this.eventManager = eventManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TaskScheduler getScheduler() {
        return scheduler;
    }

    public MessageService getMessages() {
        return messages;
    }

    public TitleService getTitles() {
        return titles;
    }

    public SoundService getSounds() {
        return sounds;
    }

    public ScoreboardTemplateRegistry getScoreboards() {
        return scoreboards;
    }

    public ArenaManager getArenas() {
        return arenas;
    }

    public LockoutService getLockouts() {
        return lockouts;
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}
