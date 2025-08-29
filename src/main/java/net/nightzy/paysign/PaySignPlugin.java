package net.nightzy.paysign;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import net.milkbowl.vault.economy.Economy;

/**
 * Main class of the PaySign plugin.
 * Handles sign interactions, creation, and integration with Vault economy.
 */
public final class PaySignPlugin extends JavaPlugin implements Listener {

    // Logger for plugin debug/info messages
    static final Logger logger = Logger.getLogger(PaySignPlugin.class.getName());

    // Permissions
    private static final String PERMISSION_CREATE = "nightzypaysign.create";
    private static final String PERMISSION_CREATE_OTHER = PERMISSION_CREATE + ".other";
    private static final String PERMISSION_USE = "nightzypaysign.use";

    // Keeps track of currently active triggers (fake button presses)
    private final Deque<Trigger> activeTriggers = new ArrayDeque<>(512);

    private Configuration configuration;
    private MessageRenderer messageRenderer;
    private SignDataParser signDataParser;
    private Economy economy;

    // ============================================================
    // Plugin lifecycle
    // ============================================================

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        Server server = getServer();
        PluginManager pluginManager = server.getPluginManager();
        BukkitScheduler scheduler = server.getScheduler();

        // Load config and helpers
        this.configuration = new Configuration(this::getConfig);
        this.messageRenderer = new MessageRenderer() {
            @Override
            public String prefixed(String text) {
                return ChatColor.GOLD.toString() + ChatColor.ITALIC + "[" + getName() + "] " + ChatColor.RESET + text;
            }
        };
        this.signDataParser = new SignDataParser();

        // Register event listeners
        pluginManager.registerEvents(this, this);

        // Hook into Vault Economy (in the next tick to avoid init issues)
        scheduler.runTask(this, () -> {
            logger.fine("Resolving Economy service provider...");
            RegisteredServiceProvider<Economy> economyProvider =
                    server.getServicesManager().getRegistration(Economy.class);

            if (economyProvider != null) {
                String pluginName = economyProvider.getPlugin().getDescription().getFullName();
                Economy provider = economyProvider.getProvider();

                logger.info("Hooked into economy plugin " + pluginName + ": " + provider.getClass().getName());
                this.economy = provider;
            } else {
                logger.severe("No economy provider found. Please install an economy plugin with Vault support.");
                this.setEnabled(false);
            }
        });
    }

    @Override
    public void onDisable() {
        // Restore all active triggers
        this.activeTriggers.forEach(Trigger::flush);
        this.activeTriggers.clear();
        this.economy = null;
    }

    // ============================================================
    // Event handlers
    // ============================================================

    /**
     * Handles when a player right-clicks a sign.
     * If the sign is a PaySign, performs payment and triggers redstone signal.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only react to right-click on blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.isSneaking()) return; // ignore sneaking players

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !Tag.SIGNS.isTagged(clickedBlock.getType())) return;

        // Verify block state is actually a Sign
        BlockState state = clickedBlock.getState();
        if (!(state instanceof Sign)) return;
        Sign sign = (Sign) state;

        // Try parsing sign into a PaySign
        PaySign paySign;
        try {
            Optional<PaySign> paySignMaybe = this.signDataParser.parse(sign);
            if (!paySignMaybe.isPresent()) return;
            paySign = paySignMaybe.get();
        } catch (SignDataParser.ParseException ignored) {
            logger.fine("Could not parse clicked sign data.");
            return;
        }

        // Prevent item use (so it doesn't overlap with sign)
        event.setUseItemInHand(Event.Result.DENY);

        // Check permissions
        if (!player.hasPermission(PERMISSION_USE)) {
            logger.fine("Player is not permitted to use PaySign.");
            player.sendMessage(this.messageRenderer.noPermissionToUse());
            return;
        }

        // Perform the payment
        if (!paySign.pay(player, this.messageRenderer, this.economy, this.configuration.allowDecimals())) {
            return; // payment failed
        }

        BukkitScheduler scheduler = getServer().getScheduler();
        logger.info(player.getName() + " triggered PaySign at " + sign.getLocation());

        // Run trigger in next tick (to avoid interfering with interact event)
        scheduler.runTask(this, () -> {
            Trigger trigger = new Trigger(this, paySign);
            this.activeTriggers.addLast(trigger);

            Switch fakeButton = trigger.execute();

            // Schedule flush (reset) after delay
            scheduler.runTaskLater(this, () -> {
                try {
                    trigger.flush();
                } finally {
                    this.activeTriggers.remove(trigger);
                }
            }, paySign.getDelay().orElse(this.configuration.delay()));
        });
    }

    /**
     * Handles when a player creates/edits a sign.
     * Validates PaySign format and player permissions.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSign(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Verify it's a sign
        BlockState state = block.getState();
        if (!(state instanceof Sign)) return;
        Sign sign = (Sign) state;

        // Try parsing PaySign data from new sign lines
        PaySign paySign;
        try {
            Optional<PaySign> paySignMaybe = this.signDataParser.parse(sign, event.getLines());
            if (!paySignMaybe.isPresent()) {
                return; // not a PaySign
            }
            paySign = paySignMaybe.get();
        } catch (SignDataParser.ParseException e) {
            logger.fine("Could not parse new sign data.");
            this.cancel(event, player.hasPermission(PERMISSION_CREATE)
                    ? this.messageRenderer.error(e.getText())
                    : this.messageRenderer.noPermissionToCreate());
            return;
        }

        // Check create permissions
        if (!player.hasPermission(PERMISSION_CREATE)) {
            logger.fine("Player is not permitted to create PaySign.");
            this.cancel(event, this.messageRenderer.noPermissionToCreate());
            return;
        }

        // Check if player is allowed to create sign for another player
        if (!paySign.getPlayerName().equalsIgnoreCase(player.getName())
                && !player.hasPermission(PERMISSION_CREATE_OTHER)) {
            logger.fine("Player is not permitted to create PaySign for others.");
            this.cancel(event, this.messageRenderer.noPermissionToCreateOther());
            return;
        }

        // Check if decimals are allowed
        if (!this.configuration.allowDecimals() && paySign.getPrice() != paySign.getPrice(false)) {
            logger.fine("Decimal prices are disabled.");
            this.cancel(event, this.messageRenderer.disabledDecimals());
            return;
        }

        logger.info(player.getName() + " created a PaySign at " + sign.getLocation());
        event.setLine(0, PaySign.NAMESPACE_COLOR + PaySign.NAMESPACE);
        player.sendMessage(this.messageRenderer.createdSuccessfully());
    }

    // ============================================================
    // Helper methods
    // ============================================================

    /**
     * Cancels sign creation and breaks the block.
     */
    private void cancel(SignChangeEvent event, String reason) {
        Objects.requireNonNull(event, "event cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        event.setCancelled(true);
        event.getBlock().breakNaturally();
        event.getPlayer().sendMessage(reason);
    }
}
