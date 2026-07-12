package com.nexusevents.lobby;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Protecciones de la region del lobby global, persistidas en
 * {@code lobby.yml} bajo {@code protection}. Cada una es toggleable en
 * vivo por comando o menu.
 */
public final class LobbyProtectionSettings {

    /** Claves aceptadas por {@code /evento mainlobby set}. */
    public static final List<String> SETTING_KEYS = Arrays.asList(
            "no-damage", "no-hunger", "no-pvp", "no-mob-spawn", "no-explosions",
            "no-block-break", "no-block-place", "no-fire", "no-item-drop",
            "no-item-pickup", "no-interact");

    private boolean noDamage = true;
    private boolean noHunger = true;
    private boolean noPvp = true;
    private boolean noMobSpawn = true;
    private boolean noExplosions = true;
    private boolean noBlockBreak = true;
    private boolean noBlockPlace = true;
    private boolean noFire = true;
    private boolean noItemDrop = true;
    private boolean noItemPickup = false;
    private boolean noInteract = false;

    /**
     * Carga las protecciones desde la seccion {@code protection} (los
     * ausentes conservan su valor por defecto).
     *
     * @param section seccion protection o null.
     */
    public void load(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        noDamage = section.getBoolean("no-damage", noDamage);
        noHunger = section.getBoolean("no-hunger", noHunger);
        noPvp = section.getBoolean("no-pvp", noPvp);
        noMobSpawn = section.getBoolean("no-mob-spawn", noMobSpawn);
        noExplosions = section.getBoolean("no-explosions", noExplosions);
        noBlockBreak = section.getBoolean("no-block-break", noBlockBreak);
        noBlockPlace = section.getBoolean("no-block-place", noBlockPlace);
        noFire = section.getBoolean("no-fire", noFire);
        noItemDrop = section.getBoolean("no-item-drop", noItemDrop);
        noItemPickup = section.getBoolean("no-item-pickup", noItemPickup);
        noInteract = section.getBoolean("no-interact", noInteract);
    }

    /**
     * Escribe todas las protecciones bajo {@code protection.} en la
     * configuracion dada.
     *
     * @param root configuracion de lobby.yml.
     */
    public void save(ConfigurationSection root) {
        root.set("protection.no-damage", noDamage);
        root.set("protection.no-hunger", noHunger);
        root.set("protection.no-pvp", noPvp);
        root.set("protection.no-mob-spawn", noMobSpawn);
        root.set("protection.no-explosions", noExplosions);
        root.set("protection.no-block-break", noBlockBreak);
        root.set("protection.no-block-place", noBlockPlace);
        root.set("protection.no-fire", noFire);
        root.set("protection.no-item-drop", noItemDrop);
        root.set("protection.no-item-pickup", noItemPickup);
        root.set("protection.no-interact", noInteract);
    }

    /**
     * Actualiza una proteccion desde texto.
     *
     * @param key   proteccion (ver SETTING_KEYS).
     * @param value "true" o "false".
     * @return true si la clave y el valor son validos.
     */
    public boolean applySetting(String key, String value) {
        String lowered = value.trim().toLowerCase(Locale.ROOT);
        if (!lowered.equals("true") && !lowered.equals("false")) {
            return false;
        }
        boolean enabled = Boolean.parseBoolean(lowered);
        switch (key.toLowerCase(Locale.ROOT)) {
            case "no-damage":
                noDamage = enabled;
                return true;
            case "no-hunger":
                noHunger = enabled;
                return true;
            case "no-pvp":
                noPvp = enabled;
                return true;
            case "no-mob-spawn":
                noMobSpawn = enabled;
                return true;
            case "no-explosions":
                noExplosions = enabled;
                return true;
            case "no-block-break":
                noBlockBreak = enabled;
                return true;
            case "no-block-place":
                noBlockPlace = enabled;
                return true;
            case "no-fire":
                noFire = enabled;
                return true;
            case "no-item-drop":
                noItemDrop = enabled;
                return true;
            case "no-item-pickup":
                noItemPickup = enabled;
                return true;
            case "no-interact":
                noInteract = enabled;
                return true;
            default:
                return false;
        }
    }

    /**
     * Estado de una proteccion por clave (para el menu y el info).
     *
     * @param key proteccion.
     * @return true si esta activada.
     */
    public boolean isEnabled(String key) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "no-damage":
                return noDamage;
            case "no-hunger":
                return noHunger;
            case "no-pvp":
                return noPvp;
            case "no-mob-spawn":
                return noMobSpawn;
            case "no-explosions":
                return noExplosions;
            case "no-block-break":
                return noBlockBreak;
            case "no-block-place":
                return noBlockPlace;
            case "no-fire":
                return noFire;
            case "no-item-drop":
                return noItemDrop;
            case "no-item-pickup":
                return noItemPickup;
            case "no-interact":
                return noInteract;
            default:
                return false;
        }
    }

    public boolean isNoDamage() {
        return noDamage;
    }

    public boolean isNoHunger() {
        return noHunger;
    }

    public boolean isNoPvp() {
        return noPvp;
    }

    public boolean isNoMobSpawn() {
        return noMobSpawn;
    }

    public boolean isNoExplosions() {
        return noExplosions;
    }

    public boolean isNoBlockBreak() {
        return noBlockBreak;
    }

    public boolean isNoBlockPlace() {
        return noBlockPlace;
    }

    public boolean isNoFire() {
        return noFire;
    }

    public boolean isNoItemDrop() {
        return noItemDrop;
    }

    public boolean isNoItemPickup() {
        return noItemPickup;
    }

    public boolean isNoInteract() {
        return noInteract;
    }

}
