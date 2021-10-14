package com.namelessmc.plugin.spigot;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.FileUtils;

public enum Config {

	MAIN("config.yml", false),
	COMMANDS("commands.yml", false),

	;

	private final @NotNull String fileName;
	private final boolean autoSave;

	private @Nullable FileConfiguration configuration;
	private final @NotNull File file;

	Config(final @NotNull String fileName, final boolean autoSave){
		this.fileName = fileName;
		this.autoSave = autoSave;

		final File dataFolder = NamelessPlugin.getInstance().getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		this.file = new File(NamelessPlugin.getInstance().getDataFolder(), this.fileName);
	}

	public static void initialize() {
		final NamelessPlugin plugin = NamelessPlugin.getInstance();

		// Create config directory
		if (!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdirs();
		}

		// Create config files if missing
		for (final Config config : Config.values()) {
			config.reload();
		}
	}

	public @NotNull FileConfiguration getConfig() {
		if (this.configuration == null) {
			reload();
		}

		return this.configuration;
	}

	public void setConfig(final FileConfiguration config) {
		this.configuration = config;
	}

	public boolean autoSave() {
		return this.autoSave;
	}

	public void reload() {
		if (!this.file.exists()) {
			try {
				FileUtils.copyOutOfJar(Config.class, "/" + this.fileName, this.file);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}

		this.configuration = YamlConfiguration.loadConfiguration(this.file);
	}

	public void save() {
		if (this.configuration != null) {
			try {
				this.configuration.save(this.file);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
