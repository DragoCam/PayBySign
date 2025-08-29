package net.nightzy.paysign;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

/**
 * This class represents a trigger mechanism for {@link PaySign}s.
 * It simulates a button press by replacing the sign block data temporarily.
 * The trigger also listens to events that could destroy or modify the fake block
 * and cancels them to protect the PaySign.
 */
public class Trigger implements Listener, Predicate<Block> {

    // Logger for debug information
    private static final Logger logger = Logger.getLogger(Trigger.class.getName());

    // Factory that creates a default wooden button BlockData
    private static final Supplier<Switch> BUTTON_FACTORY = () -> (Switch) Material.OAK_BUTTON.createBlockData();

    // Default facing for buttons placed on the ground
    private static final BlockFace FLOOR_FACING = BlockFace.NORTH;

    // Sounds for fake button being pressed/released
    private static final Sound SOUND_ON = Sound.BLOCK_WOODEN_BUTTON_CLICK_ON;
    private static final Sound SOUND_OFF = Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF;
    private static final float SOUND_VOLUME = 0.3F;

    private final Plugin plugin;
    private final PaySign paySign;
    private Block baseBlock;

    public Trigger(Plugin plugin, PaySign paySign) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.paySign = Objects.requireNonNull(paySign, "paySign cannot be null");
    }

    /**
     * Test if the given block is related to this PaySign trigger.
     * @param block Block to check
     * @return true if the block is the sign or its base block
     */
    @Override
    public boolean test(Block block) {
        if (paySign.getSign().getBlock().equals(block)) {
            return true;
        }
        return baseBlock != null && baseBlock.equals(block);
    }

    /**
     * @return the PaySign this trigger belongs to
     */
    public PaySign getPaySign() {
        return this.paySign;
    }

    /**
     * Executes the trigger: registers event listeners, creates a fake powered button
     * on the sign block, plays the activation sound, and updates neighbor physics.
     * @return the created fake button block data
     */
    public Switch execute() {
        logger.finer("Registering events and executing trigger.");

        // Register this class as an event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Store the base block under the sign
        baseBlock = paySign.getBaseBlock();

        // Replace the sign temporarily with a fake button
        Switch button = createFakeButton();
        paySign.getSign().getBlock().setBlockData(button);

        // Play "button pressed" sound
        playSound(SOUND_ON, 0.6F);

        // Trigger neighbor updates for physics
        updateBaseBlockNeighbors();
        return button;
    }

    /**
     * Creates a fake powered button block data with the correct facing.
     * @return powered button block data
     */
    public Switch createFakeButton() {
        logger.fine("Creating fake button block data.");

        BlockFace facing = paySign.getFacing();
        Switch button = BUTTON_FACTORY.get();

        // Determine button face (wall or floor)
        button.setFace(facing.equals(BlockFace.UP) ? Switch.Face.FLOOR : Switch.Face.WALL);
        button.setFacing(facing.equals(BlockFace.UP) ? FLOOR_FACING : facing);

        // Make button powered (pressed state)
        button.setPowered(true);
        return button;
    }

    /**
     * Plays a sound at the sign's location.
     * @param sound sound type
     * @param pitch pitch variation
     */
    private void playSound(Sound sound, float pitch) {
        Objects.requireNonNull(sound, "sound cannot be null");
        Sign sign = paySign.getSign();
        sign.getWorld().playSound(sign.getLocation(), sound, SoundCategory.BLOCKS, SOUND_VOLUME, pitch);
    }

    /**
     * Forces Minecraft to re-check block neighbors by temporarily replacing
     * the base block with another block type and restoring it.
     */
    private void updateBaseBlockNeighbors() {
        BlockData realBlockData = baseBlock.getBlockData();

        // Choose a dummy material to trigger physics
        Material dummy = realBlockData.getMaterial().equals(Material.BARRIER)
                ? Material.STONE : Material.BARRIER;

        // Set dummy block (no physics), then restore real block (with physics)
        baseBlock.setBlockData(dummy.createBlockData(), false);
        baseBlock.setBlockData(realBlockData, true);
    }

    /**
     * Restores the sign, plays the deactivation sound, and unregisters this listener.
     */
    public void flush() {
        logger.fine("Flushing trigger: restoring sign and unregistering listener.");
        try {
            // Restore the original sign
            paySign.getSign().update(true, true);

            // Play "button released" sound
            playSound(SOUND_OFF, 0.5F);

            // Update block physics again
            updateBaseBlockNeighbors();
        } finally {
            HandlerList.unregisterAll(this);
            baseBlock = null;
        }
    }

    // ========================================================================
    // Event Handlers – These protect the fake button & sign from being destroyed
    // ========================================================================

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockBreak(BlockBreakEvent event) {
        if (test(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockBurn(BlockBurnEvent event) {
        if (test(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockFade(BlockFadeEvent event) {
        if (test(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelEntityChangeBlock(EntityChangeBlockEvent event) {
        if (test(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelPhysics(BlockPhysicsEvent event) {
        if (test(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (test(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (test(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }
}
