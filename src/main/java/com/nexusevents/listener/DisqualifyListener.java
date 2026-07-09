package com.nexusevents.listener;

import com.nexusevents.event.EventManager;
import com.nexusevents.event.EventSession;
import com.nexusevents.message.MessageService;
import com.nexusevents.moderation.DisqualifyStick;
import com.nexusevents.permission.Permissions;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Golpes con el Palo Descalificador: nunca hacen dano y, si el que
 * golpea tiene el permiso y la victima es un participante vivo, la
 * descalifican (pasa a espectador con todo el pipeline estandar).
 * Corre en prioridad LOWEST para resolverse antes que cualquier otra
 * logica de dano de los eventos.
 */
public final class DisqualifyListener implements Listener {

    private final EventManager eventManager;
    private final MessageService messages;

    public DisqualifyListener(EventManager eventManager, MessageService messages) {
        this.eventManager = eventManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStickHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        if (!DisqualifyStick.matches(attacker.getInventory().getItemInMainHand())) {
            return;
        }
        event.setCancelled(true);
        if (!attacker.hasPermission(Permissions.DQ_STICK)) {
            messages.send(attacker, "moderation.stick-no-permission");
            return;
        }
        Player victim = (Player) event.getEntity();
        EventSession session = eventManager.getSessionByPlayer(victim.getUniqueId()).orElse(null);
        if (session == null || !session.isAlive(victim.getUniqueId())) {
            messages.send(attacker, "moderation.stick-not-participant",
                    Placeholder.unparsed("player", victim.getName()));
            return;
        }
        session.eliminate(victim);
        messages.send(attacker, "moderation.disqualified",
                Placeholder.unparsed("player", victim.getName()));
        messages.send(victim, "moderation.disqualified-target");
    }
}
