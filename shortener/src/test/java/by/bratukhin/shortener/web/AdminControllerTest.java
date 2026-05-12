package by.bratukhin.shortener.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.mockito.Mockito.when;

import by.bratukhin.shortener.configuration.SecurityConfig;
import by.bratukhin.shortener.service.ExpiredLinksCleanupService;
import reactor.core.publisher.Mono;

@WebFluxTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ExpiredLinksCleanupService expiredLinksCleanupService;

    @Test
    @WithMockUser(authorities = "SCOPE_url:clean")
    void cleanupSuccess() {
        when(expiredLinksCleanupService.cleanupExpired()).thenReturn(Mono.just(5L));

        webTestClient.post()
            .uri("/api/v1/admin/cleanup")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.deletedRows").isEqualTo(5);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_url:manage")
    void cleanupForbidden() {
        webTestClient.post()
            .uri("/api/v1/admin/cleanup")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void cleanupUnauthorized() {
        webTestClient.post()
            .uri("/api/v1/admin/cleanup")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
