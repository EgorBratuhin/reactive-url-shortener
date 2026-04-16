package by.bratukhin.shortener.service;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.srplib.contract.Argument;

import com.fasterxml.uuid.Generators;

import by.bratukhin.shortener.model.ShortLink;
import by.bratukhin.shortener.repository.ShortLinkRepository;
import by.bratukhin.shortener.support.ItemsPage;
import io.seruco.encoding.base62.Base62;
import reactor.core.publisher.Mono;

///
/// Implementation of [UrlShortenerService].
///
@Service
class UrlShortenerServiceImpl implements UrlShortenerService {

    private static final char SHORT_CODE_PADDING_CHAR = '0';

    private static final int SHORT_CODE_LENGTH = 22;

    public static final int UUID_BYTE_LENGTH = Long.BYTES * 2;

    private final Base62 base62 = Base62.createInstance();

    private final ShortLinkRepository shortLinkRepository;

    private final R2dbcEntityTemplate template;

    UrlShortenerServiceImpl(ShortLinkRepository shortLinkRepository, R2dbcEntityTemplate template) {
        this.shortLinkRepository = shortLinkRepository;
        this.template = template;
    }

    @Override
    @Transactional
    public Mono<ShortLink> create(URI uri, long ttlSeconds) {
        Argument.checkNotNullWithGenericMessage(uri, "uri");

        return Mono.fromCallable(() -> newShortLink(uri, ttlSeconds))
            .flatMap(shortLinkRepository::save);
    }

    private ShortLink newShortLink(URI uri, long ttlSeconds) {
        UUID id = Generators.timeBasedEpochGenerator().generate();

        ShortLink shortLink = new ShortLink();
        shortLink.setId(id);
        shortLink.setNew(true);
        shortLink.setShortCode(encode(id));
        shortLink.setOriginalUrl(uri.toString());
        shortLink.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));

        return shortLink;
    }

    private String encode(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[UUID_BYTE_LENGTH]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        String shortCode = new String(base62.encode(buffer.array()));

        return StringUtils.leftPad(shortCode, SHORT_CODE_LENGTH, SHORT_CODE_PADDING_CHAR);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemsPage<ShortLink>> getShortLinks(String lastShortCode, int pageSize) {
        Criteria criteria = Criteria.empty();
        if (StringUtils.isNotBlank(lastShortCode)) {
            criteria = Criteria.where(ShortLink.Fields.shortCode).lessThan(lastShortCode);
        }

        int limitWithNextPageIndicator = pageSize + 1;

        Query query = Query.query(criteria)
            .limit(limitWithNextPageIndicator)
            .sort(Sort.by(ShortLink.Fields.shortCode).descending());

        return template.select(ShortLink.class)
            .matching(query)
            .all()
            .collectList()
            .map(list -> toItemsPage(pageSize, list));
    }

    private static ItemsPage<ShortLink> toItemsPage(int pageSize, List<ShortLink> list) {
        boolean hasNext = list.size() > pageSize;

        List<ShortLink> data = hasNext ? list.subList(0, pageSize) : list;

        String nextCursor = hasNext ? data.getLast().getShortCode() : null;

        return new ItemsPage<>(data, hasNext, nextCursor);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ShortLink> getUrlMetadataByShortCode(String shortCode) {
        Argument.checkNotNullWithGenericMessage(shortCode, ShortLink.Fields.shortCode);

        return shortLinkRepository.findByShortCode(shortCode)
            .switchIfEmpty(Mono.error(new ObjectNotFoundException("Url metadata not found '%s'".formatted(shortCode))));
    }

    @Override
    @Transactional
    public Mono<Void> deleteByShortCode(String shortCode) {
        Argument.checkNotNullWithGenericMessage(shortCode, ShortLink.Fields.shortCode);

        return shortLinkRepository.deleteByShortCode(shortCode);
    }

}
