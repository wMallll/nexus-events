package com.nexusevents.bootstrap;

import com.nexusevents.NexusEventsPlugin;
import com.nexusevents.arena.ArenaKeys;
import com.nexusevents.arena.ArenaManager;
import com.nexusevents.arena.SetupSessionService;
import com.nexusevents.command.CommandManager;
import com.nexusevents.command.sub.arena.ArenaInfoSubCommand;
import com.nexusevents.command.sub.arena.CreateArenaSubCommand;
import com.nexusevents.command.sub.arena.DeleteArenaSubCommand;
import com.nexusevents.command.sub.arena.ListArenasSubCommand;
import com.nexusevents.command.sub.arena.SelectArenaSubCommand;
import com.nexusevents.command.sub.event.EventsSubCommand;
import com.nexusevents.command.sub.event.JoinEventSubCommand;
import com.nexusevents.command.sub.event.CollisionSubCommand;
import com.nexusevents.command.sub.event.DisqualifyStickSubCommand;
import com.nexusevents.command.sub.event.DisqualifySubCommand;
import com.nexusevents.command.sub.event.LockoutSubCommand;
import com.nexusevents.command.sub.event.TpDeadSubCommand;
import com.nexusevents.command.sub.menu.MenuSubCommand;
import com.nexusevents.command.sub.event.LeaveEventSubCommand;
import com.nexusevents.command.sub.event.StartEventSubCommand;
import com.nexusevents.command.sub.event.StopEventSubCommand;
import com.nexusevents.command.sub.setup.SaveSubCommand;
import com.nexusevents.command.sub.setup.ParkourSetupSubCommand;
import com.nexusevents.command.sub.setup.SetCircleSubCommand;
import com.nexusevents.command.sub.setup.LobbyZoneSubCommand;
import com.nexusevents.command.sub.setup.MainLobbySetupSubCommand;
import com.nexusevents.command.sub.setup.SetMainLobbySubCommand;
import com.nexusevents.command.sub.setup.SetMinYSubCommand;
import com.nexusevents.command.sub.setup.SetPointSubCommand;
import com.nexusevents.command.sub.setup.SetRegionSubCommand;
import com.nexusevents.compatibility.ServerVersion;
import com.nexusevents.configuration.ConfigManager;
import com.nexusevents.event.EventManager;
import com.nexusevents.event.circle.CircleEvent;
import com.nexusevents.event.hideandseek.HideAndSeekEvent;
import com.nexusevents.event.parkour.ParkourEvent;
import com.nexusevents.event.pixelparty.PixelPartyEvent;
import com.nexusevents.lobby.MainLobbyService;
import com.nexusevents.lobby.LobbyZoneService;
import com.nexusevents.moderation.CollisionService;
import com.nexusevents.lockout.LockoutService;
import com.nexusevents.command.sub.world.WorldSubCommand;
import com.nexusevents.world.WorldService;
import com.nexusevents.listener.EventProtectionListener;
import com.nexusevents.listener.DisqualifyListener;
import com.nexusevents.listener.LockoutListener;
import com.nexusevents.listener.LobbyZoneListener;
import com.nexusevents.listener.MainLobbyListener;
import com.nexusevents.menu.MenuListener;
import com.nexusevents.menu.MenuService;
import com.nexusevents.listener.ListenerManager;
import com.nexusevents.listener.PlayerConnectionListener;
import com.nexusevents.manager.Manager;
import com.nexusevents.manager.ManagerRegistry;
import com.nexusevents.message.MessageService;
import com.nexusevents.message.TitleService;
import com.nexusevents.placeholder.NexusEventsExpansion;
import com.nexusevents.scheduler.TaskScheduler;
import com.nexusevents.scoreboard.ScoreboardTemplateRegistry;
import com.nexusevents.sound.SoundService;
import com.nexusevents.storage.YamlArenaStorage;
import org.bukkit.Bukkit;

/**
 * Orquesta el ciclo de vida completo del plugin.
 *
 * <p>Es el unico punto de composicion ("composition root"): aca se crean
 * los servicios core y se registran los managers en orden de dependencia.
 * El apagado se realiza en orden inverso desde {@link ManagerRegistry}.</p>
 */
public final class PluginBootstrap {

    private final NexusEventsPlugin plugin;
    private final ManagerRegistry managerRegistry;
    private final TaskScheduler taskScheduler;
    private final ListenerManager listenerManager;
    private final SetupSessionService setupSessions;

    private TitleService titleService;

    public PluginBootstrap(NexusEventsPlugin plugin) {
        this.plugin = plugin;
        this.managerRegistry = new ManagerRegistry(plugin.getLogger());
        this.taskScheduler = new TaskScheduler(plugin);
        this.listenerManager = new ListenerManager(plugin);
        this.setupSessions = new SetupSessionService();
    }

