package com.nexusevents.util;

import java.util.Locale;

/**
 * Utilidades de tiempo del plugin.
 *
 * <p>Permite configurar duraciones en YAML de forma legible
 * ({@code "20t"}, {@code "10s"}, {@code "5m"}, {@code "1h"} o un numero
 * plano interpretado como segundos) y convertirlas a ticks del servidor
 * (20 ticks = 1 segundo).</p>
 */
public final class TimeUtil {

    public static final long TICKS_PER_SECOND = 20L;

    private TimeUtil() {
        throw new UnsupportedOperationException("Clase de utilidad: no debe instanciarse.");
    }

    /**
     * Convierte una duracion legible a ticks.
     *
     * <p>Sufijos soportados: {@code t} (ticks), {@code s} (segundos),
     * {@code m} (minutos), {@code h} (horas). Sin sufijo se interpreta
     * como segundos.</p>
     *
     * @param input         duracion configurada.
     * @param fallbackTicks valor a devolver si la entrada es invalida.
     * @return duracion en ticks.
     */
    public static long parseTicks(String input, long fallbackTicks) {
        if (input == null || input.trim().isEmpty()) {
            return fallbackTicks;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        char suffix = value.charAt(value.length() - 1);
        try {
            if (Character.isDigit(suffix)) {
                return Long.parseLong(value) * TICKS_PER_SECOND;
            }
            long amount = Long.parseLong(value.substring(0, value.length() - 1).trim());
            switch (suffix) {
                case 't': return amount;
                case 's': return amount * TICKS_PER_SECOND;
                case 'm': return amount * TICKS_PER_SECOND * 60L;
                case 'h': return amount * TICKS_PER_SECOND * 3600L;
                default: return fallbackTicks;
            }
        } catch (NumberFormatException exception) {
            return fallbackTicks;
        }
    }

    /**
     * Convierte una duracion legible directamente a segundos.
     *
     * @param input           duracion configurada (mismos sufijos que
     *                        {@link #parseTicks(String, long)}).
     * @param fallbackSeconds valor en segundos si la entrada es invalida.
     * @return duracion en segundos (nunca negativa).
     */
    public static int parseSeconds(String input, int fallbackSeconds) {
        long ticks = parseTicks(input, fallbackSeconds * TICKS_PER_SECOND);
        return Math.max(0, ticksToSeconds(ticks));
    }

    /**
     * Convierte segundos a ticks.
     *
     * @param seconds cantidad de segundos.
     * @return ticks equivalentes.
     */
    public static long secondsToTicks(double seconds) {
        return Math.round(seconds * TICKS_PER_SECOND);
    }

    /**
     * Convierte ticks a segundos enteros (redondeo hacia abajo).
     *
     * @param ticks cantidad de ticks.
     * @return segundos equivalentes.
     */
    public static int ticksToSeconds(long ticks) {
        return (int) (ticks / TICKS_PER_SECOND);
    }

    /**
     * Formatea una cantidad de segundos como {@code mm:ss}
     * (o {@code hh:mm:ss} si supera la hora), para contadores visibles.
     *
     * @param totalSeconds segundos totales.
     * @return texto formateado.
     */
    public static String formatSeconds(int totalSeconds) {
        int safe = Math.max(0, totalSeconds);
        int hours = safe / 3600;
        int minutes = (safe % 3600) / 60;
        int seconds = safe % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }
}
