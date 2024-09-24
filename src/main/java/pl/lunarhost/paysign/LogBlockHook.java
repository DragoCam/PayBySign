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

package pl.lunarhost.paysign;

import java.util.Objects;
import java.util.logging.Logger;

import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

public class LogBlockHook {
    static final Logger logger = Logger.getLogger(LogBlockHook.class.getName());

    /**
     * Logs click action on the sign to the LogBlock {@link Consumer}.
     * @param player Who clicked
     * @param trigger Trigger state
     * @param fakeButton Fake button simulating redstone
     */
    public void logClick(Player player, Trigger trigger, Switch fakeButton) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(fakeButton, "fakeButton");
    }

    private Switch switchOff(Switch button) {
        Objects.requireNonNull(button, "button");
        Switch off = (Switch) button.clone();
        off.setPowered(false);
        return off;
    }
}
