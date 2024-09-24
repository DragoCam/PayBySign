package pl.lunarhost.paybysign;

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

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Handles triggering for PayBySign blocks.
 */
public class Trigger implements Listener, Predicate<Block> {
    private static final Logger LOGGER = Logger.getLogger(Trigger.class.getName());
    private static final Supplier<Switch> BUTTON_SUPPLIER = () -> (Switch) Material.OAK_BUTTON.createBlockData();
    private static final BlockFace DEFAULT_FLOOR_FACING = BlockFace.NORTH;
    private static final Sound BUTTON_SOUND_ON = Sound.BLOCK_WOODEN_BUTTON_CLICK_ON;
    private static final Sound BUTTON_SOUND_OFF = Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF;
    private static final float SOUND_VOLUME = 0.3F;

    private final Plugin plugin;
    private final PayBySign payBySign;
    private Block baseBlock;

    public Trigger(Plugin plugin, PayBySign payBySign) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.payBySign = Objects.requireNonNull(payBySign, "payBySign");
    }

    @Override
    public boolean test(Block block) {
        return block.equals(this.payBySign.getSign().getBlock()) || 
               (this.baseBlock != null && block.equals(this.baseBlock));
    }

    public PayBySign getPayBySign() {
        return this.payBySign;
    }

    public Switch execute() {
        LOGGER.finer("Registering events for trigger.");
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        this.baseBlock = this.payBySign.getBaseBlock();

        Switch button = createFakeButton();
        this.payBySign.getSign().getBlock().setBlockData(button);
        playSound(BUTTON_SOUND_ON, 0.6F);

        updateBaseBlockNeighbors();
        return button;
    }

    private Switch createFakeButton() {
        LOGGER.fine("Creating fake button.");
        BlockFace facing = this.payBySign.getFacing();
        Switch button = BUTTON_SUPPLIER.get();

        button.setFace(facing.equals(BlockFace.UP) ? Switch.Face.FLOOR : Switch.Face.WALL);
        button.setFacing(facing.equals(BlockFace.UP) ? DEFAULT_FLOOR_FACING : facing);
        button.setPowered(true);
        return button;
    }

    private void playSound(Sound sound, float pitch) {
        Sign sign = this.payBySign.getSign();
        sign.getWorld().playSound(sign.getLocation(), sound, SoundCategory.BLOCKS, SOUND_VOLUME, pitch);
    }

    private void updateBaseBlockNeighbors() {
        BlockData originalData = this.baseBlock.getBlockData();
        Material temporaryMaterial = originalData.getMaterial().equals(Material.BARRIER)
                ? Material.STONE : Material.BARRIER;

        // Simulate block change to update physics
        this.baseBlock.setBlockData(temporaryMaterial.createBlockData(), false);
        this.baseBlock.setBlockData(originalData, true);
    }

    public void flush() {
        LOGGER.fine("Restoring fake button.");
        try {
            this.payBySign.getSign().update(true, true);
            playSound(BUTTON_SOUND_OFF, 0.5F);
            updateBaseBlockNeighbors();
        } finally {
            HandlerList.unregisterAll(this);
            this.baseBlock = null;
        }
    }

    //
    // Event Listeners
    //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockBreak(BlockBreakEvent event) {
        if (test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockBurn(BlockBurnEvent event) {
        if (test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelBlockFade(BlockFadeEvent event) {
        if (test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelEntityChangeBlock(EntityChangeBlockEvent event) {
        if (test(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelPhysics(BlockPhysicsEvent event) {
        if (test(event.getBlock())) {
            event.setCancelled(true);
        }
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