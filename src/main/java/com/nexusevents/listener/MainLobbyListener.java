package com.nexusevents.listener;

import com.nexusevents.lobby.LobbyProtectionSettings;
import com.nexusevents.lobby.MainLobbyService;
import com.nexusevents.permission.Permissions;
import org.bukkit.entity.Player;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.Location;
import org.bukkit.event.EventPriority;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Protecciones de la region del lobby global. Solo actuan dentro de la
 * region configurada, nunca sobre participantes de eventos (cada
 * evento gobierna a los suyos), y las acciones de construccion e
 * interaccion respetan el permiso de bypass para administradores.
 */
@SuppressWarnings("deprecation")
public final class MainLobbyListener implements Listener {

    private final MainLobbyService lobby;

    public MainLobbyListener(MainLobbyService lobby) {
        this.lobby = lobby;
    }

    private LobbyProtectionSettings protection() {
        return lobby.getProtection();
    }

    // ------------------------------------------------------------------
    // Vacio y respawn del mundo del lobby global
    // ------------------------------------------------------------------

    /**
     * En el MUNDO del lobby global, el vacio nunca mata a quien no
     * este en un evento: el dano se cancela y el jugador vuelve al
     * punto del lobby de inmediato. Funciona aunque la altura minima
     * no este configurada (esa es el atajo temprano; esto, la
     * garantia).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVoidFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID
                || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (lobby.isParticipant(player.getUniqueId())) {
            return;
        }
        Location destination = lobby.getLobby().orElse(null);
        if (destination == null || destination.getWorld() == null
                || !destination.getWorld().equals(player.getWorld())) {
            return;
        }
        event.setCancelled(true);
        player.setFallDistance(0.0F);
        player.teleport(destination);
        lobby.sendSafetyMessage(player);
    }

    /**
     * Si igualmente alguien muere en el mundo del lobby global (por
     * ejemplo, un /kill u otro plugin), reaparece en el punto del
     * lobby y no en el spawn vanilla.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (lobby.isParticipant(event.getPlayer().getUniqueId())) {
            return;
        }
        Location destination = lobby.getLobby().orElse(null);
        if (destination != null && destination.getWorld() != null
                && destination.getWorld().equals(event.getPlayer().getWorld())) {
            event.setRespawnLocation(destination);
        }
    }

    // ------------------------------------------------------------------
    // Vida, hambre y PvP
    // ------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (!lobby.shouldProtect(victim)) {
            return;
        }
        if (protection().isNoDamage()) {
            event.setCancelled(true);
            return;
        }
        if (protection().isNoPvp() && event instanceof EntityDamageByEntityEvent
                && isPlayerAttack((EntityDamageByEntityEvent) event)) {
            event.setCancelled(true);
        }
    }

    private boolean isPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return true;
        }
        if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            ProjectileSource shooter = ((org.bukkit.entity.Projectile) event.getDamager()).getShooter();
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
        if (lobby.shouldProtect(player) && protection().isNoHunger()
                && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------
    // Mobs, explosiones y fuego
    // ------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!protection().isNoMobSpawn() || !lobby.isInsideRegion(event.getLocation())) {
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
        if (protection().isNoExplosions()) {
            event.blockList().removeIf(block -> lobby.isInsideRegion(block.getLocation()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (protection().isNoFire() && lobby.isInsideRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (protection().isNoFire() && lobby.isInsideRegion(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------
    // Bloques, items e interaccion (con bypass para admins)
    // ------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (protection().isNoBlockBreak()
                && lobby.isInsideRegion(event.getBlock().getLocation())
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (protection().isNoBlockPlace()
                && lobby.isInsideRegion(event.getBlock().getLocation())
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (protection().isNoItemDrop()
                && lobby.shouldProtect(event.getPlayer())
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (protection().isNoItemPickup()
                && lobby.shouldProtect(event.getPlayer())
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!protection().isNoInteract() || event.getClickedBlock() == null) {
            return;
        }
        if (lobby.isInsideRegion(event.getClickedBlock().getLocation())
                && !event.getPlayer().hasPermission(Permissions.MAINLOBBY_BYPASS)) {
            event.setCancelled(true);
        }
    }

}
