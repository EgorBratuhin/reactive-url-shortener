package by.bratukhin.shortener.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.srplib.contract.Argument;

import com.fasterxml.uuid.Generators;

import by.bratukhin.shortener.configuration.ShortLinkConfigurationProperties;
import by.bratukhin.shortener.model.AbstractDataObject_;
import by.bratukhin.shortener.model.ShortLink;
import by.bratukhin.shortener.model.ShortLink_;
import by.bratukhin.shortener.repository.ShortLinkRepository;
import by.bratukhin.shortener.support.ItemsPage;
import io.micrometer.observation.annotation.Observed;
import reactor.core.publisher.Mono;

///
/// Implementation of [UrlShortenerService] that provides URL shortening functionality.
///
@Service
class UrlShortenerServiceImpl implements UrlShortenerService {

    private final ShortLinkConfigurationProperties shortLinkConfigurationProperties;

    private final ShortLinkRepository shortLinkRepository;

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private final ShortCodeEncoder shortCodeEncoder;

    UrlShortenerServiceImpl(ShortLinkConfigurationProperties shortLinkConfigurationProperties,
        ShortLinkRepository shortLinkRepository, R2dbcEntityTemplate r2dbcEntityTemplate,
        ReactiveRedisTemplate<String, String> redisTemplate, ShortCodeEncoder shortCodeEncoder) {

        this.shortLinkConfigurationProperties = shortLinkConfigurationProperties;
        this.shortLinkRepository = shortLinkRepository;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        this.redisTemplate = redisTemplate;
        this.shortCodeEncoder = shortCodeEncoder;
    }

    @Override
    @Transactional
    @Observed(name = "links.created", contextualName = "creating-short-link")
    public Mono<ShortLink> create(URI uri, Integer ttlSeconds, String shortCode) {
        Argument.checkNotNullWithGenericMessage(uri, "uri");

        return Mono.fromCallable(() -> newShortLink(uri, ttlSeconds, shortCode))
            .flatMap(shortLinkRepository::save)
            .onErrorMap(DataIntegrityViolationException.class, e ->
                new DuplicateShortCodeException(shortCode, e))
            .flatMap(this::cache);
    }

    private ShortLink newShortLink(URI uri, Integer ttlSeconds, String shortCode) {
        UUID id = Generators.timeBasedEpochGenerator().generate();

        Instant expiresAt = ttlSeconds != null ?
            Instant.now().plusSeconds(ttlSeconds) :
            null;

        ShortLink shortLink = new ShortLink();
        shortLink.setId(id);
        shortLink.setNew(true);
        shortLink.setShortCode(shortCode != null ? shortCode : shortCodeEncoder.encode(id));
        shortLink.setOriginalUrl(uri.toString());
        shortLink.setExpiresAt(expiresAt);

        return shortLink;
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ItemsPage<ShortLink>> getShortLinks(String nextCursor, int pageSize) {
        Criteria criteria = Criteria.empty();
        if (StringUtils.isNotBlank(nextCursor)) {
            criteria = Criteria.where(AbstractDataObject_.id).lessThan(nextCursor);
        }

        int limitWithNextPageIndicator = pageSize + 1;

        Query query = Query.query(criteria)
            .limit(limitWithNextPageIndicator)
            .sort(Sort.by(AbstractDataObject_.id).descending());

        return r2dbcEntityTemplate.select(ShortLink.class)
            .matching(query)
            .all()
            .collectList()
            .map(list -> toItemsPage(pageSize, list));
    }

    private static ItemsPage<ShortLink> toItemsPage(int pageSize, List<ShortLink> list) {
        boolean hasNext = list.size() > pageSize;

        List<ShortLink> data = hasNext ? list.subList(0, pageSize) : list;

        String nextCursor = hasNext ?
            String.valueOf(data.getLast().getId()) :
            null;

        return new ItemsPage<>(data, hasNext, nextCursor);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ShortLink> getUrlMetadataByShortCode(String shortCode) {
        Argument.checkNotNullWithGenericMessage(shortCode, ShortLink_.shortCode);

        return shortLinkRepository.findByShortCode(shortCode)
            .switchIfEmpty(Mono.error(() -> new ObjectNotFoundException("Url metadata not found '%s'".formatted(shortCode))));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Mono<String> getOriginalUrlByShortCode(String shortCode) {
        Argument.checkNotNullWithGenericMessage(shortCode, ShortLink_.shortCode);

        return redisTemplate.opsForValue().get(shortCode)
            .switchIfEmpty(Mono.defer(() -> shortLinkRepository.findByShortCode(shortCode))
                .filter(ShortLink::isActive)
                .flatMap(this::cache)
                .map(ShortLink::getOriginalUrl))
            .switchIfEmpty(Mono.error(() -> new ObjectNotFoundException("Original url not found '%s'".formatted(shortCode))));
    }

    private Mono<ShortLink> cache(ShortLink shortLink) {
        Duration timeout = shortLink.getExpiresAt() == null ?
            shortLinkConfigurationProperties.getCacheDefaultTimeout() :
            Duration.between(Instant.now(), shortLink.getExpiresAt());

        if (!timeout.isPositive()) {
            return Mono.just(shortLink);
        }

        return redisTemplate.opsForValue()
            .set(shortLink.getShortCode(), shortLink.getOriginalUrl(), timeout)
            .thenReturn(shortLink);
    }

    @Override
    @Transactional
    public Mono<Void> deleteByShortCode(String shortCode) {
        Argument.checkNotNullWithGenericMessage(shortCode, ShortLink_.shortCode);

        return getUrlMetadataByShortCode(shortCode)
            .flatMap(shortLinkRepository::delete)
            .then(Mono.defer(() -> redisTemplate.delete(shortCode)))
            .then();
    }

}
