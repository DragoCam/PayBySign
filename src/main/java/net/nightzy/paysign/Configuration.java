package net.nightzy.paysign;

import java.util.Objects;
import java.util.function.Supplier;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Plugin configuration wrapper.
 * Uses a Supplier<FileConfiguration> to allow dynamic reloads of the config.
 */
public class Configuration {

    // ============================================================
    // Default configuration values
    // ============================================================

    /** Default delay in ticks (same as wooden button delay) */
    private static final int DEFAULT_DELAY = 30;

    /** Whether decimal prices are allowed by default */
    private static final boolean DEFAULT_ALLOW_DECIMALS = true;

    // ============================================================
    // Fields
    // ============================================================

    /** Source of the FileConfiguration */
    private final Supplier<FileConfiguration> config;

    // ============================================================
    // Constructor
    // ============================================================

    /**
     * Creates a Configuration wrapper.
     * @param config Supplier that provides the current FileConfiguration
     */
    public Configuration(Supplier<FileConfiguration> config) {
        this.config = Objects.requireNonNull(config, "configuration cannot be null");
    }

    // ============================================================
    // Configuration accessors
    // ============================================================

    /**
     * Gets the current FileConfiguration from the supplier.
     * @return current FileConfiguration
     */
    public FileConfiguration getConfig() {
        return Objects.requireNonNull(this.config.get(), "FileConfiguration cannot be null");
    }

    /**
     * Gets the configured redstone delay in ticks.
     * @return delay in ticks
     */
    public int delay() {
        return this.getConfig().getInt("delay", DEFAULT_DELAY);
    }

    /**
     * Determines whether decimal prices are allowed.
     * @return true if decimals are allowed, false otherwise
     */
    public boolean allowDecimals() {
        return this.getConfig().getBoolean("allow-decimals", DEFAULT_ALLOW_DECIMALS);
    }
}
