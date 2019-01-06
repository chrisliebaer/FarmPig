package de.kiddycraft.farmpig.legacy;

import de.kiddycraft.farmpig.FarmPigInstance;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface FarmpigEnumerator {
	
	public void enumerate(Player player, UUID uuid, FarmPigInstance instance);
}
