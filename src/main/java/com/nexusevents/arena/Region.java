package com.nexusevents.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.function.Consumer;

/**
 * Region cubica inmutable de una arena.
 *
 * <p>Se define por dos esquinas (normalizadas internamente a minimo y
 * maximo) dentro de un mismo mundo, referenciado por nombre para no
 * perder datos si el mundo no esta cargado. Es la base de la plataforma
 * de Pixel Party, la zona de Parkour y la restauracion de bloques.</p>
 */
public final class Region {

    private static final String SEPARATOR = ";";

    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    private Region(String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    /**
     * Crea una region a partir de dos esquinas marcadas por un admin.
     *
     * @param first  primera esquina.
     * @param second segunda esquina.
     * @return region normalizada.
     * @throws IllegalArgumentException si las esquinas estan en mundos distintos.
     */
    public static Region of(ArenaLocation first, ArenaLocation second) {
        if (!first.getWorldName().equalsIgnoreCase(second.getWorldName())) {
            throw new IllegalArgumentException("Las dos esquinas pertenecen a mundos distintos.");
        }
        return new Region(
                first.getWorldName(),
                (int) Math.floor(first.getX()), (int) Math.floor(first.getY()), (int) Math.floor(first.getZ()),
                (int) Math.floor(second.getX()), (int) Math.floor(second.getY()), (int) Math.floor(second.getZ())
        );
    }

    /**
     * Reconstruye una region desde su forma serializada
     * ({@code mundo;x1;y1;z1;x2;y2;z2}).
     *
     * @param input cadena serializada.
     * @return region reconstruida.
     * @throws IllegalArgumentException si el formato es invalido.
     */
    public static Region deserialize(String input) {
        String[] parts = input.split(SEPARATOR);
        if (parts.length != 7) {
            throw new IllegalArgumentException("Formato de region invalido: " + input);
        }
        return new Region(
                parts[0],
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6])
        );
    }

    /**
     * Serializa la region como {@code mundo;x1;y1;z1;x2;y2;z2}.
     *
     * @return cadena serializada.
     */
    public String serialize() {
        return worldName + SEPARATOR
                + minX + SEPARATOR + minY + SEPARATOR + minZ + SEPARATOR
                + maxX + SEPARATOR + maxY + SEPARATOR + maxZ;
    }

    /**
     * Indica si la posicion dada esta dentro de la region.
     *
     * @param location posicion a evaluar.
     * @return true si esta dentro (incluyendo los bordes).
     */
    public boolean contains(Location location) {
        World world = location.getWorld();
        if (world == null || !world.getName().equalsIgnoreCase(worldName)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    /**
     * Devuelve el mundo de la region, si esta cargado.
     *
     * @return mundo de Bukkit o null si no esta cargado.
     */
    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    /**
     * Ejecuta una accion sobre cada bloque de la region, si su mundo
     * esta cargado. Recorre en orden x, y, z.
     *
     * @param action accion a ejecutar por bloque.
     */
    public void forEachBlock(Consumer<Block> action) {
        World world = getWorld();
        if (world == null) {
            return;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    action.accept(world.getBlockAt(x, y, z));
                }
            }
        }
    }

    /**
     * Cantidad total de bloques de la region.
     *
     * @return volumen en bloques.
     */
    public int getVolume() {
        return getWidthX() * getHeight() * getWidthZ();
    }

    /**
     * Descripcion corta para mensajes de administracion.
     *
     * @return texto del estilo {@code 21x1x21 en world}.
     */
    public String describe() {
        return getWidthX() + "x" + getHeight() + "x" + getWidthZ() + " en " + worldName;
    }

    public int getWidthX() {
        return maxX - minX + 1;
    }

    public int getHeight() {
        return maxY - minY + 1;
    }

    public int getWidthZ() {
        return maxZ - minZ + 1;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }
}
