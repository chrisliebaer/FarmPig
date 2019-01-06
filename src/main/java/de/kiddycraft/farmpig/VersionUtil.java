package de.kiddycraft.farmpig;

import lombok.NonNull;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtil {
    private static final String VERSION;

    static {
        Pattern pattern = Pattern.compile("(\\(MC: ([1-9.]+)\\))");
        Matcher matcher = pattern.matcher(Bukkit.getVersion());
        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format("Invalid version format for %s", Bukkit.getVersion()));
        }
        VERSION = matcher.group(2);
    }
	
	private VersionUtil() {}
	
	public static boolean versionEquals(@NonNull String version) {
        return version.equals(VERSION);
    }
}
