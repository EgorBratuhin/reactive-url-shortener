package by.bratukhin.shortener.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import io.seruco.encoding.base62.Base62;

@Component
class ShortCodeEncoder {

    private static final char SHORT_CODE_PADDING_CHAR = '0';

    static final int SHORT_CODE_LENGTH = 22;

    static final int UUID_BYTE_LENGTH = Long.BYTES * 2;

    private final Base62 base62 = Base62.createInstance();

    String encode(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(UUID_BYTE_LENGTH)
            .order(ByteOrder.BIG_ENDIAN);

        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        String shortCode = new String(base62.encode(buffer.array()));

        return StringUtils.leftPad(shortCode, SHORT_CODE_LENGTH, SHORT_CODE_PADDING_CHAR);
    }
}
