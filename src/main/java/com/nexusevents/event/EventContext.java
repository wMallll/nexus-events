package com.nexusevents.event;

import com.nexusevents.arena.ArenaManager;
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
    private final TaskScheduler scheduler;
    private final MessageService messages;
    private final TitleService titles;
    private final SoundService sounds;
    private final ScoreboardTemplateRegistry scoreboards;
    private final ArenaManager arenas;
    private final EventManager eventManager;

    EventContext(JavaPlugin plugin, TaskScheduler scheduler, MessageService messages,
                 TitleService titles, SoundService sounds, ScoreboardTemplateRegistry scoreboards,
                 ArenaManager arenas, EventManager eventManager) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.messages = messages;
        this.titles = titles;
        this.sounds = sounds;
        this.scoreboards = scoreboards;
        this.arenas = arenas;
        this.eventManager = eventManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
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

    public EventManager getEventManager() {
        return eventManager;
    }
}
