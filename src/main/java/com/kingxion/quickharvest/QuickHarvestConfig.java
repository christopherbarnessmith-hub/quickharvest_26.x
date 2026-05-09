package com.kingxion.quickharvest;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class QuickHarvestConfig {

    private static final Path CONFIG_PATH = Paths.get("config", "quickharvest.properties");

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            writeDefaults();
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
        } catch (IOException e) {
            QuickHarvest.LOGGER.warn("Could not read quickharvest.properties, using defaults. ({})", e.getMessage());
            return;
        }

        QuickHarvest.REQUIRE_HOE    = parseBool(props,  "require_hoe",    QuickHarvest.REQUIRE_HOE);
        QuickHarvest.HOE_DURABILITY = parseBool(props,  "hoe_durability",  QuickHarvest.HOE_DURABILITY);
        QuickHarvest.AUTO_DETECT    = parseBool(props,  "auto_detect",     QuickHarvest.AUTO_DETECT);
        QuickHarvest.XP_ENABLED     = parseBool(props,  "xp_enabled",      QuickHarvest.XP_ENABLED);
        QuickHarvest.XP_AMOUNT      = parseInt(props, QuickHarvest.XP_AMOUNT);
        QuickHarvest.XP_CHANCE      = parseFloat(props, QuickHarvest.XP_CHANCE);
        QuickHarvest.ALLOWLIST      = parseList(props,  "allowlist");
        QuickHarvest.DENYLIST       = parseList(props,  "denylist");

        QuickHarvest.LOGGER.info(
                "QuickHarvest config loaded: require_hoe={}, hoe_durability={}, auto_detect={}, " +
                        "xp_enabled={}, xp_amount={}, xp_chance={}",
                QuickHarvest.REQUIRE_HOE, QuickHarvest.HOE_DURABILITY, QuickHarvest.AUTO_DETECT,
                QuickHarvest.XP_ENABLED, QuickHarvest.XP_AMOUNT, QuickHarvest.XP_CHANCE
        );
    }

    private static void writeDefaults() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(CONFIG_PATH))) {
                w.println("# Ion Quick Harvest Configuration");
                w.println("# Changes take effect on next server/world load.");
                w.println();
                w.println("# If true, an empty hand does nothing — a hoe is required to harvest.");
                w.println("require_hoe=false");
                w.println();
                w.println("# If true, harvesting with a hoe costs 1 durability. Unbreaking is respected.");
                w.println("hoe_durability=true");
                w.println();
                w.println("# If true, modded crops are auto-detected by common age/growth properties.");
                w.println("# Disable and use the allowlist if you get false positives.");
                w.println("auto_detect=true");
                w.println();
                w.println("# XP settings");
                w.println("xp_enabled=false");
                w.println("xp_amount=1");
                w.println("# Chance of XP dropping per harvest (0.0 = never, 1.0 = always)");
                w.println("xp_chance=0.5");
                w.println();
                w.println("# Comma-separated block IDs that should ALWAYS be harvestable.");
                w.println("# Example: farmersdelight:tomatoes,mymod:berries");
                w.println("allowlist=");
                w.println();
                w.println("# Comma-separated block IDs that should NEVER be harvested.");
                w.println("# Useful for resolving conflicts with other mods.");
                w.println("denylist=");
            }
        } catch (IOException e) {
            QuickHarvest.LOGGER.warn("Could not write default quickharvest.properties: {}", e.getMessage());
        }
    }

    private static boolean parseBool(Properties props, String key, boolean fallback) {
        String val = props.getProperty(key);
        return val != null ? Boolean.parseBoolean(val.trim()) : fallback;
    }

    private static int parseInt(Properties props, int fallback) {
        String val = props.getProperty("xp_amount");
        if (val == null) return fallback;
        try { return Integer.parseInt(val.trim()); }
        catch (NumberFormatException e) {
            QuickHarvest.LOGGER.warn("Invalid int for '{}' in quickharvest.properties, using {}", "xp_amount", fallback);
            return fallback;
        }
    }

    private static float parseFloat(Properties props, float fallback) {
        String val = props.getProperty("xp_chance");
        if (val == null) return fallback;
        try { return Float.parseFloat(val.trim()); }
        catch (NumberFormatException e) {
            QuickHarvest.LOGGER.warn("Invalid float for '{}' in quickharvest.properties, using {}", "xp_chance", fallback);
            return fallback;
        }
    }

    private static Set<String> parseList(Properties props, String key) {
        String val = props.getProperty(key, "").trim();
        if (val.isEmpty()) return new HashSet<>();
        Set<String> result = new HashSet<>();
        for (String entry : val.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }
}