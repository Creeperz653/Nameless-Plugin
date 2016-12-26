package com.namelessmc.namelessplugin.spigot;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.namelessmc.namelessplugin.spigot.commands.GetNotificationsCommand;
import com.namelessmc.namelessplugin.spigot.commands.GetUserCommand;
import com.namelessmc.namelessplugin.spigot.commands.RegisterCommand;
import com.namelessmc.namelessplugin.spigot.commands.ReportCommand;
import com.namelessmc.namelessplugin.spigot.commands.SetGroupCommand;
import com.namelessmc.namelessplugin.spigot.mcstats.Metrics;
import com.namelessmc.namelessplugin.spigot.player.PlayerEventListener;
import com.namelessmc.namelessplugin.spigot.utils.MessagesUtil;
import com.namelessmc.namelessplugin.spigot.utils.PermissionHandler;
import com.namelessmc.namelessplugin.spigot.utils.ReflectionUtil;

import net.milkbowl.vault.permission.Permission;

public class NamelessPlugin extends JavaPlugin {

	private ReflectionUtil reflection;

	/*
	 * Metrics
	 */
	Metrics metrics;

	/*
	 *  API URL
	 */
	private String apiURL = "";

	/*
	 *  Vault Integration
	 */
	private boolean useVault = false;

	/*
	 *  Vault Permissions
	 */
	private Permission permissions = null;

	/*
	 *  Groups Support 
	 */
	@SuppressWarnings("unused")
	private boolean useGroups = false;

	/*
	 *  Enable reports?
	 */
	private boolean useReports = false;

	/*
	 *  Is the plugin disabled?
	 */
	private boolean isDisabled = false;

	/*
	 *  NamelessMC permissions strings.
	 */
	public final String permission = "namelessmc";
	public final String permissionAdmin = "namelessmc.admin";

	public ReflectionUtil getReflection(){
		return reflection;
	}

	/*
	 *  Gets API URL
	 */
	public String getAPIUrl(){
		return apiURL;
	}

	/*
	 *  OnEnable method
	 */
	@Override
	public void onEnable(){
		// Initialise  Files
		initConfig();
		initPlayerInfoFile();

		if(!isDisabled){
			// Check Vault
			detectVault();
			registerListeners();
		}
	}

	/*
	 * Register Commands/Events
	 */
	public void registerListeners(){
		// Register McStats
		try {
			metrics = new Metrics(this);
			metrics.start();
			Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&3Metrics Started!"));
		} catch (IOException e) {
			e.printStackTrace();
		} 

		// Register commands
		getCommand("register").setExecutor(new RegisterCommand(this));
		getCommand("getuser").setExecutor(new GetUserCommand(this));
		getCommand("getnotifications").setExecutor(new GetNotificationsCommand(this));
		getCommand("setgroup").setExecutor(new SetGroupCommand(this));

		if(useReports){
			getCommand("report").setExecutor(new ReportCommand(this));
		}

		// Register events
		getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
	}

	/*
	 * Check if Vault is Activated
	 */
	public void detectVault(){
				if(getServer().getPluginManager().getPlugin("Vault") != null){
					// Enable Vault integration and setup Permissions.
					useVault = true;
					initPermissions();
					// Check if the permissions plugin has groups.
					if(permissions.hasGroupSupport()){
						useGroups = true;
					} else {
						Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&4Permissions plugin does NOT support groups! Disabling NamelessMC group synchronisation."));
						useGroups = false;
					}
				} else {
					Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&4Couldn't detect Vault, disabling NamelessMC Vault integration."));
				}
	}

	/*
	 *  Initialise configuration
	 */
	private void initConfig(){
		// Check config exists, if not create one
		try {
			if(!getDataFolder().exists()){
				// Folder within plugins doesn't exist, create one now...
				getDataFolder().mkdirs();
			}

			File file = new File(getDataFolder(), "config.yml");

			if(!file.exists()){
				// Config doesn't exist, create one now...
				Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&1Creating NamelessMC configuration file..."));
				this.saveDefaultConfig();

				Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&4NamelessMC needs configuring, disabling..."));

				// Disable plugin
				getServer().getPluginManager().disablePlugin(this);

				isDisabled = true;

			} else {
				// Better way of loading config file, no need to reload.
				File configFile = new File(getDataFolder() + File.separator + "/config.yml");
				YamlConfiguration yamlConfigFile;
				yamlConfigFile = YamlConfiguration.loadConfiguration(configFile);

				// Exists already, load it
				Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&2Loading NamelessMC configuration file..."));

				apiURL = yamlConfigFile.getString("api-url");
				if(apiURL.isEmpty()){
					// API URL not set
					Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&4No API URL set in the NamelessMC configuration, disabling..."));
					getServer().getPluginManager().disablePlugin(this);
				}

				// Use the report system?
				useReports = yamlConfigFile.getBoolean("enable-reports");

				//Use group synchronization?
				if(getConfig().getBoolean("group-synchronization")){
					PermissionHandler phandler = new PermissionHandler(this);
					phandler.initConfig();
				}
			}

			boolean spigot = true;

			try {
				Class.forName("org.spigotmc.Metrics");
			} catch (Exception e) {
				spigot = false;
				e.printStackTrace();
			}

			if(spigot){
				Bukkit.getLogger().info("Spigot detected.");
				MessagesUtil messagesConfig = new MessagesUtil(this);
				messagesConfig.initMessages();
			}

		} catch(Exception e){
			// Exception generated
			e.printStackTrace();
		}
	}

	/*
	 *  Initialise Vault permissions integration for group sync
	 */
	private boolean initPermissions(){

		if(useVault){
			RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
			permissions = rsp.getProvider();
		}

		return permissions != null;
	}	

	/*
	 * Initialise The Player Info File
	 */
	private void initPlayerInfoFile() {
	    File iFile = new File(this.getDataFolder() + File.separator + "playersInformation.yml");
		if(!iFile.exists()){
			try {
				iFile.createNewFile();
				Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&2Created players information File."));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}