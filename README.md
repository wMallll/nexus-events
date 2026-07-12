# NexusEvents

Plugin profesional de **eventos multi-arena** para servidores de Minecraft, compatible desde **1.9 hasta la última versión** con un único jar y sin NMS.

Cuatro eventos completos, **menú GUI para todo**, gestor de mundos vacíos estilo Multiverse, modo torneo, herramientas de moderación, arenas con setup 100% desde el juego y configuración total por YAML.

---

## Eventos incluidos

| Evento | Id | Descripción |
|---|---|---|
| **Escondete si puedes** | `hide-and-seek` | Todos se esconden durante la cuenta regresiva; el cazador sale con su arma configurable y elimina de un golpe. Reconexión con ventana configurable para desconexiones involuntarias. |
| **Pixel Party** | `pixel-party` | La plataforma se pinta y los jugadores aparecen en su centro. Suspenso aleatorio de "decisión" → se revela el color (título + bloque en la hotbar) → los incorrectos desaparecen. Rondas cada vez más rápidas; termina por tiempo y ganan los que sigan arriba. |
| **Parkour por Islas** | `parkour` | Recorrido de fragmentos (islas) ordenados que se desintegran uno por uno, cada uno de forma progresiva y con animación de rotura. Pausas configurables entre islas concretas. Último en pie gana. |
| **El Círculo** | `circle` | Piso circular (estilo `//cyl`) cuyo suelo se rompe en grupos de bloques contiguos, en posiciones y momentos aleatorios. PvP activo con bolas de nieve (empuje garantizado). Último vivo gana. |

Nadie muere de verdad en ningún evento: los golpes o caídas letales eliminan limpiamente (sin drops ni respawn), con snapshot completo del jugador y restauración garantizada al salir — la salida es siempre al **lobby del evento** (nunca a la posición previa).

## Características

- **Compatibilidad 1.9 → última versión** con un solo jar (XSeries, FastBoard, títulos y bossbars por API nativa).
- **Menú GUI universal** (`/evento menu`): absolutamente todo operable sin comandos — cada botón ejecuta el comando real por debajo, con los mismos permisos y validaciones.
- **Gestor de mundos vacíos** estilo Multiverse: mundos void con mobs, día permanente, clima, PvP, dificultad y keep-inventory configurables en vivo.
- **Modo torneo** (`lockout`): los eliminados quedan bloqueados (sin comandos, sin reingreso al servidor, sin unirse a eventos) hasta el `clear` — persistente.
- **Moderación**: descalificación por comando o con el **Palo Descalificador** (funciona incluso sin evento activo, vía modo torneo), y teleport masivo de eliminados.
- **Espectadores en vanish real**: los vivos no los ven ni en el mundo ni en el TAB; entre eliminados sí se ven.
- **Región protegida del lobby global** con 11 protecciones configurables en vivo (vida, hambre, PvP, mobs, explosiones, bloques, fuego, items e interacción) y permiso de bypass para construir.
- **Zonas de lobby por evento y arena**: cada arena puede definir una zona protegida por evento (más una `general` como fallback) con las mismas 11 protecciones — p. ej. `/evento lobbyzone parkour test pos1` — editable por comando o desde las pestañas del editor en el menú.
- **Colisión entre jugadores a nivel servidor**: un interruptor global (`/evento collision on|off`, persistente) hace que todos los jugadores se atraviesen entre sí, con toggle en el menú de Moderación.
- **Red de seguridad de altura** por arena, por evento y para el lobby global; además, en el mundo del lobby global **el vacío no mata**: el daño se cancela y el jugador vuelve al spawn del lobby (y si algo lo mata igual, reaparece ahí).
- **Fusión automática de configuración**: al actualizar, las claves nuevas se agregan solas sin pisar tus ediciones.
- Lobby global de primer ingreso, lobby/spawn opcionales **por evento**, scoreboards FastBoard, bossbar con progreso, integración PlaceholderAPI.

## Instalación

1. Colocar `NexusEvents-x.x.x.jar` en `plugins/` y reiniciar.
2. Configurar desde el juego (comandos o `/evento menu`) y jugar.

## Comandos

