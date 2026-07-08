package com.nexusevents.arena;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Arena de eventos, completamente independiente de las demas.
 *
 * <p>Almacena posiciones y regiones por clave (ver {@link ArenaKeys}) en
 * lugar de campos fijos por evento: agregar un evento nuevo con puntos
 * propios no requiere modificar esta clase (principio abierto/cerrado).</p>
 */
public final class Arena {

    private final String name;
    private final Map<String, ArenaLocation> points = new LinkedHashMap<>();
    private final Map<String, Region> regions = new LinkedHashMap<>();

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Define o reemplaza un punto de la arena.
     *
     * @param key      clave del punto.
     * @param location posicion.
     */
    public void setPoint(String key, ArenaLocation location) {
        points.put(key, location);
    }

    /**
     * Obtiene un punto configurado.
     *
     * @param key clave del punto.
     * @return posicion, si esta configurada.
     */
    public Optional<ArenaLocation> getPoint(String key) {
        return Optional.ofNullable(points.get(key));
    }

    public boolean hasPoint(String key) {
        return points.containsKey(key);
    }

    /**
     * Define o reemplaza una region de la arena.
     *
     * @param key    clave de la region.
     * @param region region.
     */
    public void setRegion(String key, Region region) {
        regions.put(key, region);
    }

    /**
     * Obtiene una region configurada.
     *
     * @param key clave de la region.
     * @return region, si esta configurada.
     */
    public Optional<Region> getRegion(String key) {
        return Optional.ofNullable(regions.get(key));
    }

    public boolean hasRegion(String key) {
        return regions.containsKey(key);
    }

    /**
     * Vista de solo lectura de todos los puntos configurados.
     *
     * @return puntos por clave.
     */
    public Map<String, ArenaLocation> getPoints() {
        return Collections.unmodifiableMap(points);
    }

    /**
     * Vista de solo lectura de todas las regiones configuradas.
     *
     * @return regiones por clave.
     */
    public Map<String, Region> getRegions() {
        return Collections.unmodifiableMap(regions);
    }
}
