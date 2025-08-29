package net.nightzy.paysign;

import java.util.Objects;

import org.bukkit.ChatColor;

/**
 * Abstract message renderer for PaySign plugin.
 * Provides formatted messages for various situations: errors, success, notifications, etc.
 */
public abstract class MessageRenderer {

    // ============================================================
    // Public messages
    // ============================================================

    /**
     * Message when deposit to owner fails.
     */
    public String cantDeposit() {
        return this.error("Could not deposit to target player.");
    }

    /**
     * Message when a sign is successfully created.
     */
    public String createdSuccessfully() {
        return this.success("The PaySign has been successfully created.");
    }

    /**
     * Message when decimal prices are disabled.
     */
    public String disabledDecimals() {
        return this.error("Decimal prices are not allowed on this server.");
    }

    /**
     * Message when player cannot create a sign.
     */
    public String noPermissionToCreate() {
        return this.error("You do not have permission to create this PaySign.");
    }

    /**
     * Message when player cannot create a sign for another player.
     */
    public String noPermissionToCreateOther() {
        return this.error("You do not have permission to create this PaySign for other players.");
    }

    /**
     * Message when player cannot use a PaySign.
     */
    public String noPermissionToUse() {
        return this.error("You do not have permission to use this PaySign.");
    }

    /**
     * Notification message to owner when their sign is used.
     */
    public String notification(String playerName, String formattedPrice) {
        Objects.requireNonNull(playerName, "playerName cannot be null");
        Objects.requireNonNull(formattedPrice, "formattedPrice cannot be null");
        return this.fine(playerName + " has paid " + formattedPrice + " using your PaySign.");
    }

    /**
     * Message to player after a successful payment.
     */
    public String paid(String formattedPrice, String ownerName) {
        Objects.requireNonNull(formattedPrice, "formattedPrice cannot be null");
        Objects.requireNonNull(ownerName, "ownerName cannot be null");
        return this.success(formattedPrice + " has been withdrawn from your account to use " + ownerName + "'s PaySign.");
    }

    /**
     * Message when player cannot afford the sign.
     */
    public String tooPoor() {
        return this.error("You are too poor to use this PaySign.");
    }

    // ============================================================
    // Formatting helpers
    // ============================================================

    /**
     * Formats an error message in red.
     */
    public String error(String text) {
        return this.colored(text, ChatColor.RED);
    }

    /**
     * Formats a success message in green.
     */
    private String success(String text) {
        return this.colored(text, ChatColor.GREEN);
    }

    /**
     * Formats a fine/neutral message in gray.
     */
    private String fine(String text) {
        return this.colored(text, ChatColor.GRAY);
    }

    /**
     * Colors a message and adds plugin-specific prefix.
     */
    private String colored(String text, ChatColor color) {
        Objects.requireNonNull(color, "color cannot be null");
        return this.prefixed(color.toString() + text);
    }

    // ============================================================
    // Abstract method
    // ============================================================

    /**
     * Adds a prefix to all messages.
     */
    public abstract String prefixed(String text);
}
