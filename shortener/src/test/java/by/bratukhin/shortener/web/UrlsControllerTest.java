package by.bratukhin.shortener.web;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import by.bratukhin.shortener.configuration.SecurityConfig;
import by.bratukhin.shortener.configuration.ShortLinkConfigurationProperties;
import by.bratukhin.shortener.model.ShortLink;
import by.bratukhin.shortener.service.DuplicateShortCodeException;
import by.bratukhin.shortener.service.ObjectNotFoundException;
import by.bratukhin.shortener.service.UrlShortenerService;
import by.bratukhin.shortener.support.ItemsPage;
import reactor.core.publisher.Mono;

@WebFluxTest(UrlsController.class)
@Import({SecurityConfig.class, ShortLinkConfigurationProperties.class})
@WithMockUser(authorities = "SCOPE_url:manage")
class UrlsControllerTest {

    private static final String TEST_URI = "https://example.com";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UrlShortenerService urlShortenerService;

    @Test
    void createShortUrl() {
        String request = """
            {
                "url": "%s",
                "ttlSeconds": 3600,
                "shortCode": "shortCode"
            }
            """.formatted(TEST_URI);

        ShortLink shortLink = newShortLink("0123456789012345678901", TEST_URI);
        when(urlShortenerService.create(eq(URI.create(TEST_URI)), eq(3600), eq("shortCode")))
            .thenReturn(Mono.just(shortLink));

        webTestClient.post()
            .uri("/api/v1/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.shortCode").isEqualTo("0123456789012345678901")
            .jsonPath("$.originalUrl").isEqualTo(TEST_URI)
            .jsonPath("$.shortUrl").isEqualTo("http://localhost/0123456789012345678901");
    }

    @Test
    void createShortUrlDuplicateCode() {
        String request = """
            {
                "url": "%s",
                "shortCode": "taken"
            }
            """.formatted(TEST_URI);

        when(urlShortenerService.create(eq(URI.create(TEST_URI)), isNull(), eq("taken")))
            .thenReturn(Mono.error(new DuplicateShortCodeException("taken")));

        webTestClient.post()
            .uri("/api/v1/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.code").isEqualTo("SHORT_CODE_TAKEN")
            .jsonPath("$.message").isEqualTo("Short code 'taken' is already in use");
    }

    @Test
    void createShortUrlBadRequest() {
        String request = """
            {
                "url": "^",
                "ttlSeconds": 3600
            }
            """;

        webTestClient.post()
            .uri("/api/v1/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void deleteShortUrl() {
        when(urlShortenerService.deleteByShortCode("0123456789012345678901"))
            .thenReturn(Mono.empty());

        webTestClient.delete()
            .uri("/api/v1/urls/0123456789012345678901")
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    void getUrlMetadata() {
        ShortLink shortLink = newShortLink("0123456789012345678901", TEST_URI);
        when(urlShortenerService.getUrlMetadataByShortCode("0123456789012345678901"))
            .thenReturn(Mono.just(shortLink));

        webTestClient.get()
            .uri("/api/v1/urls/0123456789012345678901")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.shortCode").isEqualTo("0123456789012345678901")
            .jsonPath("$.originalUrl").isEqualTo(TEST_URI)
            .jsonPath("$.shortUrl").isEqualTo("http://localhost/0123456789012345678901");
    }

    @Test
    void urlMetadataNotFound() {
        when(urlShortenerService.getUrlMetadataByShortCode("0123456789012345678903"))
            .thenReturn(Mono.error(new ObjectNotFoundException("Url metadata not found")));

        webTestClient.get()
            .uri("/api/v1/urls/0123456789012345678903")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void listShortUrls() {
        List<ShortLink> shortLinks = List.of(newShortLink("0123456789012345678901", TEST_URI));
        ItemsPage<ShortLink> page = new ItemsPage<>(shortLinks, false, null);

        when(urlShortenerService.getShortLinks(eq("next-cursor"), eq(10)))
            .thenReturn(Mono.just(page));

        webTestClient.get()
            .uri("/api/v1/urls?nextCursor=next-cursor&size=10")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.hasNext").isEqualTo(false)
            .jsonPath("$.items[0].shortCode").isEqualTo("0123456789012345678901");
    }

    @Test
    void listShortUrlsWithPagination() {
        List<ShortLink> shortLinks = List.of(
            newShortLink("0123456789012345678901", "https://example1.com"),
            newShortLink("0123456789012345678902", "https://example2.com")
        );
        ItemsPage<ShortLink> page = new ItemsPage<>(shortLinks, true, "next-cursor");

        when(urlShortenerService.getShortLinks(isNull(), anyInt()))
            .thenReturn(Mono.just(page));

        webTestClient.get()
            .uri("/api/v1/urls")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.hasNext").isEqualTo(true)
            .jsonPath("$.nextCursor").isEqualTo("next-cursor")
            .jsonPath("$.items[0].shortCode").isEqualTo("0123456789012345678901")
            .jsonPath("$.items[1].shortCode").isEqualTo("0123456789012345678902");
    }

    private ShortLink newShortLink(String shortCode, String originalUrl) {
        ShortLink shortLink = new ShortLink();
        shortLink.setId(UUID.randomUUID());
        shortLink.setShortCode(shortCode);
        shortLink.setOriginalUrl(originalUrl);
        shortLink.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        return shortLink;
    }
}
