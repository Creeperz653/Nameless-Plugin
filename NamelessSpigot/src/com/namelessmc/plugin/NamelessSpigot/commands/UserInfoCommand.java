package com.namelessmc.plugin.NamelessSpigot.commands;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.namelessmc.NamelessAPI.NamelessException;
import com.namelessmc.NamelessAPI.NamelessPlayer;
import com.namelessmc.plugin.NamelessSpigot.Chat;
import com.namelessmc.plugin.NamelessSpigot.Message;
import com.namelessmc.plugin.NamelessSpigot.NamelessPlugin;
import com.namelessmc.plugin.NamelessSpigot.Permission;
import com.namelessmc.plugin.NamelessSpigot.util.UUIDFetcher;

public class UserInfoCommand extends Command {

	public UserInfoCommand(String name) {
		super(name, Message.HELP_DESCRIPTION_GETUSER.getMessage(), "/" + name + "<user>");
		setPermission(Permission.COMMAND_ADMIN_GETUSER.toString());
		setPermissionMessage(Message.NO_PERMISSION.getMessage());
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if (args.length != 1) {
			sender.sendMessage(Message.INCORRECT_USAGE_GETUSER.getMessage().replace("%command%", label));
			return true;
		}
		
		Bukkit.getScheduler().runTaskAsynchronously(NamelessPlugin.getInstance(), () -> {
			final String targetID = args[0]; // Name or UUID

			NamelessPlayer target;
			
			try {
				// TODO Catch errors and display user friendly player not found message
				if (targetID.length() > 16) {
					//It's (probably) a UUID
					target = NamelessPlugin.getInstance().api.getPlayer(UUID.fromString(targetID));
				} else {
					//It's (probably) a name
					final UUID uuid = UUIDFetcher.getUUID(targetID);
					target = NamelessPlugin.getInstance().api.getPlayer(uuid);
				}
			} catch (IllegalArgumentException e) {
				sender.sendMessage("Invalid UUID.");
				return;
			} catch (NamelessException e) {
				sender.sendMessage(e.getMessage());
				e.printStackTrace();
				return;
			}

			if (!target.exists()) {
				sender.sendMessage(Message.PLAYER_NOT_FOUND.getMessage());
				return;
			}

			String separator = Chat.convertColors("&3&m--------------------------------");

			sender.sendMessage(separator);

			sender.sendMessage(Message.GETUSER_USERNAME.getMessage().replace("%username%", target.getUsername()));

			sender.sendMessage(Message.GETUSER_DISPLAYNAME.getMessage().replace("%displayname%", target.getDisplayName()));

			sender.sendMessage(Message.GETUSER_UUID.getMessage().replace("%uuid%", target.getUniqueId().toString()));

			sender.sendMessage(Message.GETUSER_GROUP_ID.getMessage().replace("%groupid%", "" + target.getGroupID()));

			sender.sendMessage(Message.GETUSER_REGISTERED.getMessage().replace("%registereddate%", target.getRegisteredDate().toString()));

			sender.sendMessage(Message.GETUSER_REPUTATION.getMessage().replace("%reputation%", "" + target.getReputations()));

			if (target.isValidated()) {
				sender.sendMessage(Chat.convertColors(Message.GETUSER_VALIDATED.getMessage() + "&a: " + Message.GETUSER_VALIDATED_YES.getMessage()));
			} else {
				sender.sendMessage(Chat.convertColors(Message.GETUSER_VALIDATED.getMessage() + "&c: " + Message.GETUSER_VALIDATED_NO.getMessage()));
			}

			if (target.isBanned()) {
				sender.sendMessage(Chat.convertColors(Message.GETUSER_BANNED.getMessage() + " &c: " + Message.GETUSER_BANNED_YES.getMessage()));
			} else {
				sender.sendMessage(Chat.convertColors(Message.GETUSER_BANNED.getMessage() + "&a: " + Message.GETUSER_BANNED_NO.getMessage()));
			}

			sender.sendMessage(separator);
		});
		return true;
	}

}