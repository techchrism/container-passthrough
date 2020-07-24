package com.darkender.plugins.itemframecontainerpassthrough;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemframeContainerPassthrough extends JavaPlugin implements Listener
{
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }
}
