package com.namelessmc.plugin.bungee;

import com.namelessmc.plugin.common.command.CommandSender;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BungeeCommandSender extends CommandSender {

	private final net.md_5.bungee.api.CommandSender sender;
	private final Audience audience;

	public BungeeCommandSender(final @NotNull BungeeAudiences audiences,
							   final @NotNull net.md_5.bungee.api.CommandSender sender) {
		this.sender = sender;
		this.audience = audiences.sender(sender);
	}

	@Override
	public boolean isPlayer() {
		return this.sender instanceof ProxiedPlayer;
	}

	@Override
	public UUID getUniqueId() {
		if (this.isPlayer()) {
			return ((ProxiedPlayer) sender).getUniqueId();
		}
		throw new UnsupportedOperationException("Cannot use getUniqueId for console sender");
	}

	@Override
	public String getName() {
		if (this.isPlayer()) {
			return sender.getName();
		}
		throw new UnsupportedOperationException("Cannot use getName for console sender");
	}

	@Override
	public Audience audience() {
		return this.audience;
	}

}
