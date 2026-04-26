package by.bratukhin.shortener.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;

import io.seruco.encoding.base62.Base62;

///
/// Test for [ShortCodeEncoder].
///
class ShortCodeEncoderTest {

    private final Base62 base62 = Base62.createInstance();

    private final ShortCodeEncoder encoder = new ShortCodeEncoder();

    @Test
    void shouldReturnFixedLengthCode() {
        String code = encoder.encode(UUID.fromString("019dc88c-7c40-7051-8dee-98dc94379815"));

        assertThat(code)
            .isNotNull()
            .hasSize(ShortCodeEncoder.SHORT_CODE_LENGTH);
    }

    @Test
    void shouldContainOnlyBase62Characters() {
        UUID uuid = UUID.fromString("019dc88d-59d5-737d-854f-b996f76c5d0b");

        String code = encoder.encode(uuid);

        assertThat(base62.isBase62Encoding(code.getBytes(StandardCharsets.UTF_8)))
            .isTrue();
    }

    @Test
    void shouldBeDeterministic() {
        UUID uuid = UUID.fromString("0194a3c5-8f2a-7d00-8000-0123456789ab");

        String code1 = encoder.encode(uuid);
        String code2 = encoder.encode(uuid);

        assertThat(code1).isEqualTo(code2);
    }

    @Test
    void shouldPadWithZeroOnLeft() {
        UUID smallUuid = new UUID(0L, 1L);

        String code = encoder.encode(smallUuid);

        assertThat(code).isEqualTo("0000000000000000000001");
    }

    @Test
    void shouldHandleZeroUuid() {
        UUID zeroUuid = new UUID(0L, 0L);

        String code = encoder.encode(zeroUuid);

        assertThat(code)
            .isEqualTo(StringUtils.repeat('0', ShortCodeEncoder.SHORT_CODE_LENGTH));
    }

    @Test
    void shouldHandleMaxUuid() {
        UUID maxUuid = new UUID(-1L, -1L);

        String code = encoder.encode(maxUuid);

        assertThat(code)
            .hasSize(ShortCodeEncoder.SHORT_CODE_LENGTH)
            .doesNotStartWith("0");
    }

    @ParameterizedTest
    @MethodSource("provideUuidsForOrderTest")
    void shouldMaintainOrderForSequentialUuids(UUID uuid1, UUID uuid2) {
        String code1 = encoder.encode(uuid1);
        String code2 = encoder.encode(uuid2);

        assertThat(code1).isLessThan(code2);
    }

    private static Stream<Arguments> provideUuidsForOrderTest() {
        return Stream.of(
            Arguments.of(
                new UUID(0x0194A3C58F2A7D00L, 0x80000123456789ABL),
                new UUID(0x0194A3C58F2A7D01L, 0x80000123456789ABL)
            ),
            Arguments.of(
                new UUID(0x0194A3C58F2A7D00L, 0x0000000000000001L),
                new UUID(0x0194A3C58F2A7D00L, 0x0000000000000002L)
            )
        );
    }
}
