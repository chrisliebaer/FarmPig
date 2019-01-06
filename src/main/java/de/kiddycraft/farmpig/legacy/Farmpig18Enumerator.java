package de.kiddycraft.farmpig.legacy;

import com.google.gson.Gson;
import de.kiddycraft.farmpig.FarmPigInstance;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

@SuppressWarnings("FieldCanBeLocal")
public class Farmpig18Enumerator implements FarmpigEnumerator {
	
	private final Class<?> clazzCraftPlayer;
	private final Class<?> clazzEntityPlayer;
	private final Class<?> clazzIChatBaseComponent;
	private final Class<?> clazzChatSerializer;
	private final Class<?> clazzPacketPlayOutChat;
	private final Class<?> clazzPlayerConnection;
	private final Class<?> clazzPacket;
	
	private final Method methodCraftPlayerGetHandle;
	private final Method methodChatSerializerA;
	private final Method methodIChatBaseComponentAddSibling;
	
	private final Field fieldEntityPlayerPlayerConnection;
	private final Method methodPlayerConnectionSendPacket;
	
	private final Gson gson = new Gson();
	
	public Farmpig18Enumerator() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
		clazzCraftPlayer = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer");
		clazzEntityPlayer = Class.forName("net.minecraft.server.v1_8_R3.EntityPlayer");
		clazzIChatBaseComponent = Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent");
		clazzChatSerializer = Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent$ChatSerializer");
		clazzPacketPlayOutChat = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutChat");
		clazzPlayerConnection = Class.forName("net.minecraft.server.v1_8_R3.PlayerConnection");
		clazzPacket = Class.forName("net.minecraft.server.v1_8_R3.Packet");
		
		methodCraftPlayerGetHandle = clazzCraftPlayer.getMethod("getHandle");
		methodChatSerializerA = clazzChatSerializer.getMethod("a", String.class); // static method
		methodIChatBaseComponentAddSibling = clazzIChatBaseComponent.getMethod("addSibling", clazzIChatBaseComponent);
		methodPlayerConnectionSendPacket = clazzPlayerConnection.getMethod("sendPacket", clazzPacket);
		
		fieldEntityPlayerPlayerConnection = clazzEntityPlayer.getField("playerConnection");
	}
	
	@Override
	public void enumerate(Player player, UUID uuid, FarmPigInstance instance) {
		try {
			Object entityPlayer = methodCraftPlayerGetHandle.invoke(player);
			Object msg = createChatComponent("[\"\",{\"text\":\"§r §l[§b*§r§l]§r §l§4Name:§r \"}]");
			
			// we need to escape the name tag string
			if (instance.getNameTag() != null) {
				String nameTag = gson.toJson(instance.getNameTag());
				nameTag = nameTag.substring(1, nameTag.length() - 1);
				methodIChatBaseComponentAddSibling.invoke(msg, createChatComponent("[\"\",{\"text\":\"" + nameTag + " " + "\"}]"));
			} else {
				methodIChatBaseComponentAddSibling.invoke(msg, createChatComponent("[\"\",{\"text\":\"" + null + " " + "\"}]"));
			}
			
			if (player.hasPermission("kiddycraft.farmpig.view")) {
				methodIChatBaseComponentAddSibling.invoke(msg, createChatComponent("[\"\",{\"text\":\"[View]\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/farmpig view " + uuid + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Teleports you to this farmpig's location\"}]}}}]"));
			}
			
			if (player.hasPermission("kiddycraft.farmpig.remove")) {
				methodIChatBaseComponentAddSibling.invoke(msg, createChatComponent("[\"\",{\"text\":\"[Remove]\",\"color\":\"dark_red\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/farmpig remove " + uuid + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Removes this farmpig from the game\"}]}}}]"));
			}
			
			Object playerConnection = fieldEntityPlayerPlayerConnection.get(entityPlayer);
			methodPlayerConnectionSendPacket.invoke(playerConnection,
					clazzPacketPlayOutChat.getConstructor(clazzIChatBaseComponent).newInstance(msg));
			
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
			throw new RuntimeException("Unexpected error in legacy reflection call", e);
		}
	}
	
	private Object createChatComponent(String s) throws InvocationTargetException, IllegalAccessException {
		return methodChatSerializerA.invoke(null, s);
	}
}
