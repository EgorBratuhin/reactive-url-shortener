package by.bratukhin.shortener.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.relational.core.query.Query;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import by.bratukhin.shortener.configuration.ShortLinkConfigurationProperties;
import by.bratukhin.shortener.model.ShortLink;
import by.bratukhin.shortener.repository.ShortLinkRepository;
import by.bratukhin.shortener.support.ItemsPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

///
/// Test for [UrlShortenerServiceImpl].
///
@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceImplTest {

    private static final String TEST_URI = "https://example.com";
    private static final Integer TTL_SECONDS = 3600;
    private static final int PAGE_SIZE = 10;

    @Mock
    private ShortLinkConfigurationProperties shortLinkConfigurationProperties;

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ShortCodeEncoder shortCodeEncoder;

    @Mock
    private ReactiveSelectOperation.ReactiveSelect<ShortLink> reactiveSelect;

    @Mock
    private ReactiveSelectOperation.TerminatingSelect<ShortLink> terminatingSelect;

    @InjectMocks
    private UrlShortenerServiceImpl urlShortenerService;

    @Test
    void createNewShortLink() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(any(), any(), any(Duration.class)))
            .thenReturn(Mono.just(true));

        when(shortCodeEncoder.encode(any(UUID.class))).thenReturn("shortCode");
        when(shortLinkRepository.save(any(ShortLink.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<ShortLink> result = urlShortenerService.create(URI.create(TEST_URI), TTL_SECONDS);

        StepVerifier.create(result)
            .assertNext(shortLink -> {
                assertThat(shortLink.getOriginalUrl()).isEqualTo(TEST_URI);
                assertThat(shortLink.getShortCode()).isEqualTo("shortCode");
                assertThat(shortLink.isNew()).isTrue();
                assertThat(shortLink.getExpiresAt())
                    .isAfter(Instant.now().plusSeconds(TTL_SECONDS - 1))
                    .isBefore(Instant.now().plusSeconds(TTL_SECONDS + 1));
                assertThat(shortLink.getId()).isNotNull();
            })
            .verifyComplete();

        verify(shortCodeEncoder).encode(any(UUID.class));
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    void createNeverExpiredShortLink() {
        when(shortLinkConfigurationProperties.getCacheDefaultTimeout())
            .thenReturn(Duration.ofMinutes(1));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(eq("shortCode"), eq(TEST_URI), any(Duration.class)))
            .thenReturn(Mono.just(true));

        when(shortCodeEncoder.encode(any(UUID.class))).thenReturn("shortCode");
        when(shortLinkRepository.save(any(ShortLink.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<ShortLink> result = urlShortenerService.create(URI.create(TEST_URI), null);

        StepVerifier.create(result)
            .assertNext(shortLink -> {
                assertThat(shortLink.getOriginalUrl()).isEqualTo(TEST_URI);
                assertThat(shortLink.getShortCode()).isEqualTo("shortCode");
                assertThat(shortLink.isNew()).isTrue();
                assertThat(shortLink.getExpiresAt()).isNull();
                assertThat(shortLink.getId()).isNotNull();
            })
            .verifyComplete();

        verify(shortCodeEncoder).encode(any(UUID.class));
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @ParameterizedTest
    @ValueSource(strings = "lastShortCode")
    @NullSource
    void getShortLinksPageWithNextCursor(String lastShortCode) {
        List<ShortLink> shortLinks = newShortLinks(PAGE_SIZE + 1);

        setupR2dbcEntityTemplateMock(shortLinks);

        Mono<ItemsPage<ShortLink>> result = urlShortenerService.getShortLinks(lastShortCode, PAGE_SIZE);

        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page.hasNext()).isTrue();
                assertThat(page.items()).hasSize(PAGE_SIZE);
                assertThat(page.nextCursor())
                    .isEqualTo(shortLinks.get(PAGE_SIZE - 1).getShortCode());
            })
            .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = "lastShortCode")
    @NullSource
    void getShortLinksPageWithoutNextCursor(String lastShortCode) {
        List<ShortLink> shortLinks = newShortLinks(PAGE_SIZE);

        setupR2dbcEntityTemplateMock(shortLinks);

        Mono<ItemsPage<ShortLink>> result = urlShortenerService.getShortLinks(lastShortCode, PAGE_SIZE);

        StepVerifier.create(result)
            .assertNext(page -> {
                assertThat(page.hasNext()).isFalse();
                assertThat(page.items()).hasSize(PAGE_SIZE);
                assertThat(page.nextCursor()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void getUrlMetadataByShortCode() {
        ShortLink expectedShortLink = newShortLink("shortCode", TEST_URI);

        when(shortLinkRepository.findByShortCode("shortCode"))
            .thenReturn(Mono.just(expectedShortLink));

        Mono<ShortLink> result = urlShortenerService.getUrlMetadataByShortCode("shortCode");

        StepVerifier.create(result)
            .assertNext(shortLink -> {
                assertThat(shortLink.getShortCode()).isEqualTo("shortCode");
                assertThat(shortLink.getOriginalUrl()).isEqualTo(TEST_URI);
            })
            .verifyComplete();

        verify(shortLinkRepository).findByShortCode("shortCode");
    }

    @Test
    void getUrlMetadataByShortCodeNotFound() {
        String nonExistentCode = "nonexistent";

        when(shortLinkRepository.findByShortCode(nonExistentCode))
            .thenReturn(Mono.empty());

        Mono<ShortLink> result = urlShortenerService.getUrlMetadataByShortCode(nonExistentCode);

        StepVerifier.create(result)
            .expectError(ObjectNotFoundException.class)
            .verify();

        verify(shortLinkRepository).findByShortCode(nonExistentCode);
    }

    @Test
    void getOriginalUrlByShortCode() {
        String shortCode = "shortCode";
        ShortLink expectedShortLink = newShortLink(shortCode, TEST_URI);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(shortCode)).thenReturn(Mono.empty());
        when(valueOperations.set(eq(shortCode), eq(TEST_URI), any(Duration.class)))
            .thenReturn(Mono.just(true));

        when(shortLinkRepository.findByShortCode(shortCode))
            .thenReturn(Mono.just(expectedShortLink));

        Mono<String> result = urlShortenerService.getOriginalUrlByShortCode(shortCode);

        StepVerifier.create(result)
            .assertNext(originalUrl -> assertThat(originalUrl).isEqualTo(TEST_URI))
            .verifyComplete();

        verify(shortLinkRepository).findByShortCode(shortCode);
    }

    @Test
    void getOriginalUrlFromCache() {
        String shortCode = "shortCode";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(shortCode)).thenReturn(Mono.just(TEST_URI));

        Mono<String> result = urlShortenerService.getOriginalUrlByShortCode(shortCode);

        StepVerifier.create(result)
            .assertNext(originalUrl -> assertThat(originalUrl).isEqualTo(TEST_URI))
            .verifyComplete();
    }

    @Test
    void getOriginalUrlByShortCodeNotFound() {
        String nonExistentCode = "nonexistent";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(nonExistentCode)).thenReturn(Mono.empty());

        when(shortLinkRepository.findByShortCode(nonExistentCode))
            .thenReturn(Mono.empty());

        Mono<String> result = urlShortenerService.getOriginalUrlByShortCode(nonExistentCode);

        StepVerifier.create(result)
            .expectError(ObjectNotFoundException.class)
            .verify();

        verify(shortLinkRepository).findByShortCode(nonExistentCode);
    }

    @Test
    void deleteByShortCode() {
        ShortLink expectedShortLink = newShortLink("shortCode", TEST_URI);

        when(redisTemplate.delete("shortCode"))
            .thenReturn(Mono.just(1L));

        when(shortLinkRepository.findByShortCode("shortCode"))
            .thenReturn(Mono.just(expectedShortLink));

        when(shortLinkRepository.delete(expectedShortLink))
            .thenReturn(Mono.empty());

        Mono<Void> result = urlShortenerService.deleteByShortCode("shortCode");

        StepVerifier.create(result)
            .verifyComplete();

        verify(shortLinkRepository).delete(expectedShortLink);
    }


    private List<ShortLink> newShortLinks(int count) {
        return IntStream.range(0, count)
            .mapToObj(linkNumber -> newShortLink(
                "shortCode%d".formatted(linkNumber),
                "https://example%d.com".formatted(linkNumber)))
            .toList();
    }

    private ShortLink newShortLink(String shortCode, String originalUrl) {
        ShortLink shortLink = new ShortLink();
        shortLink.setId(UUID.randomUUID());
        shortLink.setShortCode(shortCode);
        shortLink.setOriginalUrl(originalUrl);
        shortLink.setNew(false);
        shortLink.setExpiresAt(Instant.now().plusSeconds(TTL_SECONDS));
        return shortLink;
    }

    private void setupR2dbcEntityTemplateMock(List<ShortLink> shortLinks) {
        when(r2dbcEntityTemplate.select(ShortLink.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(shortLinks));
    }

}
