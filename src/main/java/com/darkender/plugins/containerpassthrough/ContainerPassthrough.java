package com.darkender.plugins.containerpassthrough;

import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Container;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashSet;
import java.util.Set;

public class ContainerPassthrough extends JavaPlugin implements Listener
{
    private Set<EntityType> passthroughEntities;
    
    @Override
    public void onEnable()
    {
        passthroughEntities = new HashSet<>();
        passthroughEntities.add(EntityType.PAINTING);
        passthroughEntities.add(EntityType.ITEM_FRAME);
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    private boolean tryOpeningContainer(Player player)
    {
        RayTraceResult result = player.rayTraceBlocks(5.0, FluidCollisionMode.NEVER);
        if(result == null || result.getHitBlock() == null || !(result.getHitBlock().getState() instanceof Container))
        {
            return false;
        }
        Container container = (Container) result.getHitBlock().getState();
        
        // Send out an event to ensure container locking plugins aren't bypassed
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(), result.getHitBlock(), result.getHitBlockFace(), EquipmentSlot.HAND);
        getServer().getPluginManager().callEvent(interactEvent);
        if(interactEvent.useInteractedBlock() == Event.Result.DENY || interactEvent.isCancelled())
        {
            return false;
        }
        
        if(!player.getOpenInventory().getTopInventory().equals(container.getInventory()))
        {
            player.openInventory(container.getInventory());
        }
        return true;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if(event.getPlayer().isSneaking() || !passthroughEntities.contains(event.getRightClicked().getType()))
        {
            return;
        }
        
        if(tryOpeningContainer(event.getPlayer()))
        {
            event.setCancelled(true);
        }
    }
}
