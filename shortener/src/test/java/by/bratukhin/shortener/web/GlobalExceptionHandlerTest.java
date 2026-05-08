package by.bratukhin.shortener.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import by.bratukhin.shortener.service.ObjectNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

///
/// Test for [GlobalExceptionHandler].
///
@WebFluxTest(GlobalExceptionHandlerTest.TestExceptionController.class)
@Import(GlobalExceptionHandlerTest.TestConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void objectNotFound() {
        webTestClient.get()
            .uri("/test/not-found")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND")
            .jsonPath("$.message").isEqualTo("Url metadata not found 'test'");
    }

    @Test
    void illegalArgument() {
        webTestClient.get()
            .uri("/test/invalid-arg")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_ARGUMENT")
            .jsonPath("$.message").isEqualTo("Invalid URL format");
    }

    @Test
    void serverWebInputError() {
        webTestClient.post()
            .uri("/test/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.code").isEqualTo("BAD_REQUEST")
            .jsonPath("$.message").isEqualTo("Invalid request");
    }

    @Test
    void validationError() {
        webTestClient.post()
            .uri("/test/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "name": ""
                }
                """)
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody()
            .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
            .jsonPath("$.message").isEqualTo("Validation failed. Check 'errors' for details.")
            .jsonPath("$.errors[0].field").isEqualTo("name")
            .jsonPath("$.errors[0].message").isEqualTo("must not be blank")
            .jsonPath("$.errors[0].code").isEqualTo("NotBlank");
    }

    @Test
    void internalError() {
        webTestClient.get()
            .uri("/test/internal-error")
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody()
            .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
            .jsonPath("$.message").isEqualTo("Internal server error");
    }

    @Test
    void noResourceFound() {
        webTestClient.get()
            .uri("/test/non-existent-path-12345")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND")
            .jsonPath("$.message").isEqualTo("Resource not found");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        TestExceptionController testExceptionController() {
            return new TestExceptionController();
        }
    }

    @RestController
    static class TestExceptionController {

        @GetMapping("/test/not-found")
        Mono<Void> notFound() {
            throw new ObjectNotFoundException("Url metadata not found 'test'");
        }

        @GetMapping("/test/invalid-arg")
        Mono<Void> invalidArg() {
            throw new IllegalArgumentException("Invalid URL format");
        }

        @PostMapping("/test/validate")
        Mono<TestBody> validate(@Valid @RequestBody Mono<TestBody> body) {
            return body;
        }

        @GetMapping("/test/internal-error")
        Mono<Void> internalError() {
            throw new RuntimeException("Database connection failed");
        }
    }

    static class TestBody {

        @NotBlank
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
