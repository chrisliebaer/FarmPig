package de.kiddycraft.farmpig;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@SuppressWarnings("ThrowCaughtLocally")
public class FarmPigPlugin extends JavaPlugin implements CommandExecutor {
	
	private static final long RESPAWN_TICKS = 20;
	private static final List<EntityType> VALID_TYPES =
			stream(EntityType.values()).filter(t -> t.isAlive() && t.isSpawnable()).collect(Collectors.toList());
	
	private HashMap<UUID, FarmPigInstance> instances = new HashMap<>();
	
	@Override
	public void onEnable() {
		// TODO: find way to implement server version check?
		
		// load config from disk and spawn farmpigs
		File configFile = new File(getDataFolder(), "farmpigs.json");
		if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
			try (var reader = new FileReader(configFile)) {
				List<FarmPigConfig> list = new Gson().fromJson(reader, new TypeToken<List<FarmPigConfig>>(){}.getType());
				for (FarmPigConfig farmpigConfig : list) {
					
					var instance = farmpigConfig.toFarmPigInstance(this);
					if (instance != null) {
						instances.put(UUID.randomUUID(), instance);
						getServer().getPluginManager().registerEvents(instance, this);
						instance.spawn();
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("failed to load farmpigs config", e);
			}
		}
		
		// attempt to find leftover entities in world and remove
		// TODO would cause disk IO every time new entity is spawned since new uuid
		
		getServer().getPluginCommand("farmpig").setExecutor(this);
	}
	
	@Override
	public void onDisable() {
		// remove all active farmpigs from world
		instances.values().forEach(FarmPigInstance::despawn);
		
		// no need to store config since always up to date
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (sender instanceof Player) {
			if (args.length < 1 || !sender.hasPermission("kiddycraft.farmpig"))
				return false;
			
			String subCmd = args[0];
			switch (subCmd) {
				case "list":
					sender.sendMessage("§lThese are all §5farmpigs§r§l on this server");
					for (var entry : instances.entrySet()) {
						var instance = entry.getValue();
						var uuid = entry.getKey();
						
						// info
						// TODO: replace with naked json https://minecraftjson.com/
						ComponentBuilder builder = new ComponentBuilder("");
						// builder.append(TextComponent.fromLegacyText(" §l[§b*§r§l]§r §l§4Name:§r " + instance.getNameTag() + "§r, "));
						
						// add view option if permission is present
						if (sender.hasPermission("kiddycraft.farmpig.view")) {
							TextComponent view = new TextComponent("[View]");
							view.setColor(net.md_5.bungee.api.ChatColor.GREEN);
							view.setHoverEvent(
									new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Teleports you to this farmpig's location").create()));
							view.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/farmpig view " + uuid));
							// builder.append(view);
						}
						
						builder.append(" ");
						
						// add remove option if permission is present
						if (sender.hasPermission("kiddycraft.farmpig.remove")) {
							TextComponent remove = new TextComponent("[Remove]");
							remove.setColor(net.md_5.bungee.api.ChatColor.DARK_RED);
							remove.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/farmpig remove " + uuid.toString()));
							remove.setHoverEvent(
									new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Removes this farmpig from the game").create()));
							// builder.append(remove);
						}
						
						sender.spigot().sendMessage(builder.create());
					}
					break;
				case "add":
					if (args.length < 2 || !sender.hasPermission("kiddycraft.farmpig.add"))
						return false;
					EntityType type;
					try {
						type = EntityType.valueOf(args[1].toUpperCase());
						if (!VALID_TYPES.contains(type)) {
							throw new IllegalArgumentException("invalid entity type");
						}
					} catch (IllegalArgumentException e) {
						sender.sendMessage("You entered an invalid entity type, valids types are: " + Joiner.on(", ").join(VALID_TYPES));
						return false;
					}
					
					// extract entity name
					String name = null;
					if (args.length >= 3) { // check if name is given
						name = Joiner.on(' ').join(Arrays.copyOfRange(args, 2, args.length));
						name = ChatColor.translateAlternateColorCodes('&', name);
						
					}
					
					addFarmPig(((Player) sender).getLocation(), name, type);
					sender.sendMessage("farmpig has been added at your location");
					
					break;
				case "remove":
					if (args.length < 2 || !sender.hasPermission("kiddycraft.farmpig.remove"))
						return false;
					try {
						UUID uuid = UUID.fromString(args[1]);
						if (instances.containsKey(uuid)) {
							removeFarmPig(uuid);
							sender.sendMessage("farmpig has been removed");
						} else {
							sender.sendMessage("This entity does not exist and can't be deleted");
							return false;
						}
					} catch (IllegalArgumentException e) {
						sender.sendMessage("This is not a valid uuid, please do not attempt to use this command directly");
						return false;
					}
					break;
				case "respawn":
					if (!sender.hasPermission("kiddycraft.farmpig.respawn"))
						return false;
					instances.values().forEach(FarmPigInstance::respawn);
					break;
				case "view": // internal command for view option
					if (args.length < 2 || !sender.hasPermission("kiddycraft.farmpig.view"))
						return false;
					
					try {
						UUID uuid = UUID.fromString(args[1]);
						var instance = instances.get(uuid);
						if (instance == null)
							return false;
						
						((Player) sender).teleport(instance.getLocation());
						
					} catch (IllegalArgumentException e) {
						sender.sendMessage("This is not a valid uuid, please do not attempt to use this command directly");
						return false;
					}
					
					break;
				default:
					return false;
				
			}
			return true;
		} else {
			sender.sendMessage("Command requires player context.");
			return false;
		}
	}
	
	private void addFarmPig(Location location, String name, EntityType type) {
		FarmPigInstance instance = createNew(location, type, name, RESPAWN_TICKS);
		instances.put(UUID.randomUUID(), instance);
		
		// trigger first spawn and register instance in event loop
		instance.spawn();
		getServer().getPluginManager().registerEvents(instance, this);
		
		saveFarmPigs();
	}
	
	private void removeFarmPig(UUID uuid) {
		FarmPigInstance instance = instances.remove(uuid);
		Preconditions.checkArgument(instance != null, "attempted to remove non existing farm pig");
		instance.despawn();
		HandlerList.unregisterAll(instance);
		
		saveFarmPigs();
	}
	
	private void saveFarmPigs() {
		getDataFolder().mkdirs();
		try (var writer = new FileWriter(new File(getDataFolder(), "farmpigs.json"))) {
			Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
			
			// serialize config
			var list = instances.values().stream().map(FarmPigConfig::new).collect(Collectors.toList());
			gson.toJson(list, writer);
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "failed to write farmpig config", e);
		}
	}
	
	public FarmPigInstance createNew(Location location, EntityType type, String name, long respawnTicks) {
		return new FarmPigInstance(this, name, location, null, type, respawnTicks);
	}
	
	
	@Data
	private static final class FarmPigConfig {
		private String nameTag;
		private EntityType type;
		private long respawnTicks;
		private Map<String, Object> location;
		
		public FarmPigConfig() {}
		
		public FarmPigConfig(FarmPigInstance i) {
			nameTag = i.getNameTag();
			type = i.getType();
			respawnTicks = i.getRespawnTicks();
			location = i.getLocation().serialize();
		}
		
		public FarmPigInstance toFarmPigInstance(FarmPigPlugin plugin) {
			try {
				return new FarmPigInstance(plugin, nameTag, Location.deserialize(location), null, type, respawnTicks);
			} catch (IllegalArgumentException e) {
				return null; // world doesn't exist
			}
		}
	}
}
