package com.namelessmc.plugin.spigot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.namelessmc.plugin.common.AbstractDataSender;
import com.namelessmc.plugin.common.NamelessPlugin;
import com.namelessmc.plugin.spigot.hooks.maintenance.MaintenanceStatusProvider;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class SpigotDataSender extends AbstractDataSender {

	protected SpigotDataSender(final @NotNull NamelessPlugin plugin,
							   final @NotNull NamelessPluginSpigot spigotPlugin) {
		super(plugin);

		final Configuration config = plugin.config().getMainConfig();

		// TPS TODO Send real TPS
		this.registerGlobalInfoProvider(json ->
				json.addProperty("tps", 20));

		// Permissions
		try {
			final Permission permissions = spigotPlugin.getPermissions();
			if (permissions != null) {
				this.registerGlobalInfoProvider(json -> {
					final String[] gArray = permissions.getGroups();
					final JsonArray groups = new JsonArray(gArray.length);
					Arrays.stream(gArray).map(JsonPrimitive::new).forEach(groups::add);
					json.add("groups", groups);
				});
				this.registerPlayerInfoProvider((json, player) -> {
					final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
					final String[] gArray = permissions.getPlayerGroups(bukkitPlayer);
					final JsonArray groups = new JsonArray(gArray.length);
					Arrays.stream(gArray).map(JsonPrimitive::new).forEach(groups::add);
					json.add("groups", groups);
				});
			}
		} catch (final UnsupportedOperationException ignored) {}

		// Maintenance
		MaintenanceStatusProvider maintenance = spigotPlugin.getMaintenanceStatusProvider();
		if (maintenance != null) {
			this.registerGlobalInfoProvider(json ->
					json.addProperty("maintenance", maintenance.maintenanceEnabled()));
		}

		final boolean uploadPlaceholders = config.getBoolean("server-data-sender.placeholders.enabled");

		// PlaceholderAPI placeholders
		if (uploadPlaceholders) {
			this.registerGlobalInfoProvider(json -> {
				final JsonObject placeholders = new JsonObject();
				config.getStringList("server-data-sender.placeholders.global").forEach((key) ->
						placeholders.addProperty(key, ChatColor.stripColor(spigotPlugin.getPapiParser().parse(null, "%" + key + "%")))
				);
				json.add("placeholders", placeholders);
			});
			this.registerPlayerInfoProvider((json, player) -> {
				final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
				final JsonObject placeholders = new JsonObject();
				config.getStringList("server-data-sender.placeholders.player").forEach((key) ->
						placeholders.addProperty(key, ChatColor.stripColor(spigotPlugin.getPapiParser().parse(bukkitPlayer, "%" + key + "%")))
				);
				json.add("placeholders", placeholders);
			});

		}

		// Location
		this.registerPlayerInfoProvider((json, player) -> {
			final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
			final JsonObject location = new JsonObject();
			final Location loc = bukkitPlayer.getLocation();
			location.addProperty("world", loc.getWorld().getName());
			location.addProperty("x", loc.getBlockX());
			location.addProperty("y", loc.getBlockY());
			location.addProperty("z", loc.getBlockZ());
			json.add("location", location);
		});

		Statistic playStat;
		try {
			playStat = Statistic.PLAY_ONE_TICK;
		} catch (final NoSuchFieldError ignored) {
			try {
				// it's PLAY_ONE_MINUTE in 1.13+ but unlike the name suggests it actually still records ticks played
				//noinspection JavaReflectionMemberAccess
				playStat = (Statistic) Statistic.class.getField("PLAY_ONE_MINUTE").get(null);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
					| SecurityException e) {
				throw new RuntimeException(e);
			}
		}
		final Statistic finalPlayStat = Objects.requireNonNull(playStat);

		// Misc player stats
		this.registerPlayerInfoProvider((json, player) -> {
			final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
			json.addProperty("playtime", bukkitPlayer.getStatistic(finalPlayStat) / 120);
			json.addProperty("ip", bukkitPlayer.getAddress().toString());
		});

	}

}
