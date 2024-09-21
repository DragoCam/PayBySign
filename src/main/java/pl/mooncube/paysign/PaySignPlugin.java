/*
 * Copyright 2024 Klaudiusz Wojtyczka <drago.klaudiusz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.mooncube.paysign;

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
 * PaySign plugin main class.
 */
public final class PaySignPlugin extends JavaPlugin implements Listener {
    static final Logger logger = Logger.getLogger(PaySignPlugin.class.getName());

    private static final String PERMISSION_CREATE = "mooncube.create";
    private static final String PERMISSION_CREATE_OTHER = PERMISSION_CREATE + ".other";
    private static final String PERMISSION_USE = "mooncube.use";

    private final Deque<Trigger> activeTriggers = new ArrayDeque<>(512);

    private Configuration configuration;
    private MessageRenderer messageRenderer;
    private SignDataParser signDataParser;
    private Economy economy;

    private LogBlockHook logBlockHook;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        Server server = this.getServer();
        PluginManager pluginManager = server.getPluginManager();
        BukkitScheduler scheduler = server.getScheduler();

        this.configuration = new Configuration(this::getConfig);
        this.messageRenderer = new MessageRenderer() {
            @Override
            public String prefixed(String text) {
                return ChatColor.GOLD + ChatColor.ITALIC.toString() + "[" + getName() + "] " + ChatColor.RESET + text;
            }
        };
        this.signDataParser = new SignDataParser();

        pluginManager.registerEvents(this, this);

        scheduler.runTask(this, () -> {
            logger.fine("Resolving Economy service provider...");
            RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(Economy.class);

            if (economyProvider != null) {
                String pluginName = economyProvider.getPlugin().getDescription().getFullName();
                Economy provider = economyProvider.getProvider();

                logger.info("Hooked economy into " + pluginName + ": " + provider.getClass().getName());
                this.economy = provider;
            } else {
                logger.severe("Economy service isn't provided. Please install an economy plugin.");
                this.setEnabled(false);
            }
        });

        if (pluginManager.getPlugin("LogBlock") != null) {
            logger.info("Enabling LogBlock hook...");
            this.logBlockHook = new LogBlockHook();
        }

    }

    @Override
    public void onDisable() {

        this.activeTriggers.forEach(Trigger::flush);
        this.activeTriggers.clear();
        this.economy = null;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isSneaking()) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (!Tag.SIGNS.isTagged(clickedBlock.getType())) {
            return;
        }

        BlockState state = clickedBlock.getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;

        PaySign paySign;
        try {
            Optional<PaySign> paySignMaybe = this.signDataParser.parse(sign);
            if (!paySignMaybe.isPresent()) {
                return;
            }
            paySign = paySignMaybe.get();
        } catch (SignDataParser.ParseException ignored) {
            logger.fine("Could not parse target sign data.");
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);

        if (!player.hasPermission(PERMISSION_USE)) {
            logger.fine("The player is not permitted to use this sign.");
            player.sendMessage(this.messageRenderer.noPermissionToUse());
            return;
        }

        if (!paySign.pay(player, this.messageRenderer, this.economy, this.configuration.allowDecimals())) {
            return;
        }

        BukkitScheduler scheduler = this.getServer().getScheduler();
        logger.info(player.getName() + " is triggering PaySign sign at " + sign.getLocation());

        // Execute in next tick so that PlayerInteractEvent is handled properly
        scheduler.runTask(this, () -> {
            Trigger trigger = new Trigger(this, paySign);
            this.activeTriggers.addLast(trigger);

            Switch fakeButton = trigger.execute();
            if (this.logBlockHook != null) {
                this.logBlockHook.logClick(player, trigger, fakeButton);
            }

            scheduler.runTaskLater(this, () -> {
                try {
                    trigger.flush();
                } finally {
                    this.activeTriggers.remove(trigger);
                }
            }, paySign.getDelay().orElse(this.configuration.delay()));
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSign(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        BlockState state = block.getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;

        PaySign paySign;
        try {
            Optional<PaySign> paySignMaybe = this.signDataParser.parse(sign, event.getLines());
            if (!paySignMaybe.isPresent()) {
                // not PaySign sign
                return;
            }
            paySign = paySignMaybe.get();
        } catch (SignDataParser.ParseException e) {
            logger.fine("Could not parse target sign data.");
            this.cancel(event, player.hasPermission(PERMISSION_CREATE)
                    ? this.messageRenderer.error(e.getText())
                    : this.messageRenderer.noPermissionToCreate());
            return;
        }

        if (!player.hasPermission(PERMISSION_CREATE)) {
            logger.fine("The player is not permitted to create the sign.");
            this.cancel(event, this.messageRenderer.noPermissionToCreate());
            return;
        }

        if (!paySign.getPlayerName().equalsIgnoreCase(player.getName()) && !player.hasPermission(PERMISSION_CREATE_OTHER)) {
            logger.fine("The player is not permitted to create the sign for other players.");
            this.cancel(event, this.messageRenderer.noPermissionToCreateOther());
            return;
        }

        if (!this.configuration.allowDecimals() && paySign.getPrice() != paySign.getPrice(false)) {
            logger.fine("Decimal prices aren't enabled on this server.");
            this.cancel(event, this.messageRenderer.disabledDecimals());
            return;
        }

        logger.info(player.getName() + " is creating a new PaySign sign at " + sign.getLocation());
        event.setLine(0, PaySign.NAMESPACE_COLOR + PaySign.NAMESPACE);
        player.sendMessage(this.messageRenderer.createdSuccessfully());
    }

    private void cancel(SignChangeEvent event, String reason) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(reason, "reason");

        event.setCancelled(true);
        event.getBlock().breakNaturally();
        event.getPlayer().sendMessage(reason);
    }
}
