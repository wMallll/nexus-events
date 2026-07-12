package com.nexusevents.listener;

import com.nexusevents.lobby.LobbyProtectionSettings;
import com.nexusevents.lobby.LobbyZoneService;
import com.nexusevents.permission.Permissions;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Protecciones de las zonas de lobby por arena: identicas a las del
 * lobby global, resueltas segun la zona que contiene cada posicion.
 * Los participantes de eventos quedan exentos de las protecciones de
 * jugador, y el permiso de bypass exime de las de construccion e
 * interaccion.
 */
@SuppressWarnings("deprecation")
public final class LobbyZoneListener implements Listener {

    private final LobbyZoneService zones;

    public LobbyZoneListener(LobbyZoneService zones) {
        this.zones = zones;
    }

    private LobbyProtectionSettings settingsForPlayer(Player player) {
        if (zones.isParticipant(player.getUniqueId())) {
            return null;
        }
        return zones.zoneAt(player.getLocation())
                .map(LobbyZoneService.LobbyZone::getSettings).orElse(null);
    }

    private LobbyProtectionSettings settingsAt(Location location) {
        return zones.zoneAt(location)
                .map(LobbyZoneService.LobbyZone::getSettings).orElse(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        LobbyProtectionSettings settings = settingsForPlayer((Player) event.getEntity());
        if (settings == null) {
            return;
        }
        if (settings.isNoDamage()) {
            event.setCancelled(true);
            return;
        }
        if (settings.isNoPvp() && event instanceof EntityDamageByEntityEvent
                && isPlayerAttack((EntityDamageByEntityEvent) event)) {
            event.setCancelled(true);
        }
    }

    private boolean isPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return true;
        }
        if (event.getDamager() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            return shooter instanceof Player;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        LobbyProtectionSettings settings = settingsForPlayer(player);
        if (settings != null && settings.isNoHunger()
                && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LobbyProtectionSettings settings = settingsAt(event.getLocation());
        if (settings == null || !settings.isNoMobSpawn()) {
            return;
        }
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.CUSTOM
                && reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            LobbyProtectionSettings settings = settingsAt(block.getLocation());
            return settings != null && settings.isNoExplosions();
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        LobbyProtectionSettings settings = settingsAt(event.getBlock().getLocation());
        if (settings != null && settings.isNoFire()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        LobbyProtectionSettings settings = settingsAt(event.getBlock().getLocation());
        if (settings != null && settings.isNoFire()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        LobbyProtectionSettings settings = settingsAt(event.getBlock().getLocation());
        if (settings != null && settings.isNoBlockBreak()
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        LobbyProtectionSettings settings = settingsAt(event.getBlock().getLocation());
        if (settings != null && settings.isNoBlockPlace()
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        LobbyProtectionSettings settings = settingsForPlayer(event.getPlayer());
        if (settings != null && settings.isNoItemDrop()
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        LobbyProtectionSettings settings = settingsForPlayer(event.getPlayer());
        if (settings != null && settings.isNoItemPickup()
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        LobbyProtectionSettings settings = settingsAt(event.getClickedBlock().getLocation());
        if (settings != null && settings.isNoInteract()
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }
}
