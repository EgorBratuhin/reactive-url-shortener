package by.bratukhin.shortener.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import io.seruco.encoding.base62.Base62;

///
/// Component responsible for encoding UUIDs into compact short codes.
///
/// This encoder converts a UUID into a Base62-encoded string padded to a fixed length
/// of 22 characters. The encoding uses the most significant and least significant bits
/// of the UUID to ensure uniqueness and reversibility.
///
/// Base62 encoding uses characters 0-9, a-z, and A-Z, providing a more compact
/// representation compared to Base64.
///
@Component
class ShortCodeEncoder {

    private static final char SHORT_CODE_PADDING_CHAR = '0';

    static final int SHORT_CODE_LENGTH = 22;

    static final int UUID_BYTE_LENGTH = Long.BYTES * 2;

    private final Base62 base62 = Base62.createInstance();

    ///
    /// Encodes a UUID into a Base62-encoded short code string.
    ///
    /// The UUID is converted to its 16-byte representation, encoded using Base62,
    /// and padded with leading zeros to ensure a consistent length of 22 characters.
    ///
    /// @param uuid the UUID to encode; must not be null
    /// @return a Base62-encoded string of exactly 22 characters
    ///
    String encode(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(UUID_BYTE_LENGTH)
            .order(ByteOrder.BIG_ENDIAN);

        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        String shortCode = new String(base62.encode(buffer.array()));

        return StringUtils.leftPad(shortCode, SHORT_CODE_LENGTH, SHORT_CODE_PADDING_CHAR);
    }
}