    public void enable() {
        long start = System.currentTimeMillis();

        ServerVersion version = ServerVersion.get();
        plugin.getLogger().info("Version del servidor detectada: " + version.getDisplayName()
                + (version.isPaper() ? " (Paper)" : " (Spigot/Bukkit)"));

        registerManagers();
        managerRegistry.enableAll();
        registerListeners();

        plugin.getLogger().info("NexusEvents v" + plugin.getDescription().getVersion()
                + " habilitado en " + (System.currentTimeMillis() - start) + "ms.");
    }

    public void disable() {
        managerRegistry.disableAll();
        listenerManager.unregisterAll();
        taskScheduler.cancelAll();
        plugin.getLogger().info("NexusEvents deshabilitado correctamente.");
    }

    /**
     * Registra los managers en orden de dependencia:
     * configuracion -> mensajes -> sonidos -> scoreboards -> arenas -> comandos.
     */
    private void registerManagers() {
        ConfigManager configManager = new ConfigManager(plugin);
        MessageService messageService = new MessageService(plugin, configManager);
        SoundService soundService = new SoundService(plugin, configManager);
        ScoreboardTemplateRegistry scoreboardTemplates = new ScoreboardTemplateRegistry(plugin, configManager);
        ArenaManager arenaManager = new ArenaManager(plugin, new YamlArenaStorage(plugin));
        MainLobbyService mainLobbyService = new MainLobbyService(plugin, configManager,
                taskScheduler, messageService);
        LockoutService lockoutService = new LockoutService(plugin, configManager);
        LobbyZoneService lobbyZoneService = new LobbyZoneService(plugin, configManager);
        CollisionService collisionService = new CollisionService(plugin, configManager, taskScheduler);
        WorldService worldService = new WorldService(plugin, configManager, taskScheduler);
        CommandManager commandManager = new CommandManager(plugin, managerRegistry, messageService);

        this.titleService = new TitleService(plugin, messageService);

        EventManager eventManager = new EventManager(plugin, configManager, taskScheduler,
                messageService, titleService, soundService, scoreboardTemplates, arenaManager,
                lockoutService, mainLobbyService);
        eventManager.register(new HideAndSeekEvent(configManager, plugin.getLogger()));
        eventManager.register(new PixelPartyEvent(configManager, plugin.getLogger()));
        eventManager.register(new ParkourEvent(configManager, plugin.getLogger()));
        eventManager.register(new CircleEvent(configManager, plugin.getLogger()));

        mainLobbyService.setParticipantCheck(playerId ->
                eventManager.getSessionByPlayer(playerId).isPresent());
        lobbyZoneService.setParticipantCheck(playerId ->
                eventManager.getSessionByPlayer(playerId).isPresent());
        collisionService.setParticipantCheck(playerId ->
                eventManager.getSessionByPlayer(playerId).isPresent());

        MenuService menuService = new MenuService(plugin, taskScheduler, messageService,
                arenaManager, eventManager, worldService, lockoutService, mainLobbyService,
                lobbyZoneService, collisionService);

        registerArenaCommands(commandManager, arenaManager, eventManager, mainLobbyService,
                lobbyZoneService, messageService, soundService);
        registerEventCommands(commandManager, eventManager, lockoutService, messageService, soundService);
        commandManager.register(new WorldSubCommand(worldService, messageService, soundService));
        commandManager.register(new MenuSubCommand(menuService, messageService));
        commandManager.register(new TpDeadSubCommand(eventManager, messageService, soundService));
        commandManager.register(new DisqualifySubCommand(eventManager, lockoutService, messageService, soundService));
        commandManager.register(new DisqualifyStickSubCommand(messageService, soundService));
        commandManager.register(new CollisionSubCommand(collisionService, messageService, soundService));

        managerRegistry.register(ConfigManager.class, configManager);
        managerRegistry.register(MessageService.class, messageService);
        managerRegistry.register(SoundService.class, soundService);
        managerRegistry.register(ScoreboardTemplateRegistry.class, scoreboardTemplates);
        managerRegistry.register(ArenaManager.class, arenaManager);
        managerRegistry.register(MainLobbyService.class, mainLobbyService);
        managerRegistry.register(LobbyZoneService.class, lobbyZoneService);
        managerRegistry.register(CollisionService.class, collisionService);
        managerRegistry.register(LockoutService.class, lockoutService);
        managerRegistry.register(WorldService.class, worldService);
        managerRegistry.register(MenuService.class, menuService);
        managerRegistry.register(EventManager.class, eventManager);
        managerRegistry.register(CommandManager.class, commandManager);
    }

