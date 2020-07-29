package com.darkender.plugins.containerpassthrough;

import org.bukkit.Bukkit;
import org.bukkit.block.Chest;
import org.bukkit.entity.HumanEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils
{
    private Constructor<?> blockPositionConstructor;
    private Method getTileEntityMethod;
    private Field viewCountField;
    private Method closeContainerMethod;
    
    public ReflectionUtils()
    {
        try
        {
            Class<?> blockPositionClass = getNmsClass("BlockPosition");
            blockPositionConstructor = blockPositionClass.getConstructor(int.class, int.class, int.class);
            
            getTileEntityMethod = getNmsClass("WorldServer")
                    .getDeclaredMethod("getTileEntity", blockPositionClass, boolean.class);
            getTileEntityMethod.setAccessible(true);
    
            Class<?> tileEntityChest = getNmsClass("TileEntityChest");
            closeContainerMethod = tileEntityChest.getMethod("closeContainer",
                    getNmsClass("EntityHuman"));
    
            viewCountField = tileEntityChest.getDeclaredField("viewingCount");
            viewCountField.setAccessible(true);
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private Object getTileEntity(Chest chest)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException
    {
        Object blockPosition = blockPositionConstructor.newInstance(chest.getX(), chest.getY(), chest.getZ());
        Object worldHandle = chest.getWorld().getClass().getMethod("getHandle").invoke(chest.getWorld());
        return getTileEntityMethod.invoke(worldHandle, blockPosition, false);
    }
    
    public void closeContainer(Chest chest, HumanEntity entity)
    {
        try
        {
            Object entityHuman = entity.getClass().getMethod("getHandle").invoke(entity);
            closeContainerMethod.invoke(getTileEntity(chest), entityHuman);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public int getViewers(Chest chest)
    {
        try
        {
            return (int) viewCountField.get(getTileEntity(chest));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return -1;
    }
    
    private Class<?> getNmsClass(String nmsClassName) throws ClassNotFoundException
    {
        return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + "." + nmsClassName);
    }
}
