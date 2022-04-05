package com.namelessmc.plugin.spigot.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.namelessmc.plugin.spigot.NamelessPluginSpigot;

public class PlayerQuit implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		NamelessPluginSpigot.LOGIN_TIME.remove(player.getUniqueId());
	}

}
