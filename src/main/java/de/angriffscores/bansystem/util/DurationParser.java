package de.angriffscores.bansystem.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
public final class DurationParser {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);
    private static final Pattern FULL_PATTERN = Pattern.compile("^(?:\\d+[smhdw])+$", Pattern.CASE_INSENSITIVE);

    /**
     * Parses durations like {@code 30m}, {@code 2h}, {@code 1d} or {@code 1d2h30m}.
     *
     * @param input raw duration string
     * @return parsed duration if valid
     */
    public static @NonNull Optional<Duration> parse(@NonNull String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (!FULL_PATTERN.matcher(normalized).matches()) {
            return Optional.empty();
        }

        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        long totalSeconds = 0L;
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            totalSeconds += switch (unit) {
                case "s" -> amount;
                case "m" -> amount * 60L;
                case "h" -> amount * 3600L;
                case "d" -> amount * 86400L;
                case "w" -> amount * 604800L;
                default -> 0L;
            };
        }
        if (totalSeconds <= 0L) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(totalSeconds));
    }

    /**
     * Formats a duration into a compact human-readable string.
     *
     * @param duration duration to format
     * @return formatted duration
     */
    public static @NonNull String format(@NonNull Duration duration) {
        long totalSeconds = Math.max(duration.getSeconds(), 0L);
        long weeks = totalSeconds / 604800L;
        totalSeconds %= 604800L;
        long days = totalSeconds / 86400L;
        totalSeconds %= 86400L;
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder builder = new StringBuilder();
        if (weeks > 0L) {
            builder.append(weeks).append('w');
        }
        if (days > 0L) {
            builder.append(days).append('d');
        }
        if (hours > 0L) {
            builder.append(hours).append('h');
        }
        if (minutes > 0L) {
            builder.append(minutes).append('m');
        }
        if (seconds > 0L || builder.isEmpty()) {
            builder.append(seconds).append('s');
        }
        return builder.toString();
    }

    /**
     * Formats remaining time until an instant.
     *
     * @param expiresAt expiry instant
     * @return formatted remaining duration
     */
    public static @NonNull String formatRemaining(@NonNull Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            return "0s";
        }
        return format(remaining);
    }
}
