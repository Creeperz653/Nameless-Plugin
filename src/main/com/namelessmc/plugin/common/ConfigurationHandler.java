package com.namelessmc.plugin.common;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import xyz.derkades.derkutils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationHandler implements Reloadable {

	private final @NotNull Path dataDirectory;
	private Configuration mainConfig;
	private Configuration commandsConfig;

	public ConfigurationHandler(final @NotNull Path dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	public @NotNull Configuration getMainConfig() {
		return this.mainConfig;
	}

	public @NotNull Configuration getCommandsConfig() {
		return this.commandsConfig;
	}

	@Override
	public void reload() {
		try {
			Files.createDirectories(dataDirectory);
			this.mainConfig = copyFromJarAndLoad("config.yaml");
			this.commandsConfig = copyFromJarAndLoad("commands.yaml");
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Configuration copyFromJarAndLoad(final @NotNull String name) throws IOException {
		Path path = dataDirectory.resolve(name);
		FileUtils.copyOutOfJar(ConfigurationHandler.class, name, path);
		return ConfigurationProvider.getProvider(YamlConfiguration.class).load(path.toFile());
	}

}
