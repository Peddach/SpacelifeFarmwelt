package de.petropia.farmworld.listener;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PreventPVPListener implements Listener {

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event){
        if(event.getDamager().getType() == EntityType.PLAYER && event.getEntity().getType() == EntityType.PLAYER){
            event.setCancelled(true);
        }
    }

}
