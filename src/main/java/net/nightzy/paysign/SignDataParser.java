package net.nightzy.paysign;

import java.util.Objects;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;

import com.google.common.base.Preconditions;

/**
 * Parses a {@link PaySign} from a {@link Sign} or directly from its text lines.
 * Ensures correct format: 
 *   Line 1: Identifier (namespace)
 *   Line 2: Player name
 *   Line 3: Price (numeric, >= 0)
 *   Line 4: Optional delay in seconds (positive integer)
 */
public class SignDataParser {

    /**
     * Parses a PaySign from the given Sign object.
     * @param sign Bukkit Sign
     * @return Optional PaySign if parsing succeeded, otherwise empty
     * @throws ParseException if parsing fails due to invalid input
     */
    public Optional<PaySign> parse(Sign sign) throws ParseException {
        Objects.requireNonNull(sign, "sign cannot be null");
        return parse(sign, sign.getLines());
    }

    /**
     * Parses a PaySign from raw sign text lines.
     * @param sign Bukkit Sign
     * @param lines array of 4 lines of text
     * @return Optional PaySign if parsing succeeded, otherwise empty
     * @throws ParseException if any line has invalid content
     */
    public Optional<PaySign> parse(Sign sign, String[] lines) throws ParseException {
        Objects.requireNonNull(sign, "sign cannot be null");
        Objects.requireNonNull(lines, "lines cannot be null");

        // Ensure exactly 4 lines exist
        Preconditions.checkArgument(lines.length == 4, "Expected 4 lines, but got " + lines.length);

        // ========================
        // Line 1: Identifier
        // ========================
        if (!ChatColor.stripColor(lines[0]).equals(PaySign.NAMESPACE)) {
            return Optional.empty(); // Not a PaySign
        }

        // ========================
        // Line 2: Player name
        // ========================
        String playerName = lines[1];
        if (playerName.isEmpty()) {
            throw new ParseException(1, "No player name given");
        }

        // ========================
        // Line 3: Price
        // ========================
        if (lines[2].isEmpty()) {
            throw new ParseException(2, "No price given");
        }

        double price;
        try {
            price = Double.parseDouble(lines[2]);
        } catch (NumberFormatException e) {
            throw new ParseException(2, "Price is not a valid number", e);
        }

        if (price < 0) {
            throw new ParseException(2, "Price cannot be negative");
        }

        // ========================
        // Line 4: Delay (optional)
        // ========================
        int delay = 0;
        if (!lines[3].isEmpty()) {
            try {
                // Multiply seconds by 20 (Minecraft ticks)
                delay = Integer.parseInt(lines[3]) * 20;
            } catch (NumberFormatException e) {
                throw new ParseException(3, "Redstone delay is not a valid number", e);
            }

            if (delay < 1) {
                throw new ParseException(3, "Redstone delay must be positive");
            }
        }

        // Build and return new PaySign
        return Optional.of(new PaySign(sign, playerName, price, delay));
    }

    /**
     * Custom exception class for parsing errors.
     * Provides the line index and formatted error message.
     */
    public static class ParseException extends Exception {
        private final int line;

        public ParseException(int line, String message) {
            super(message);
            this.line = line;
        }

        public ParseException(int line, String message, Throwable cause) {
            super(message, cause);
            this.line = line;
        }

        /**
         * @return zero-based line index that caused the error
         */
        public int getLine() {
            return this.line;
        }

        /**
         * @return formatted error message including line number and cause
         */
        public String getText() {
            StringBuilder text = new StringBuilder();
            text.append("Line ").append(this.line + 1).append(": ").append(this.getMessage());

            Throwable cause = this.getCause();
            if (cause != null && cause.getMessage() != null) {
                text.append(": ").append(cause.getMessage());
            }

            return text.toString();
        }
    }
}
