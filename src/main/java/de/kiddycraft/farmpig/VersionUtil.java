package de.kiddycraft.farmpig;

import lombok.NonNull;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtil {
    private final static String VERSION;

    static {
        Pattern pattern = Pattern.compile("(\\(MC: ([1-9\\.]+)\\))");
        Matcher matcher = pattern.matcher(Bukkit.getVersion());
        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format("Invalid version format for %s", Bukkit.getVersion()));
        }
        VERSION = matcher.group(2);
    }

    public static boolean versionEquals(@NonNull String version) {
        return version.equals(VERSION);
    }
}
