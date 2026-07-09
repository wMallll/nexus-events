package com.nexusevents.event.circle;

import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Listener con alcance de sesion para "El Circulo".
 *
 * <p>Con el PvP activo, garantiza que ningun participante muera de
 * verdad: cuando un golpe seria letal, cancela el dano y elimina al
 * jugador limpiamente (sin drops ni pantalla de respawn).</p>
 */
final class CircleListener implements Listener {

    private final CircleSession session;

    CircleListener(CircleSession session) {
        this.session = session;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (!session.isAlive(victim.getUniqueId())) {
            return;
        }
        if (victim.getHealth() - event.getFinalDamage() <= 0.5) {
            event.setCancelled(true);
            session.eliminate(victim);
        }
    }

    /**
     * Garantiza el PvP del evento entre participantes vivos: des-cancela
     * el golpe (mundos con pvp bloqueado por otros plugins) y, si fue
     * una bola de nieve, aplica el empuje manual configurado. No
     * interviene si el guardia de golpes letales ya elimino a la
     * victima (corre antes, en prioridad HIGH).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onParticipantHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (!session.isAlive(victim.getUniqueId())) {
            return;
        }
        Player attacker = null;
        Snowball snowball = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Snowball) {
            snowball = (Snowball) event.getDamager();
            if (snowball.getShooter() instanceof Player) {
                attacker = (Player) snowball.getShooter();
            }
        }
        if (attacker == null || !session.isAlive(attacker.getUniqueId())) {
            return;
        }
        event.setCancelled(false);
        if (snowball != null) {
            session.applySnowballPush(victim, snowball.getVelocity());
        }
    }
}
