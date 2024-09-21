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

import java.util.Objects;
import java.util.function.Supplier;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * General plugin configuration. Uses {@link Supplier} for the source file to
 * make it reloadable.
 */
public class Configuration {
    private static final int DEFAULT_DELAY = 30; // ticks, same as #wooden_buttons
    private static final boolean DEFAULT_ALLOW_DECIMALS = true;

    private final Supplier<FileConfiguration> config;

    public Configuration(Supplier<FileConfiguration> config) {
        this.config = Objects.requireNonNull(config, "configuration");
    }

    public FileConfiguration getConfig() {
        return Objects.requireNonNull(this.config.get());
    }

    public int delay() {
        return this.getConfig().getInt("delay", DEFAULT_DELAY);
    }

    public boolean allowDecimals() {
        return this.getConfig().getBoolean("allow-decimals", DEFAULT_ALLOW_DECIMALS);
    }
}
