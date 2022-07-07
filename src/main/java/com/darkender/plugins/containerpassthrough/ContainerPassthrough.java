package com.darkender.plugins.containerpassthrough;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashSet;
import java.util.Set;

public class ContainerPassthrough extends JavaPlugin implements Listener
{
    private Set<EntityType> passthroughEntities;
    private Set<Material> passthroughBlocks;
    private Set<Material> dyeItems;
    private boolean ignoreInteractEvents = false;
    private ReflectionUtils reflectionUtils;
    
    @Override
    public void onEnable()
    {
        passthroughEntities = new HashSet<>();
        passthroughEntities.add(EntityType.PAINTING);
        passthroughEntities.add(EntityType.ITEM_FRAME);
        passthroughEntities.add(EntityType.GLOW_ITEM_FRAME);
        
        passthroughBlocks = new HashSet<>();
        for(Material material : Material.values())
        {
            if(material.name().contains("SIGN"))
            {
                passthroughBlocks.add(material);
            }
        }
        dyeItems = new HashSet<>();
        for(Material material : Material.values())
        {
            if(material.name().contains("DYE"))
            {
                dyeItems.add(material);
            }
        }
        dyeItems.add(Material.INK_SAC);
        dyeItems.add(Material.COCOA_BEANS);
        dyeItems.add(Material.LAPIS_LAZULI);
        dyeItems.add(Material.BONE_MEAL);
        dyeItems.add(Material.GLOW_INK_SAC);

        getServer().getPluginManager().registerEvents(this, this);
        reflectionUtils = new ReflectionUtils();
    }
    
    private boolean canOpenContainer(Player player, Block block, BlockFace face)
    {
        // Send out an event to ensure container locking plugins aren't bypassed
        ignoreInteractEvents = true;
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(), block, face, EquipmentSlot.HAND);
        getServer().getPluginManager().callEvent(interactEvent);
        ignoreInteractEvents = false;
        return !(interactEvent.useInteractedBlock() == Event.Result.DENY || interactEvent.isCancelled());
    }

    private boolean tryOpeningContainerRaytrace(Player player)
    {
        RayTraceResult result = player.rayTraceBlocks(5.0, FluidCollisionMode.NEVER);
        if(result == null || result.getHitBlock() == null || (!(result.getHitBlock().getState() instanceof Container) && !(result.getHitBlock().getState().getType().toString() == Material.CRAFTING_TABLE.toString()) && !(result.getHitBlock().getState().getType().toString() == Material.ENDER_CHEST.toString())))
        {
            return false;
        }
        if(result.getHitBlock().getState().getType().toString() == Material.CRAFTING_TABLE.toString()) {
            player.openWorkbench(result.getHitBlock().getLocation(), true);
            return true;
        } else if(result.getHitBlock().getState().getType().toString() == Material.ENDER_CHEST.toString()) {
            player.openInventory(player.getEnderChest());
            player.getWorld().playSound(result.getHitBlock().getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
            return true;
        }
        Container container = (Container) result.getHitBlock().getState();
        
        if(canOpenContainer(player, result.getHitBlock(), result.getHitBlockFace()))
        {
            tryOpeningContainer(player, container);
        }
        return true;
    }
    
    private void tryOpeningContainer(Player player, Container container)
    {
        if(!player.getOpenInventory().getTopInventory().equals(container.getInventory()))
        {
            player.openInventory(container.getInventory());
            if(container.getInventory() instanceof DoubleChestInventory)
            {
                player.setMetadata("doublechest-open", new FixedMetadataValue(this, true));
            }
        }
    }
    
    private void closeDoubleChest(DoubleChest doubleChest, HumanEntity entity)
    {
        Chest left = (Chest) doubleChest.getLeftSide();
        Chest right = (Chest) doubleChest.getRightSide();
    
        // Fix old spigot bugs...
        Bukkit.getScheduler().runTaskLater(this, () ->
        {
            if(reflectionUtils.isReflectionReady() && reflectionUtils.getViewers(left) != left.getInventory().getViewers().size())
            {
                reflectionUtils.closeContainer(left, entity);
                reflectionUtils.closeContainer(right, entity);
            }
        }, 4L);
        
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if(event.getPlayer().isSneaking() ||
                !passthroughEntities.contains(event.getRightClicked().getType()) ||
                event.getHand() == EquipmentSlot.OFF_HAND)
        {
            return;
        }
        
        if(tryOpeningContainerRaytrace(event.getPlayer()))
        {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlayerInteract(PlayerInteractEvent event)
    {
        if(ignoreInteractEvents || event.getPlayer().isSneaking() ||
                event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                event.getClickedBlock() == null ||
                event.getHand() == EquipmentSlot.OFF_HAND ||
                !passthroughBlocks.contains(event.getClickedBlock().getType()) ||
                dyeItems.contains(event.getPlayer().getInventory().getItemInMainHand().getType()))

        {
            return;
        }
        Block blockBehind = event.getClickedBlock().getRelative(event.getBlockFace().getOppositeFace());
        if(!(blockBehind.getState() instanceof Container) || !canOpenContainer(event.getPlayer(), blockBehind, event.getBlockFace())) {
            if(!(blockBehind.getState().getType().toString() == Material.CRAFTING_TABLE.toString()) && !(blockBehind.getState().getType().toString() == Material.ENDER_CHEST.toString())) {
                return;
            } else {
                if(blockBehind.getState().getType().toString() == Material.ENDER_CHEST.toString()) {
                    event.getPlayer().openInventory(event.getPlayer().getEnderChest());
                    event.getPlayer().getWorld().playSound(event.getClickedBlock().getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
                } else if(blockBehind.getState().getType().toString() == Material.CRAFTING_TABLE.toString()) {
                    event.getPlayer().openWorkbench(blockBehind.getLocation(), true);
                }
                event.setCancelled(true);
                return;
            }
        }
        Container container = (Container) blockBehind.getState();
        tryOpeningContainer(event.getPlayer(), container);
        event.setCancelled(true);
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onInventoryClose(InventoryCloseEvent event)
    {
        HumanEntity p = event.getPlayer();
        if(!p.hasMetadata("doublechest-open"))
        {
            return;
        }
        DoubleChest doubleChest = (DoubleChest) event.getInventory().getHolder();
        closeDoubleChest(doubleChest, event.getPlayer());
        p.removeMetadata("doublechest-open", this);
    }
}