    /**
     * Registra los comandos de administracion de arenas y de setup.
     */
    private void registerArenaCommands(CommandManager commands, ArenaManager arenas,
                                       EventManager events, MainLobbyService mainLobby,
                                       LobbyZoneService lobbyZones,
                                       MessageService messages, SoundService sounds) {
        commands.register(new CreateArenaSubCommand(arenas, setupSessions, messages, sounds));
        commands.register(new DeleteArenaSubCommand(arenas, setupSessions, messages, sounds));
        commands.register(new ListArenasSubCommand(arenas, messages));
        commands.register(new SelectArenaSubCommand(arenas, setupSessions, messages, sounds));
        commands.register(new ArenaInfoSubCommand(arenas, setupSessions, messages));

        commands.register(new SetPointSubCommand("setspawn", ArenaKeys.SPAWN, arenas, setupSessions, events, messages, sounds));
        commands.register(new SetPointSubCommand("setlobby", ArenaKeys.LOBBY, arenas, setupSessions, events, messages, sounds));
        commands.register(new SetPointSubCommand("sethunterspawn", ArenaKeys.HUNTER_SPAWN, arenas, setupSessions, events, messages, sounds));
        commands.register(new SetCircleSubCommand(arenas, setupSessions, messages, sounds));

        commands.register(new SetRegionSubCommand("setpixelparty", ArenaKeys.REGION_PIXEL_PARTY, arenas, setupSessions, messages, sounds));
        commands.register(new ParkourSetupSubCommand(arenas, setupSessions, messages, sounds));

        commands.register(new SaveSubCommand(arenas, messages, sounds));
        commands.register(new SetMainLobbySubCommand(mainLobby, messages, sounds));
        commands.register(new SetMinYSubCommand(arenas, setupSessions, events, mainLobby, messages, sounds));
        commands.register(new MainLobbySetupSubCommand(mainLobby, messages, sounds));
        commands.register(new LobbyZoneSubCommand(lobbyZones, arenas, events, messages, sounds));
    }

    /**
     * Registra los comandos del sistema de eventos.
     */
    private void registerEventCommands(CommandManager commands, EventManager events,
                                       LockoutService lockouts, MessageService messages,
                                       SoundService sounds) {
        commands.register(new StartEventSubCommand(events, messages, sounds));
        commands.register(new StopEventSubCommand(events, messages, sounds));
        commands.register(new JoinEventSubCommand(events, messages, sounds));
        commands.register(new LeaveEventSubCommand(events, messages, sounds));
        commands.register(new EventsSubCommand(events, messages));
        commands.register(new LockoutSubCommand(lockouts, messages, sounds));
    }

    /**
     * Registra los listeners globales del plugin una vez que todos los
     * managers estan habilitados.
     */
    private void registerListeners() {
        EventManager eventManager = managerRegistry.get(EventManager.class);
        MainLobbyService mainLobbyService = managerRegistry.get(MainLobbyService.class);
        LockoutService lockoutService = managerRegistry.get(LockoutService.class);
        MessageService lockoutMessages = managerRegistry.get(MessageService.class);
        MenuService menuService = managerRegistry.get(MenuService.class);
        listenerManager.registerAll(
                new PlayerConnectionListener(eventManager, mainLobbyService, taskScheduler),
                new EventProtectionListener(eventManager),
                new LockoutListener(lockoutService, lockoutMessages),
                new DisqualifyListener(eventManager, lockoutService, lockoutMessages),
                new MainLobbyListener(mainLobbyService),
                new LobbyZoneListener(managerRegistry.get(LobbyZoneService.class)),
                new MenuListener(menuService)
        );
        registerIntegrations(eventManager);
    }

    /**
     * Registra integraciones opcionales con plugins de terceros
     * presentes en el servidor.
     */
    private void registerIntegrations(EventManager eventManager) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexusEventsExpansion(plugin, eventManager,
                    managerRegistry.get(MessageService.class)).register();
            plugin.getLogger().info("PlaceholderAPI detectado: placeholders %nexusevents_...% registrados.");
        }
    }

    public <T extends Manager> T getManager(Class<T> type) {
        return managerRegistry.get(type);
    }

    public NexusEventsPlugin getPlugin() {
        return plugin;
    }

    public ManagerRegistry getManagerRegistry() {
        return managerRegistry;
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public TitleService getTitleService() {
        return titleService;
    }

    public SetupSessionService getSetupSessions() {
        return setupSessions;
    }

    public ListenerManager getListenerManager() {
        return listenerManager;
    }
}
