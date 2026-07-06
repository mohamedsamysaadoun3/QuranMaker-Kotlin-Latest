package hazem.nurmontage.videoquran.utils

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

/**
 * Formats price strings with proper currency symbol handling and locale-aware
 * number formatting.
 *
 * Handles the following edge cases:
 * - Extracts non-numeric currency symbols (e.g., "$", "EUR", "GBP", "JPY", currency symbols)
 * - Normalizes comma-as-decimal-separator (e.g., "3,50" -> "3.50" for European formats)
 * - Parses the numeric value using US locale [NumberFormat]
 * - Re-formats with grouping separators and up to 2 decimal places
 * - Re-attaches the currency symbol to the formatted number
 *
 * Examples:
 * - `"$3.50"` -> `"$3.50"` (no change needed)
 * - currency+"3,50" -> currency+"3.5"` (comma -> dot, trailing zero removed)
 * - `"$1000"` -> `"$1,000"` (grouping separator added)
 * - `"$1999.99"` -> `"$1,999.99"`
 *
 * If the input cannot be parsed, the original string is returned unchanged.
 *
 * Converted from PriceFormatter.java — logic preserved exactly.
 */
object PriceFormatter {

    /**
     * Format a price string with proper currency symbol and number formatting.
     *
     * Processing steps:
     * 1. Extract the currency symbol (first non-numeric, non-dot, non-comma character)
     * 2. Remove the symbol from the string and trim whitespace
     * 3. If the remaining number uses commas as decimal separators (no dot present),
     *    replace commas with dots
     * 4. Parse the number using US locale [NumberFormat]
     * 5. Re-format with the pattern `#,##0.##` (grouping + up to 2 decimals)
     * 6. Prepend the currency symbol
     *
     * @param price The raw price string (e.g., "$3.50", currency+"3,50")
     * @return The formatted price string, or the original if parsing fails
     */
    fun formatPrice(price: String?): String {
        if (price.isNullOrEmpty()) return ""

        val symbol = extractCurrencySymbol(price)
        var numericPart = price.replace(symbol, "").trim()

        // Handle European-style comma as decimal separator
        if (numericPart.contains(",") && !numericPart.contains(".")) {
            numericPart = numericPart.replace(",", ".")
        }

        try {
            val numberFormat = NumberFormat.getNumberInstance(Locale.US)
            if (numberFormat !is DecimalFormat) return price

            val parsed = numberFormat.parse(numericPart) ?: return price
            val value = BigDecimal(parsed.toString())

            numberFormat.applyPattern("#,##0.##")
            return symbol + numberFormat.format(value)
        } catch (_: NumberFormatException) {
            return price
        } catch (_: ParseException) {
            return price
        }
    }

    /**
     * Extract the currency symbol from a price string.
     *
     * Scans the string character by character and returns the first character
     * that is not a digit, dot, or comma. This handles common currency
     * symbols as well as multi-character symbols that start with a
     * non-numeric character.
     *
     * If no currency symbol is found, returns an empty string.
     *
     * @param price The price string to scan
     * @return The first currency character found, or empty string
     */
    private fun extractCurrencySymbol(price: String): String {
        for (c in price) {
            if (!Character.isDigit(c) && c != '.' && c != ',') {
                return c.toString()
            }
        }
        return ""
    }
}
