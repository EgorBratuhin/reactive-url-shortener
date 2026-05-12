package by.bratukhin.shortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import by.bratukhin.shortener.model.ShortLink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

///
/// Test for [ExpiredLinksCleanupServiceImpl].
///
@ExtendWith(MockitoExtension.class)
class ExpiredLinksCleanupServiceImplTest {

    @Mock
    private R2dbcEntityTemplate template;

    @InjectMocks
    private ExpiredLinksCleanupServiceImpl service;

    @BeforeEach
    void setUp() {
        when(template.delete(any(Query.class), eq(ShortLink.class)))
            .thenReturn(Mono.just(1L));
    }

    @Test
    void cleanupExpired() {
        StepVerifier.create(service.cleanupExpired())
            .expectNext(1L)
            .verifyComplete();
    }

}
