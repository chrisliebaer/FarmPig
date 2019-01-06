package de.kiddycraft.farmpig.legacy;

import org.bukkit.entity.LivingEntity;

public class DefaultEntityTagManipulator implements EntityTagManipulation {
	
	@Override
	public void updateEntity(LivingEntity entity) {
		entity.setAI(false);
		entity.setSilent(true);
		entity.setCollidable(false);
	}
}
