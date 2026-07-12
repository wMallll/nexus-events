package com.nexusevents.permission;

/**
 * Nodos de permisos del plugin, centralizados en un unico lugar.
 *
 * <p>Todo el codigo referencia estas constantes en vez de literales,
 * lo que evita errores de tipeo y facilita mantener la documentacion
 * de permisos para LuckPerms.</p>
 */
public final class Permissions {

    /** Acceso total al plugin (nodo padre). */
    public static final String ADMIN = "evento.admin";

    /** Uso basico de /evento y la ayuda. */
    public static final String USE = "evento.use";

    /** Recarga de configuracion. */
    public static final String RELOAD = "evento.reload";

    /** Informacion de version. */
    public static final String VERSION = "evento.version";

    /** Creacion de arenas. */
    public static final String ARENA_CREATE = "evento.arena.create";

    /** Eliminacion de arenas. */
    public static final String ARENA_DELETE = "evento.arena.delete";

    /** Listado de arenas. */
    public static final String ARENA_LIST = "evento.arena.list";

    /** Seleccion e inspeccion de arenas para edicion. */
    public static final String ARENA_EDIT = "evento.arena.edit";

    /** Guardado manual de todas las arenas. */
    public static final String SETUP_SAVE = "evento.setup.save";

    /** Inicio de eventos. */
    public static final String START = "evento.start";

    /** Detencion de eventos. */
    public static final String STOP = "evento.stop";

    /** Union a eventos como jugador. */
    public static final String JOIN = "evento.join";

    /** Salida de eventos como jugador. */
    public static final String LEAVE = "evento.leave";

    /** Listado de eventos disponibles y activos. */
    public static final String EVENTS_LIST = "evento.events";

    /** Control del modo torneo (bloqueo de eliminados). */
    public static final String LOCKOUT = "evento.lockout";

    /** Exencion de los efectos del modo torneo (para admins). */
    public static final String LOCKOUT_BYPASS = "evento.lockout.bypass";

    /** Gestor de mundos del plugin. */
    public static final String WORLD = "evento.world";

    /** Menu principal del plugin. */
    public static final String MENU = "evento.menu";

    /** Teletransportar a todos los eliminados hasta el ejecutor. */
    public static final String TP_DEAD = "evento.tpdead";

    /** Descalificar participantes. */
    public static final String DISQUALIFY = "evento.disqualify";

    /** Recibir y usar el Palo Descalificador. */
    public static final String DQ_STICK = "evento.dqstick";

    /** Exencion de las protecciones de construccion e interaccion del
     * lobby global. */
    public static final String MAINLOBBY_BYPASS = "evento.mainlobby.bypass";

    /** Interruptor global de la colision entre jugadores. */
    public static final String COLLISION = "evento.collision";

    /**
     * Construye el nodo de permiso de un comando de setup concreto
     * (por ejemplo {@code evento.setup.setspawn}).
     *
     * @param commandName nombre del subcomando de setup.
     * @return nodo de permiso correspondiente.
     */
    public static String setup(String commandName) {
        return "evento.setup." + commandName;
    }

    private Permissions() {
        throw new UnsupportedOperationException("Clase de constantes: no debe instanciarse.");
    }
}
