package by.bratukhin.shortener.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.CacheControl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import by.bratukhin.shortener.configuration.SecurityConfig;
import by.bratukhin.shortener.service.ObjectNotFoundException;
import by.bratukhin.shortener.service.UrlShortenerService;
import reactor.core.publisher.Mono;

///
/// Test for [RedirectController].
///
@WebFluxTest(RedirectController.class)
@Import(SecurityConfig.class)
class RedirectControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UrlShortenerService urlShortenerService;

    @Test
    void redirectWhenShortCodeExists() {
        String shortCode = "0330bhML322HMHIolUI760";
        String originalUrl = "https://example.com/very/long/path";

        when(urlShortenerService.getOriginalUrlByShortCode(shortCode))
            .thenReturn(Mono.just(originalUrl));

        webTestClient.get()
            .uri("/{shortCode}", shortCode)
            .exchange()
            .expectStatus().isFound()
            .expectHeader().valueEquals("Location", originalUrl)
            .expectHeader().cacheControl(CacheControl.noCache())
            .expectBody().isEmpty();

        verify(urlShortenerService).getOriginalUrlByShortCode(shortCode);
    }

    @Test
    void notFoundWhenShortCodeDoesNotExist() {
        String shortCode = "000000000000000unknown";
        when(urlShortenerService.getOriginalUrlByShortCode(shortCode))
            .thenReturn(Mono.error(new ObjectNotFoundException("Not found")));

        webTestClient.get()
            .uri("/{shortCode}", shortCode)
            .exchange()
            .expectStatus().isNotFound();

        verify(urlShortenerService).getOriginalUrlByShortCode(shortCode);
    }

    @Test
    void serviceError() {
        String shortCode = "00000000000000000error";
        when(urlShortenerService.getOriginalUrlByShortCode(shortCode))
            .thenReturn(Mono.error(new RuntimeException("Any error")));

        webTestClient.get()
            .uri("/{shortCode}", shortCode)
            .exchange()
            .expectStatus().is5xxServerError();
    }

}
