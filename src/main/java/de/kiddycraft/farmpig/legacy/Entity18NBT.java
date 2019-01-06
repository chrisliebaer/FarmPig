package de.kiddycraft.farmpig.legacy;

import org.bukkit.entity.LivingEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("FieldCanBeLocal")
public class Entity18NBT implements EntityTagManipulation {
	
	private final Class<?> clazzEntityLiving;
	private final Class<?> clazzNbtTagCompound;
	private final Class<?> clazzCraftLivingEntity;
	
	private final Method methodCraftLivingEntityGetHandle;
	
	private final Method methodEntityLivingGetNBTTag;
	private final Method methodEntityLivingC;
	private final Method methodEntityLivingF;
	
	private final Method methodNBTTagsetInt;
	private final Method methodNBTTagsetBoolean;
	
	
	public Entity18NBT() throws ClassNotFoundException, NoSuchMethodException {
		clazzEntityLiving = Class.forName("net.minecraft.server.v1_8_R3.EntityLiving");
		clazzNbtTagCompound = Class.forName("net.minecraft.server.v1_8_R3.NBTTagCompound");
		clazzCraftLivingEntity = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity");
		
		methodCraftLivingEntityGetHandle = clazzCraftLivingEntity.getMethod("getHandle");
		methodEntityLivingGetNBTTag = clazzEntityLiving.getMethod("getNBTTag");
		
		// enter obfuscation
		methodEntityLivingC = clazzEntityLiving.getMethod("c", clazzNbtTagCompound);
		methodEntityLivingF = clazzEntityLiving.getMethod("f", clazzNbtTagCompound);
		
		methodNBTTagsetInt = clazzNbtTagCompound.getMethod("setInt", String.class, Integer.TYPE);
		methodNBTTagsetBoolean = clazzNbtTagCompound.getMethod("setBoolean", String.class, Boolean.TYPE);
	}
	
	@Override
	public void updateEntity(LivingEntity entity) {
		try {
			// returns EntityLiving (sic)
			Object entityLiving = methodCraftLivingEntityGetHandle.invoke(entity);
			
			// return NBTTagCompound, can be null if there is none
			Object nbtTagCompound = methodEntityLivingGetNBTTag.invoke(entityLiving);
			if (nbtTagCompound == null)
				nbtTagCompound = clazzNbtTagCompound.getConstructor().newInstance();
			
			// does something, whatever ¯\_(ツ)_/¯
			methodEntityLivingC.invoke(entityLiving, nbtTagCompound);
			
			// the reason we are even here
			methodNBTTagsetInt.invoke(nbtTagCompound, "NoAI", 1);
			methodNBTTagsetBoolean.invoke(nbtTagCompound, "Silent", true);
			
			// does something else ¯\_(ツ)_/¯
			methodEntityLivingF.invoke(entityLiving, nbtTagCompound);
			
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
			throw new RuntimeException("Unexpected error in legacy reflection call", e);
		}
	}
}
