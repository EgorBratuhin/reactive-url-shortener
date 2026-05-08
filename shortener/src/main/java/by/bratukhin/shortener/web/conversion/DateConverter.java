package by.bratukhin.shortener.web.conversion;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

///
/// Utility class for date and time conversions.
///
public final class DateConverter {

    ///
    /// Converts the given [Instant] to [OffsetDateTime] using the system default time zone.
    ///
    /// @param input the [Instant] to convert; may be null
    /// @return the corresponding [OffsetDateTime], or null if the input is null
    ///
    public static OffsetDateTime convert(Instant input) {
        if (input == null) {
            return null;
        }

        return OffsetDateTime.ofInstant(input, ZoneId.systemDefault());
    }

    private DateConverter() {
        /* This utility class should not be instantiated */
    }

}
