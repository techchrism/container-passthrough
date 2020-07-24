package com.darkender.plugins.itemframecontainerpassthrough;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Container;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

public class ItemframeContainerPassthrough extends JavaPlugin implements Listener
{
    private int openCount = 0;
    
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if(event.getRightClicked().getType() != EntityType.ITEM_FRAME || event.getHand() == EquipmentSlot.OFF_HAND)
        {
            return;
        }
    
        RayTraceResult result = event.getPlayer().rayTraceBlocks(5.0, FluidCollisionMode.NEVER);
        if(result == null || result.getHitBlock() == null || !(result.getHitBlock().getState() instanceof Container))
        {
            return;
        }
        Container container = (Container) result.getHitBlock().getState();
        event.setCancelled(true);
        event.getPlayer().openInventory(container.getInventory());
        Bukkit.broadcastMessage("Opening container! (" + (++openCount) + ")");
    }
}