Raíz: `/evento` (aliases `/eventos`, `/nexusevents`). `(arg)` obligatorio, `[arg]` opcional.

**Generales:** `help` · `version` · `reload` · `menu` (abre la GUI, permiso para todos)

**Arenas:** `createarena (nombre)` · `deletearena (nombre) confirm` · `arenas` · `select (nombre)` · `arena [nombre]` · `save`

**Setup (usan tu posición actual):**
`setlobby [evento]` · `setspawn [evento]` · `sethunterspawn` · `setcircle (radio)` · `setpixelparty (1|2)` · `parkour (pos1|pos2|add|remove (n)|list|clear)` · `setminy [evento|main]` · `setmainlobby` · `mainlobby (pos1|pos2|set (opción) (valor)|removeregion|setminy|info)` · `lobbyzone (evento|general) (arena) (pos1|pos2|set (opción) (valor)|removeregion|info)`

**Control de eventos:** `start (evento) (arena)` · `stop [arena]` · `events` · `join [arena]` · `leave`

**Mundos:** `world create (nombre) [normal|nether|end]` · `world tp (nombre)` · `world list` · `world load (nombre)` · `world set (nombre) (opción) (valor)` · `world setspawn [nombre]` · `world delete (nombre) confirm`
Opciones de `set`: `spawn-mobs`, `always-day`, `time`, `no-rain`, `pvp`, `keep-inventory`, `difficulty`.

**Moderación:** `tpdead` (trae a todos los eliminados hasta vos, cross-mundo) · `disqualify (jugador)` · `dqstick` (Palo Descalificador) · `lockout (on|off|clear)` (modo torneo) · `collision (on|off)` (colisión global del servidor)

Permiso maestro: `evento.admin`. Cada comando tiene su nodo granular (ver `plugin.yml`); `join`, `leave` y `menu` vienen abiertos a todos, `evento.lockout.bypass` exime a los admins del modo torneo y `evento.mainlobby.bypass`, de las protecciones de construcción e interacción del lobby global.

## Setup rápido por evento

Base: `createarena` → `setlobby` → `setspawn` (o sus variantes por evento) → `setminy` recomendado.

- **hide-and-seek:** `+ sethunterspawn`.
- **pixel-party:** `+ setpixelparty 1|2` marcando la plataforma **al nivel de sus bloques** (los jugadores aparecen solos en el centro).
- **parkour:** por cada isla, `parkour pos1` → `pos2` → `add`, en el orden en que deben caer. `remove (n)` renumera el resto.
- **circle:** construí el piso (`//cyl stone 100 1`), parate en el **centro sobre el piso** y `setcircle 100`.

Verificá con `/evento arena` y arrancá con `/evento start (evento) (arena)` — si falta algo, el chat te dice exactamente qué.

## Archivos de configuración

`config.yml` (global + bossbar) · `messages.yml` · `sounds.yml` · `scoreboards.yml` · `events/*.yml` (uno por evento) · `worlds.yml` · `lobby.yml` (lobby global: spawn, altura, región y protecciones) · `lobbyzones.yml` (zonas de lobby por arena) · `lockouts.yml` (modo torneo) · `arenas/*.yml`. Duraciones con sufijos `t/s/m/h`.

## Placeholders (PlaceholderAPI)

`%nexusevents_active%` · `%nexusevents_event%` · `%nexusevents_arena%` · `%nexusevents_state%` · `%nexusevents_alive%` · `%nexusevents_spectators%` · `%nexusevents_time%`

## Compilar desde el código

JDK 8–21 y Maven: `mvn clean package` → `target/NexusEvents-x.x.x.jar` (librerías empaquetadas y relocadas). Incluye workflow de GitHub Actions que compila cada push.

## Arquitectura (para desarrolladores)

Bootstrap con inyección por constructor (sin singletons), managers con ciclo de vida ordenado, eventos como plugins internos (`GameEvent` + `EventSession` con template method: 4 hooks bastan para un evento nuevo), menús sobre `InventoryHolder` que ejecutan los comandos reales, persistencia detrás de interfaces y configuración tipada. Java 8 target.

## Licencia

Producto comercial. Todos los derechos reservados. Prohibida su redistribución sin autorización.
