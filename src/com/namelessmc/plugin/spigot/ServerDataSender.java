package com.namelessmc.plugin.spigot;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.namelessmc.java_api.ApiError;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.plugin.common.logger.AbstractLogger;
import com.namelessmc.plugin.spigot.hooks.maintenance.MaintenanceStatusProvider;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.StreamSupport;

public class ServerDataSender extends BukkitRunnable {

	@Override
	public void run() {
		final AbstractLogger logger = NamelessPlugin.getInstance().getCommonLogger();
		final FileConfiguration config = NamelessPlugin.getInstance().getConfig();
		final int serverId = config.getInt("server-data-sender.server-id");

		final JsonObject data = new JsonObject();
		data.addProperty("tps", 20); // TODO Send real TPS
		data.addProperty("time", System.currentTimeMillis());
		data.addProperty("free-memory", Runtime.getRuntime().freeMemory());
		data.addProperty("max-memory", Runtime.getRuntime().maxMemory());
		data.addProperty("allocated-memory", Runtime.getRuntime().totalMemory());
		data.addProperty("server-id", serverId);

		final net.milkbowl.vault.permission.Permission permissions = NamelessPlugin.getInstance().getPermissions();

		try {
			if (permissions != null) {
				final String[] gArray = permissions.getGroups();
				final JsonArray groups = new JsonArray(gArray.length);
				Arrays.stream(gArray).map(JsonPrimitive::new).forEach(groups::add);
				data.add("groups", groups);
			}
		} catch (final UnsupportedOperationException ignored) {}

		MaintenanceStatusProvider maintenance = NamelessPlugin.getInstance().getMaintenanceStatusProvider();
		if (maintenance != null) {
			data.addProperty("maintenance", maintenance.maintenanceEnabled());
		}

		final boolean uploadPlaceholders = config.getBoolean("server-data-sender.placeholders.enabled");

		if (uploadPlaceholders) {
			final JsonObject placeholders = new JsonObject();
			config.getStringList("server-data-sender.placeholders.global").forEach((key) ->
				placeholders.addProperty(key, ChatColor.stripColor(NamelessPlugin.getInstance().getPapiParser().parse(null, "%" + key + "%")))
			);
			data.add("placeholders", placeholders);
		}

		final JsonObject players = new JsonObject();

		for (final Player player : Bukkit.getOnlinePlayers()) {
			final JsonObject playerInfo = new JsonObject();

			playerInfo.addProperty("name", player.getName());

			final JsonObject location = new JsonObject();
			final Location loc = player.getLocation();
			location.addProperty("world", loc.getWorld().getName());
			location.addProperty("x", loc.getBlockX());
			location.addProperty("y", loc.getBlockY());
			location.addProperty("z", loc.getBlockZ());

			playerInfo.add("location", location);
			playerInfo.addProperty("ip", player.getAddress().getAddress().getHostAddress());
			// it's PLAY_ONE_MINUTE in 1.13+ but unlike the name suggests it actually still records ticks played
			Statistic playStat = Statistic.PLAY_ONE_MINUTE;
			playerInfo.addProperty("playtime", player.getStatistic(playStat) / 120);
			JsonArray completedAdvancements = new JsonArray();
			Streams.stream(Bukkit.advancementIterator())
					.filter(a -> player.getAdvancementProgress(a).isDone())
					.map(Advancement::getKey)
					.map(NamespacedKey::toString)
					.forEach(completedAdvancements::add);
			playerInfo.add("completed_advancements", completedAdvancements);

			try {
				if (permissions != null) {
					final String[] gArray = permissions.getPlayerGroups(player);
					final JsonArray groups = new JsonArray(gArray.length);
					Arrays.stream(gArray).map(JsonPrimitive::new).forEach(groups::add);
					playerInfo.add("groups", groups);
				}
			} catch (final UnsupportedOperationException ignored) {}

			if (uploadPlaceholders) {
				final JsonObject placeholders = new JsonObject();
				config.getStringList("server-data-sender.placeholders.player").forEach((key) ->
					placeholders.addProperty(key, ChatColor.stripColor(NamelessPlugin.getInstance().getPapiParser().parse(player, "%" + key + "%")))
				);
				playerInfo.add("placeholders", placeholders);
			}

			playerInfo.addProperty("login-time", NamelessPlugin.LOGIN_TIME.get(player.getUniqueId()));

			players.add(player.getUniqueId().toString().replace("-", ""), playerInfo);
		}

		data.add("players", players);

		Bukkit.getScheduler().runTaskAsynchronously(NamelessPlugin.getInstance(), () ->
			NamelessPlugin.getInstance().getNamelessApi().ifPresent(api -> {
				try {
					api.submitServerInfo(data);
				} catch (final ApiError e) {
					if (e.getError() == ApiError.INVALID_SERVER_ID) {
						logger.warning("Server ID is incorrect. Please enter a correct server ID or disable the server data uploader.");
					} else {
						logger.logException(e);
					}
				} catch (final NamelessException e) {
					if (e.getCause() instanceof SocketTimeoutException) {
						logger.warning("Connection timed out while sending server data to NamelessMC. Was your webserver down, or is your network connection unstable?");
					} else {
						logger.logException(e);
					}
				}
			})
		);
	}

}
