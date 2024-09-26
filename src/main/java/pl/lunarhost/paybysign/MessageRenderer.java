package pl.lunarhost.paybysign;

import org.bukkit.ChatColor;

@FunctionalInterface
public interface MessageRenderer {

    String prefixed(String text); // Definicja metody prefixed

    // Formatowanie wiadomości z prefiksem i sufiksem
    default String formatMessage(String text) {
        String messagePrefixAndSuffix = "&8«&6*&8»&8&m-------------------&8«&6*&8»&2 PyBySign &8«&6*&8»&8&m-------------------&8«&6*&8»";
        return ChatColor.translateAlternateColorCodes('&', messagePrefixAndSuffix + "\n" + text + "\n" + messagePrefixAndSuffix);
    }

    // Metody generujące wiadomości z użyciem formatMessage

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
        return error("Nie masz permisji, aby utworzyć tę tabliczkę dla innych graczy.");
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
    // Formatters for different types of messages
    //

    default String error(String text) {
        return formatMessage(ChatColor.RED + text);
    }

    default String success(String text) {
        return formatMessage(ChatColor.GREEN + text);
    }

    default String fine(String text) {
        return formatMessage(ChatColor.GRAY + text);
    }
}
