package com.nexusevents.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

/**
 * Posicion inmutable y serializable de una arena.
 *
 * <p>A diferencia de {@link Location}, conserva el nombre del mundo como
 * dato: si el mundo no esta cargado al momento de leer la arena, la
 * informacion no se pierde y puede volver a guardarse intacta. La
 * conversion a {@link Location} se hace recien al momento de usarla.</p>
 */
public final class ArenaLocation {

    private static final String SEPARATOR = ";";

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    private ArenaLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Crea una posicion a partir de un {@link Location} de Bukkit.
     *
     * @param location posicion de origen; su mundo no puede ser null.
     * @return posicion inmutable equivalente.
     */
    public static ArenaLocation from(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("La posicion no tiene mundo asociado.");
        }
        return new ArenaLocation(world.getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch());
    }

    /**
     * Reconstruye una posicion desde su forma serializada
     * ({@code mundo;x;y;z;yaw;pitch}).
     *
     * @param input cadena serializada.
     * @return posicion reconstruida.
     * @throws IllegalArgumentException si el formato es invalido.
     */
    public static ArenaLocation deserialize(String input) {
        String[] parts = input.split(SEPARATOR);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Formato de posicion invalido: " + input);
        }
        return new ArenaLocation(
                parts[0],
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }

    /**
     * Serializa la posicion como {@code mundo;x;y;z;yaw;pitch}.
     *
     * @return cadena serializada.
     */
    public String serialize() {
        return worldName + SEPARATOR
                + format(x) + SEPARATOR
                + format(y) + SEPARATOR
                + format(z) + SEPARATOR
                + format(yaw) + SEPARATOR
                + format(pitch);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    /**
     * Convierte a {@link Location} de Bukkit, si el mundo esta cargado.
     *
     * @return posicion de Bukkit o null si el mundo no esta cargado.
     */
    public Location toBukkit() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Descripcion corta para mensajes de administracion.
     *
     * @return texto del estilo {@code 10.5, 64.0, -3.2 (world)}.
     */
    public String describe() {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f (%s)", x, y, z, worldName);
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
