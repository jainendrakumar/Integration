import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jainendra Kumar
 * TODO:
 */

public class LogAnalyzerUtil {

    /**
     * Tries to parse the input string into a LocalDateTime.
     * It first attempts to parse a full ISO date–time.
     * Then it attempts to parse as OffsetTime or LocalTime.
     * If the string is purely numeric and less than 86,400,
     * it is treated as seconds from midnight on the current day.
     */
    public static LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Try full ISO date-time (e.g. 2025-02-14T06:13:42+05:30)
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            // Continue to try other formats.
        }

        // Try parsing as OffsetTime with pattern "HH:mm:ssXXX"
        try {
            OffsetTime offsetTime = OffsetTime.parse(dateStr, DateTimeFormatter.ofPattern("HH:mm:ssXXX"));
            return LocalDate.now().atTime(offsetTime.toLocalTime());
        } catch (DateTimeParseException e) {
            // Continue to try next format.
        }

        // Try parsing as plain LocalTime with pattern "HH:mm:ss"
        try {
            LocalTime localTime = LocalTime.parse(dateStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            return LocalDate.now().atTime(localTime);
        } catch (DateTimeParseException e) {
            // Continue to try numeric parsing.
        }

        // Try parsing a numeric string, e.g., "38387"
        try {
            double seconds = Double.parseDouble(dateStr);
            // If the number is less than a day's worth of seconds, treat it as seconds from midnight.
            if (seconds < 86400) {
                return LocalDate.now().atStartOfDay().plusSeconds((long) seconds);
            }
            // Otherwise, you might choose to treat it as epoch seconds.
            // return LocalDateTime.ofEpochSecond((long) seconds, 0, ZoneOffset.UTC);
        } catch (NumberFormatException e) {
            // Not a numeric value.
        }

        throw new RuntimeException("Unable to parse date/time: " + dateStr);
    }
}
