package by.bratukhin.shortener.service;

import java.net.URI;
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
import org.springframework.data.relational.core.query.Query;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private static final long TTL_SECONDS = 3600L;
    private static final int PAGE_SIZE = 10;

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private R2dbcEntityTemplate template;

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

    @ParameterizedTest
    @ValueSource(strings = "lastShortCode")
    @NullSource
    void getShortLinksPageWithNextCursor(String lastShortCode) {
        List<ShortLink> shortLinks = newShortLinks(PAGE_SIZE + 1);

        setupTemplateMock(shortLinks);

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

        setupTemplateMock(shortLinks);

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
    void deleteByShortCode() {
        when(shortLinkRepository.deleteByShortCode("shortCode"))
            .thenReturn(Mono.empty());

        Mono<Void> result = urlShortenerService.deleteByShortCode("shortCode");

        StepVerifier.create(result)
            .verifyComplete();

        verify(shortLinkRepository).deleteByShortCode("shortCode");
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

    private void setupTemplateMock(List<ShortLink> shortLinks) {
        when(template.select(ShortLink.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(shortLinks));
    }

}
