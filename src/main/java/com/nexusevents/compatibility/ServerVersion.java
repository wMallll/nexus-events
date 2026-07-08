package com.nexusevents.compatibility;

import org.bukkit.Bukkit;

/**
 * Representa la version del servidor en ejecucion.
 *
 * <p>Se detecta parseando {@code Bukkit.getBukkitVersion()} (por ejemplo
 * {@code "1.20.6-R0.1-SNAPSHOT"}), lo que evita depender de los paquetes
 * NMS/CraftBukkit y funciona en cualquier fork. Es la base de la capa de
 * compatibilidad: el resto del plugin consulta {@link #isAtLeast(int)}
 * para ramificar comportamiento entre versiones.</p>
 */
public final class ServerVersion {

    private static volatile ServerVersion instance;

    private final int major;
    private final int minor;
    private final int patch;
    private final boolean paper;
    private final String displayName;

    private ServerVersion(int major, int minor, int patch, boolean paper) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.paper = paper;
        this.displayName = major + "." + minor + (patch > 0 ? "." + patch : "");
    }

    /**
     * Obtiene la version detectada del servidor (se calcula una sola vez).
     *
     * @return version del servidor actual.
     */
    public static ServerVersion get() {
        ServerVersion current = instance;
        if (current == null) {
            synchronized (ServerVersion.class) {
                current = instance;
                if (current == null) {
                    current = detect();
                    instance = current;
                }
            }
        }
        return current;
    }

    private static ServerVersion detect() {
        String raw = Bukkit.getBukkitVersion().split("-")[0];
        String[] parts = raw.split("\\.");
        int major = parseNumber(parts, 0, 1);
        int minor = parseNumber(parts, 1, 9);
        int patch = parseNumber(parts, 2, 0);
        return new ServerVersion(major, minor, patch, detectPaper());
    }

    private static int parseNumber(String[] parts, int index, int fallback) {
        if (index >= parts.length) {
            return fallback;
        }
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean detectPaper() {
        String[] markerClasses = {
                "io.papermc.paper.configuration.Configuration",
                "com.destroystokyo.paper.PaperConfig"
        };
        for (String className : markerClasses) {
            try {
                Class.forName(className);
                return true;
            } catch (ClassNotFoundException exception) {
                // La clase no existe en esta plataforma: se prueba la siguiente.
            }
        }
        return false;
    }

    /**
     * Indica si el servidor es al menos la version {@code 1.<minor>}.
     *
     * @param minor version menor minima (por ejemplo 13 para 1.13).
     * @return true si la version del servidor es igual o superior.
     */
    public boolean isAtLeast(int minor) {
        return this.minor >= minor;
    }

    /**
     * Indica si el servidor es al menos la version {@code 1.<minor>.<patch>}.
     *
     * @param minor version menor minima.
     * @param patch parche minimo dentro de esa version menor.
     * @return true si la version del servidor es igual o superior.
     */
    public boolean isAtLeast(int minor, int patch) {
        return this.minor > minor || (this.minor == minor && this.patch >= patch);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public boolean isPaper() {
        return paper;
    }

    public String getDisplayName() {
        return displayName;
    }
}
