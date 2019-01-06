package de.kiddycraft.farmpig.legacy;

import de.kiddycraft.farmpig.FarmPigInstance;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DefaultFarmpigEnumerator implements FarmpigEnumerator {
	
	@Override
	public void enumerate(Player player, UUID uuid, FarmPigInstance instance) {
		// info
		ComponentBuilder builder = new ComponentBuilder("");
		builder.append(TextComponent.fromLegacyText(" §l[§b*§r§l]§r §l§4Name:§r " + instance.getNameTag() + "§r, "));
		
		// add view option if permission is present
		if (player.hasPermission("kiddycraft.farmpig.view")) {
			TextComponent view = new TextComponent("[View]");
			view.setColor(ChatColor.GREEN);
			view.setHoverEvent(
					new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Teleports you to this farmpig's location").create()));
			view.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/farmpig view " + uuid));
			builder.append(view);
		}
		builder.append(" ");
		
		// add remove option if permission is present
		if (player.hasPermission("kiddycraft.farmpig.remove")) {
			TextComponent remove = new TextComponent("[Remove]");
			remove.setColor(ChatColor.DARK_RED);
			remove.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/farmpig remove " + uuid));
			remove.setHoverEvent(
					new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Removes this farmpig from the game").create()));
			builder.append(remove);
		}
		
		player.spigot().sendMessage(builder.create());
	}
}
