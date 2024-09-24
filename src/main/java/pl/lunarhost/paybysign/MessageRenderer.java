package pl.lunarhost.paybysign;

import org.bukkit.ChatColor;

/**
 * Renders different messages.
 */
@FunctionalInterface
public interface MessageRenderer {
    String prefixed(String text);

    default String cantDeposit() {
        return error("Could not deposit target player.");
    }

    default String createdSuccessfully() {
        return success("Tabliczka została stworzona.");
    }

    default String disabledDecimals() {
        return error("Ceny dziesiętne nie są dozwolone na tym serwerze.");
    }

    default String noPermissionToCreate() {
        return error("Nie masz permisji do utworzenia tej tabliczki.");
    }

    default String noPermissionToCreateOther() {
        return error("Nie masz permisji, aby utworzyć tą tabliczkę dla innych graczy.");
    }

    default String noPermissionToUse() {
        return error("Nie masz permisji na używanie tej tabliczki.");
    }

    default String notification(String playerName, String formattedPrice) {
        return fine(playerName + " zapłacił " + formattedPrice + " używając twojej tabliczki.");
    }

    default String paid(String formattedPrice, String ownerName) {
        return success(formattedPrice + " zostało pobrane z Twojego konta w celu korzystania z tabliczki " + ownerName);
    }

    default String tooPoor() {
        return error("Jesteś zbyt biedny, aby używać tej tabliczki.");
    }

    //
    // Formatters
    //

    default String error(String text) {
        return colored(text, ChatColor.RED);
    }

    default String success(String text) {
        return colored(text, ChatColor.GREEN);
    }

    default String fine(String text) {
        return colored(text, ChatColor.GRAY);
    }

    default String colored(String text, ChatColor color) {
        return prefixed(color.toString() + text);
    }
}
