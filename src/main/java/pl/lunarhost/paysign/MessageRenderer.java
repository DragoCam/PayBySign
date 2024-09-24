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

import org.bukkit.ChatColor;

/**
 * Renders different messages.
 */
public abstract class MessageRenderer {
    public String cantDeposit() {
        return this.error("Could not deposit target player.");
    }

    public String createdSuccessfully() {
        return this.success("Tabliczka została stworzona.");
    }

    public String disabledDecimals() {
        return this.error("Ceny dziesiętne nie są dozwolone na tym serwerze.");
    }

    public String noPermissionToCreate() {
        return this.error("Nie masz permisji do utworzenia tej tabliczki.");
    }

    public String noPermissionToCreateOther() {
        return this.error("Nie masz permisji, aby utworzyć tą tabliczkę dla innych graczy.");
    }

    public String noPermissionToUse() {
        return this.error("Nie masz permisji na używanie tej tabliczki.");
    }

    public String notification(String playerName, String formattedPrice) {
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(formattedPrice, "formattedPrice");
        return this.fine(playerName + " zapłacił " + formattedPrice + " używając twojej tabliczki.");
    }

    public String paid(String formattedPrice, String ownerName) {
        Objects.requireNonNull(formattedPrice, "formattedPrice");
        Objects.requireNonNull(ownerName, "ownerName");
        return this.success(formattedPrice + " zostało pobrane z Twojego konta w celu korzystania z tabliczki " + ownerName);
    }

    public String tooPoor() {
        return this.error("Jesteś zbyt biedny, aby używać tej tabliczki.");
    }

    //
    // Formatters
    //

    public String error(String text) {
        return this.colored(text, ChatColor.RED);
    }

    private String success(String text) {
        return this.colored(text, ChatColor.GREEN);
    }

    private String fine(String text) {
        return this.colored(text, ChatColor.GRAY);
    }

    private String colored(String text, ChatColor color) {
        Objects.requireNonNull(color, "color");
        return this.prefixed(color.toString() + text);
    }

    public abstract String prefixed(String text);
}
