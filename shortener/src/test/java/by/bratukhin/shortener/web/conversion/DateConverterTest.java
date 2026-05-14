package by.bratukhin.shortener.web.conversion;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

///
/// Test for [DateConverter].
///
class DateConverterTest {

    @Test
    void convert() {
        OffsetDateTime converted = DateConverter.convert(Instant.parse("2026-05-14T10:15:30.00Z"));

        assertThat(converted)
            .isNotNull()
            .isEqualTo("2026-05-14T10:15:30.00Z");
    }
}
