package by.bratukhin.shortener;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.reactive.GenericReactiveWebApplicationContext;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;

///
/// Test for [ReactiveUrlShortenerApplication].
///
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReactiveUrlShortenerApplicationTest {

    @Test
    void contextLoads(GenericReactiveWebApplicationContext applicationContext) {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.isActive()).isTrue();
        assertThat(applicationContext.isRunning()).isTrue();
    }

    @Test
    void main() {
        ReactiveUrlShortenerApplication.main(new String[] {});
    }

}
