package com.namelessmc.plugin.common;

import com.namelessmc.java_api.*;
import com.namelessmc.java_api.exception.UnknownNamelessVersionException;
import com.namelessmc.plugin.common.command.AbstractScheduler;
import com.namelessmc.plugin.common.logger.AbstractLogger;
import net.md_5.bungee.config.Configuration;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("OptionalAssignedToNull")
public class ApiProvider implements Reloadable {

	private static final String USER_AGENT = "Nameless-Plugin";

	private Optional<NamelessAPI> cachedApi; // null if not cached

	private final @NotNull AbstractScheduler scheduler;
	private final @NotNull AbstractLogger logger;
	private final @NotNull ConfigurationHandler config;

	private String apiUrl;
	private String apiKey;
	private boolean debug;
	private Duration timeout;
	private boolean bypassVersionCheck;

	public ApiProvider(final @NotNull AbstractScheduler scheduler,
					   final @NotNull AbstractLogger logger,
					   final @NotNull ConfigurationHandler config) {
		this.scheduler = scheduler;
		this.logger = logger;
		this.config = config;
	}

	@Override
	public void reload() {
		final Configuration config = this.config.getMainConfig();
		this.apiUrl = config.getString("api.url");
		this.apiKey = config.getString("api.key");
		this.debug = config.getBoolean("api.debug", false);
		this.timeout = Duration.ofMillis(config.getInt("api.timeout", 5000));
		this.bypassVersionCheck = config.getBoolean("api.bypass-version-check", false);

		this.cachedApi = null;

		scheduler.runAsync(this::getNamelessApi);
	}

	// For bStats
	public String isApiWorkingMetric() {
		if (this.cachedApi == null) {
			// In theory the API should always be cached, but in case it's not we
			// do not want to force load it because that would affect server performance.
			return "Unknown";
		}

		if (this.cachedApi.isPresent()) {
			return "Working";
		} else {
			return "Not working";
		}
	}

	public synchronized Optional<NamelessAPI> getNamelessApi() {
		Objects.requireNonNull(logger, "Exception logger not initialized before API was requested. This is a bug.");

		if (this.cachedApi != null) {
			return this.cachedApi;
		}

		this.cachedApi = Optional.empty();

		try {
			if (this.apiUrl == null || this.apiUrl.isEmpty()) {
				this.logger.severe("You have not entered an API URL in the config. Please get your site's API URL from StaffCP > Configuration > API and reload the plugin.");
			} else if (this.apiKey == null || this.apiKey.isEmpty()) {
				this.logger.severe("You have not entered an API key in the config. Please get your site's API key from StaffCP > Configuration > API and reload the plugin.");
			} else {
				URL url = null;
				try {
					url = new URL(this.apiUrl);
				} catch (MalformedURLException e){
					this.logger.severe("You have entered an invalid API URL. Please get an up-to-date API URL from StaffCP > Configuration > API and reload the plugin.");
					this.logger.severe("Error message: '" + e.getMessage() + "'");
				}

				if (url != null) {
					final NamelessAPI api = NamelessAPI.builder(url, this.apiKey)
							.userAgent(USER_AGENT)
							.customDebugLogger(this.debug ? this.logger.getApiLogger() : null)
							.timeout(this.timeout)
							.build();

					final Website info = api.getWebsite();
					try {
						NamelessVersion version = info.getParsedVersion();
						if (this.bypassVersionCheck) {
							this.logger.warning("Bypassing version checks, use at your own risk!");
							this.cachedApi = Optional.of(api);
						} else if (NamelessVersion.isSupportedByJavaApi(version)) {
							this.logger.info("Website connection appears to be working.");
							this.cachedApi = Optional.of(api);
						} else {
							this.logger.severe("Your website runs a version of NamelessMC (" + version + ") that is not supported by this version of the plugin. Please update your NamelessMC website and/or the plugin.");
						}
					} catch (final UnknownNamelessVersionException e) {
						this.logger.severe("The plugin doesn't recognize the NamelessMC version you are using. Ensure you are running a recent version of the plugin and NamelessMC v2.");
					}
				}
			}
		} catch (final ApiError e) {
			if (e.getError() == ApiError.INVALID_API_KEY) {
				this.logger.severe("You have entered an invalid API key. Please get an up-to-date API URL from StaffCP > Configuration > API and reload the plugin.");
			} else {
				this.logger.severe("Encountered an unexpected error code " + e.getError() + " while trying to connect to your website. Enable api debug mode in the config file for more details. When you think you've fixed the problem, reload the plugin to attempt connecting again.");
			}
		} catch (final NamelessException e) {
			this.logger.warning("Encountered an error while connecting to the website. This message is expected if your site is down temporarily and can be ignored if the plugin works fine otherwise. If the plugin doesn't work as expected, please enable api-debug-mode in the config and run /nlpl reload to get more information.");
			// Do not cache so it immediately tries again the next time. These types of errors may fix on their
			// own, so we don't want to break the plugin until the administrator reloads.
			if (this.debug) {
				this.logger.warning("Debug is enabled, printing full error message:");
				this.logger.logException(e);
			}

			this.cachedApi = null;
			return Optional.empty();
		}

		return this.cachedApi;
	}

}
