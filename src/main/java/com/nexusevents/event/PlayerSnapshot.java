package com.nexusevents.event;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Fotografia inmutable del estado de un jugador antes de entrar a un
 * evento: inventario, armadura, modo de juego, vida, hambre, experiencia,
 * vuelo, efectos y posicion.
 *
 * <p>Se captura al unirse y se restaura al salir o al finalizar el
 * evento, garantizando que participar nunca tenga costo para el jugador.
 * Usa exclusivamente API disponible desde 1.9.</p>
 */
public final class PlayerSnapshot {

    private final Location location;
    private final GameMode gameMode;
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final int level;
    private final float exp;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final boolean allowFlight;
    private final boolean flying;
    private final boolean collidable;
    private final List<PotionEffect> effects;

    private PlayerSnapshot(Player player) {
        this.location = player.getLocation().clone();
        this.gameMode = player.getGameMode();
        this.inventory = cloneItems(player.getInventory().getContents());
        this.armor = cloneItems(player.getInventory().getArmorContents());
        this.level = player.getLevel();
        this.exp = player.getExp();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.collidable = player.isCollidable();
        this.effects = new ArrayList<>(player.getActivePotionEffects());
    }

    /**
     * Captura el estado actual del jugador.
     *
     * @param player jugador a fotografiar.
     * @return snapshot inmutable.
     */
    public static PlayerSnapshot capture(Player player) {
        return new PlayerSnapshot(player);
    }

    /**
     * Restaura sobre el jugador exactamente el estado capturado.
     *
     * @param player jugador (debe estar online).
     */
    public void restore(Player player) {
        player.setGameMode(gameMode);
        player.getInventory().setContents(cloneItems(inventory));
        player.getInventory().setArmorContents(cloneItems(armor));
        player.setLevel(level);
        player.setExp(exp);
        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setFireTicks(0);

        clearEffects(player);
        player.addPotionEffects(effects);

        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);
        player.setCollidable(collidable);
        player.teleport(location);
    }

    private static void clearEffects(Player player) {
        for (PotionEffect active : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(active.getType());
        }
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] != null ? source[i].clone() : null;
        }
        return copy;
    }
}
