package by.bratukhin.shortener.web.conversion;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

///
/// Конвертер дат.
///
public final class DateConverter {

    ///
    /// Конвертировать [Instant] в [OffsetDateTime].
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
