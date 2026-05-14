package by.bratukhin.shortener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import by.bratukhin.shortener.model.ShortLink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

///
/// Test for [ExpiredLinksCleanupServiceImpl].
///
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ExpiredLinksCleanupServiceImplTest {

    @Mock
    private R2dbcEntityTemplate template;

    @InjectMocks
    private ExpiredLinksCleanupServiceImpl service;

    @Test
    void cleanupExpired(CapturedOutput output) {
        when(template.delete(any(Query.class), eq(ShortLink.class)))
            .thenReturn(Mono.just(1L));

        StepVerifier.create(service.cleanupExpired())
            .expectNext(1L)
            .verifyComplete();

        assertThat(output.getOut())
            .isNotNull()
            .contains("Deleted '1' short links.");
    }

    @Test
    void cleanupExpiredEmpty(CapturedOutput output) {
        when(template.delete(any(Query.class), eq(ShortLink.class)))
            .thenReturn(Mono.just(0L));

        StepVerifier.create(service.cleanupExpired())
            .expectNext(0L)
            .verifyComplete();

        assertThat(output.getOut())
            .doesNotContain("Deleted");
    }

}
