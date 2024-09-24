package pl.lunarhost.paybysign;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
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

public final class PayBySignPlugin extends JavaPlugin implements Listener {
    private static final Logger logger = Logger.getLogger(PayBySignPlugin.class.getName());

    private static final String PERMISSION_CREATE = "paybysign.create";
    private static final String PERMISSION_CREATE_OTHER = PERMISSION_CREATE + ".other";
    private static final String PERMISSION_USE = "paybysign.use";

    private final Deque<Trigger> activeTriggers = new ArrayDeque<>(512);

    private Configuration configuration;
    private MessageRenderer messageRenderer;
    private SignDataParser signDataParser;
    private Economy economy;
    private LogBlockHook logBlockHook;

    @Override
    public void onEnable() {
        setupPlugin();
        setupEconomy();
        setupLogBlockHook();
    }

    @Override
    public void onDisable() {
        clearTriggers();
    }

    private void setupPlugin() {
        saveDefaultConfig();
        configuration = new Configuration(this::getConfig);
        messageRenderer = text -> ChatColor.GOLD + ChatColor.ITALIC.toString() + "[" + getName() + "] " + ChatColor.RESET + text;
        signDataParser = new SignDataParser();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(this, this);
    }

    private void setupEconomy() {
        getServer().getScheduler().runTask(this, () -> {
            RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
            if (economyProvider != null) {
                economy = economyProvider.getProvider();
                logger.info("Hooked into economy plugin: " + economyProvider.getPlugin().getDescription().getFullName());
            } else {
                logger.severe("No economy plugin found. Disabling plugin.");
                setEnabled(false);
            }
        });
    }

    private void setupLogBlockHook() {
        if (getServer().getPluginManager().getPlugin("LogBlock") != null) {
            logBlockHook = new LogBlockHook();
            logger.info("LogBlock hook enabled.");
        }
    }

    private void clearTriggers() {
        activeTriggers.forEach(Trigger::flush);
        activeTriggers.clear();
        economy = null;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isInvalidInteraction(event)) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        Sign sign = extractSign(clickedBlock);
        if (sign == null) return;

        PayBySign payBySign = parsePayBySign(sign);
        if (payBySign == null) return;

        handlePayBySignInteraction(event, player, sign, payBySign);
    }

    private boolean isInvalidInteraction(PlayerInteractEvent event) {
        return !event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getPlayer().isSneaking() || event.getClickedBlock() == null || !Tag.SIGNS.isTagged(event.getClickedBlock().getType());
    }

    private Sign extractSign(Block block) {
        BlockState state = block.getState();
        return (state instanceof Sign) ? (Sign) state : null;
    }

    private PayBySign parsePayBySign(Sign sign) {
        try {
            Optional<PayBySign> payBySignMaybe = signDataParser.parse(sign);
            return payBySignMaybe.orElse(null);
        } catch (SignDataParser.ParseException e) {
            logger.fine("Could not parse sign data.");
            return null;
        }
    }

    private void handlePayBySignInteraction(PlayerInteractEvent event, Player player, Sign sign, PayBySign payBySign) {
        event.setUseItemInHand(Event.Result.DENY);

        if (!player.hasPermission(PERMISSION_USE)) {
            player.sendMessage(messageRenderer.noPermissionToUse());
            return;
        }

        if (!payBySign.pay(player, messageRenderer, economy, configuration.allowDecimals())) return;

        executePayBySignTrigger(player, sign, payBySign);
    }

    private void executePayBySignTrigger(Player player, Sign sign, PayBySign payBySign) {
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.runTask(this, () -> {
            Trigger trigger = new Trigger(this, payBySign);
            activeTriggers.addLast(trigger);
            Switch fakeButton = trigger.execute();
            if (logBlockHook != null) {
                logBlockHook.logClick(player, trigger, fakeButton);
            }

            scheduler.runTaskLater(this, () -> {
                try {
                    trigger.flush();
                } finally {
                    activeTriggers.remove(trigger);
                }
            }, payBySign.getDelay().orElse(configuration.delay()));
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSign(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Sign sign = extractSign(block);
        if (sign == null) return;

        PayBySign payBySign = parsePayBySignFromEvent(event, sign);
        if (payBySign == null) return;

        if (!player.hasPermission(PERMISSION_CREATE)) {
            cancel(event, messageRenderer.noPermissionToCreate());
            return;
        }

        if (!payBySign.getPlayerName().equalsIgnoreCase(player.getName()) && !player.hasPermission(PERMISSION_CREATE_OTHER)) {
            cancel(event, messageRenderer.noPermissionToCreateOther());
            return;
        }

        if (!configuration.allowDecimals() && payBySign.getPrice() != payBySign.getPrice(false)) {
            cancel(event, messageRenderer.disabledDecimals());
            return;
        }

        logger.info(player.getName() + " is creating a new PayBySign at " + sign.getLocation());
        event.setLine(0, PayBySign.NAMESPACE_COLOR + PayBySign.NAMESPACE);
        player.sendMessage(messageRenderer.createdSuccessfully());
    }

    private PayBySign parsePayBySignFromEvent(SignChangeEvent event, Sign sign) {
        try {
            Optional<PayBySign> payBySignMaybe = signDataParser.parse(sign, event.getLines());
            return payBySignMaybe.orElse(null);
        } catch (SignDataParser.ParseException e) {
            cancel(event, messageRenderer.error(e.getText()));
            return null;
        }
    }

    private void cancel(SignChangeEvent event, String reason) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(reason, "reason");

        event.setCancelled(true);
        event.getBlock().breakNaturally();
        event.getPlayer().sendMessage(reason);
    }
}
