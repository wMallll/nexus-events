# NexusEvents

Plugin profesional de **eventos multi-arena** para servidores de Minecraft, compatible desde **1.9 hasta la última versión** con un único jar y sin NMS.

Incluye cuatro eventos completos, sistema de arenas con setup 100% desde el juego, scoreboards en vivo, bossbars, reconexión inteligente y configuración total por YAML.

---

## Eventos incluidos

| Evento | Id | Descripción |
|---|---|---|
| **Escondete si puedes** | `hide-and-seek` | Todos se esconden durante una cuenta regresiva; el cazador sale con su arma configurable y elimina de un golpe. Eliminados quedan invisibles y con vuelo. Incluye reconexión con ventana configurable para desconexiones involuntarias. |
| **Pixel Party** | `pixel-party` | Plataforma multicolor por mosaicos. Cada ronda se anuncia un color por título; los bloques incorrectos desaparecen y quien cae queda eliminado. Rondas cada vez más rápidas. Termina por tiempo: todos los que sigan en pie ganan. |
| **Parkour Colapsable** | `parkour` | El recorrido desaparece desde el inicio siguiendo su forma real, en lotes pequeños por tick (optimizado contra lag) y con velocidad creciente. Último en pie gana. |
| **El Círculo** | `circle` | Un círculo de partículas se cierra de forma continua; quien queda fuera recibe daño configurable (sin muertes reales). Último vivo gana. |

## Características

- **Compatibilidad 1.9 → última versión** con un solo jar (XSeries, FastBoard, títulos y bossbars por API nativa).
- **Setup completo desde el juego**: nada de editar coordenadas a mano. Guardado automático.
- **Múltiples arenas independientes**, con eventos simultáneos en arenas distintas.
- **Todo configurable**: mensajes (legacy + HEX + MiniMessage mezclables), sonidos, títulos, actionbars, scoreboards, bossbar, tiempos, permisos, paletas.
- **Seguridad del jugador**: snapshot completo al entrar (inventario, XP, efectos, posición, gamemode) y restauración garantizada al salir — incluso tras desconexiones, en el próximo login.
- **Fusión automática de configuración**: al actualizar el plugin, las claves nuevas se agregan solas a tus YAML sin pisar tus ediciones.
- **Lobby global de primer ingreso** y lobby/spawn opcionales **por evento** en cada arena.
- Integración opcional con **PlaceholderAPI** y softdepends de LuckPerms, Vault, WorldEdit y ProtocolLib.

## Instalación

1. Colocar `NexusEvents-x.x.x.jar` en la carpeta `plugins/`.
2. Reiniciar el servidor. Se genera `plugins/NexusEvents/` con toda la configuración.
3. Configurar arenas desde el juego (ver más abajo) y jugar.

## Comandos

Comando raíz: `/evento` (aliases: `/eventos`, `/nexusevents`).

| Comando | Descripción | Permiso |
|---|---|---|
| `/evento help` | Ayuda dinámica según tus permisos | `evento.use` |
| `/evento version` | Versión del plugin y del servidor | `evento.version` |
| `/evento reload` | Recarga toda la configuración en caliente | `evento.reload` |
| `/evento createarena (nombre)` | Crea una arena y la selecciona | `evento.arena.create` |
| `/evento deletearena (nombre) confirm` | Elimina una arena definitivamente | `evento.arena.delete` |
| `/evento arenas` | Lista las arenas con su estado de setup | `evento.arena.list` |
| `/evento select (nombre)` | Selecciona la arena a editar | `evento.arena.edit` |
| `/evento arena [nombre]` | Muestra la configuración de una arena | `evento.arena.edit` |
| `/evento setspawn [evento]` | Spawn general o específico de un evento | `evento.setup.setspawn` |
| `/evento setlobby [evento]` | Lobby de espera general o por evento | `evento.setup.setlobby` |
| `/evento sethunterspawn` | Spawn del cazador | `evento.setup.sethunterspawn` |
| `/evento setcircle` | Centro del círculo | `evento.setup.setcircle` |
| `/evento setpixelparty (1\|2)` | Esquinas de la plataforma | `evento.setup.setpixelparty` |
| `/evento setparkour (1\|2)` | Esquinas del recorrido | `evento.setup.setparkour` |
| `/evento setmainlobby` | Lobby global de primer ingreso | `evento.setup.setmainlobby` |
| `/evento save` | Guarda todas las arenas | `evento.setup.save` |
| `/evento start (evento) (arena)` | Inicia un evento | `evento.start` |
| `/evento stop [arena]` | Detiene un evento | `evento.stop` |
| `/evento join [arena]` | Entrar a un evento (default: todos) | `evento.join` |
| `/evento leave` | Salir del evento (default: todos) | `evento.leave` |
| `/evento events` | Eventos disponibles y activos | `evento.events` |

`evento.admin` otorga todos los permisos (compatible con LuckPerms).

## Setup rápido por evento

Toda arena necesita `setlobby` (espera) y `setspawn` (inicio). Además:

- **hide-and-seek**: `sethunterspawn` donde espera el cazador.
- **pixel-party**: `setpixelparty 1` y `2` marcando la plataforma **al nivel de sus bloques** (parate dentro de un hueco en la esquina, no encima). Plataforma plana de 1 bloque, spawn arriba.
- **parkour**: `setparkour 1` y `2` en una caja que contenga todo el recorrido (el aire sobrante no molesta). El **spawn marca el inicio del colapso**.
- **circle**: `setcircle` en el centro; spawn dentro del radio inicial.

Verificá todo con `/evento arena` y arrancá con `/evento start (evento) (arena)`.

## Archivos de configuración

| Archivo | Contenido |
|---|---|
| `config.yml` | Ajustes globales de eventos (jugadores, tiempos, countdown, bossbar) |
| `messages.yml` | Todos los mensajes (legacy + HEX + MiniMessage, multilínea) |
| `sounds.yml` | Sonidos por clave lógica, multi-versión |
| `scoreboards.yml` | Plantillas de scoreboard por evento |
| `events/*.yml` | Configuración específica de cada evento |
| `lobby.yml` | Lobby global de primer ingreso |
| `arenas/*.yml` | Una arena por archivo (generado automáticamente) |

Las duraciones aceptan sufijos: `t` (ticks), `s`, `m`, `h`.

## Placeholders (PlaceholderAPI)

`%nexusevents_active%`, `%nexusevents_event%`, `%nexusevents_arena%`, `%nexusevents_state%`, `%nexusevents_alive%`, `%nexusevents_spectators%`, `%nexusevents_time%`.

## Compilar desde el código

Requisitos: JDK 8–21 y Maven.

```bash
mvn clean package
```

El jar final queda en `target/NexusEvents-x.x.x.jar` (con las librerías empaquetadas y relocadas). El repositorio incluye un workflow de GitHub Actions que compila automáticamente cada push.

## Arquitectura (para desarrolladores)

Bootstrap con inyección por constructor (sin singletons), managers con ciclo de vida ordenado (`Manager`/`Reloadable`), eventos como plugins internos (`GameEvent` define el tipo, `EventSession` la partida con template method: implementar 4 hooks alcanza para un evento nuevo), persistencia detrás de interfaces y configuración tipada por modelos. Java 8 target para soportar servidores 1.9.

## Licencia

Producto comercial. Todos los derechos reservados. Prohibida su redistribución sin autorización.
