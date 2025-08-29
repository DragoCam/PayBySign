package net.nightzy.paysign;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

/**
 * Represents a PaySign in the world.
 * Holds data about owner, price, delay and provides logic for payment & redstone triggering.
 */
public class PaySign {

    // ============================================================
    // Constants & Logger
    // ============================================================

    static final Logger logger = Logger.getLogger(PaySign.class.getName());

    protected static final String NAMESPACE = "[PaySign]";
    protected static final ChatColor NAMESPACE_COLOR = ChatColor.DARK_GREEN;

    // ============================================================
    // Fields
    // ============================================================

    private final Sign sign;           // Sign block instance
    private final String playerName;   // Owner of the sign
    private final double price;        // Price to use the sign
    private final int delay;           // Optional delay before reset (ticks)

    // ============================================================
    // Constructor
    // ============================================================

    public PaySign(Sign sign, String playerName, double price, int delay) {
        this.sign = Objects.requireNonNull(sign, "sign cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "playerName cannot be null");
        this.price = price;
        this.delay = delay;
    }

    // ============================================================
    // Getters
    // ============================================================

    public Sign getSign() {
        return this.sign;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    /**
     * Returns the owner of this sign if they are online.
     */
    public Optional<Player> getOwner(Server server) {
        Objects.requireNonNull(server, "server cannot be null");
        return Optional.ofNullable(server.getPlayer(this.playerName));
    }

    public double getPrice() {
        return this.price;
    }

    /**
     * Returns the price depending on whether decimals are allowed.
     */
    public double getPrice(boolean allowDecimals) {
        return allowDecimals ? this.price : (int) this.price;
    }

    /**
     * Returns the optional delay (in ticks) if set, otherwise empty.
     */
    public OptionalInt getDelay() {
        return this.delay > 0 ? OptionalInt.of(this.delay) : OptionalInt.empty();
    }

    // ============================================================
    // Payment Logic
    // ============================================================

    /**
     * Handles payment for using the PaySign.
     *
     * @param player the player who pays
     * @param messageRenderer message helper for localized text
     * @param economy the Vault economy provider
     * @param allowDecimals whether decimals in price are allowed
     * @return true if payment was successful, false otherwise
     */
    public boolean pay(Player player, MessageRenderer messageRenderer, Economy economy, boolean allowDecimals) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(messageRenderer, "messageRenderer cannot be null");
        Objects.requireNonNull(economy, "economy cannot be null");

        String worldName = player.getWorld().getName();
        double price = this.getPrice(allowDecimals);

        // Free sign
        if (Double.compare(price, 0) == 0) {
            logger.finer("The sign is free of charge.");
            return true;
        }

        // Check if player can afford
        if (!economy.has(player, worldName, price)) {
            logger.fine("The player is too poor to use this sign.");
            player.sendMessage(messageRenderer.tooPoor());
            return false;
        }

        // Withdraw from player
        EconomyResponse withdraw = economy.withdrawPlayer(player, worldName, price);
        if (!withdraw.transactionSuccess()) {
            logger.fine("Could not withdraw player balance.");
            player.sendMessage(messageRenderer.error(withdraw.errorMessage));
            return false;
        }

        // Deposit to owner
        EconomyResponse deposit = economy.depositPlayer(this.playerName, worldName, price);
        if (!deposit.transactionSuccess()) {
            logger.warning("Could not deposit " + this.playerName +
                           " for PaySign at " + this.sign.getLocation());
            economy.depositPlayer(player, worldName, price); // rollback
            player.sendMessage(messageRenderer.cantDeposit());
            return false;
        }

        // Notify payer
        String formattedPrice = economy.format(withdraw.amount);
        player.sendMessage(messageRenderer.paid(formattedPrice, this.playerName));
        logger.info(player.getName() + " has paid " + formattedPrice +
                    " for using " + this.playerName + "'s mechanism.");

        // Notify owner (if online)
        this.getOwner(player.getServer()).ifPresent(owner -> {
            owner.sendMessage(messageRenderer.notification(player.getName(), formattedPrice));
        });

        return true;
    }

    // ============================================================
    // Sign Utility Methods
    // ============================================================

    /**
     * Gets the facing direction of the sign.
     */
    public BlockFace getFacing() {
        BlockData blockData = this.sign.getBlock().getBlockData();
        Material material = blockData.getMaterial();

        if (Tag.STANDING_SIGNS.isTagged(material)) {
            return BlockFace.UP; // freestanding sign
        } else if (Tag.WALL_SIGNS.isTagged(material) && blockData instanceof Directional) {
            return ((Directional) blockData).getFacing(); // wall-mounted
        } else {
            throw new IllegalStateException("Invalid block material: " + material);
        }
    }

    /**
     * Gets the base block to which the sign is attached.
     */
    public Block getBaseBlock() {
        return this.sign.getBlock().getRelative(this.getFacing().getOppositeFace());
    }
}
