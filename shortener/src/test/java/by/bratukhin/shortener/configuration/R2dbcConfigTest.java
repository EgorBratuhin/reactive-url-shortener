package by.bratukhin.shortener.configuration;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import static org.assertj.core.api.Assertions.assertThat;

import by.bratukhin.shortener.model.AbstractDataObject;
import reactor.test.StepVerifier;

///
/// Test for [R2dbcConfig].
///
@ExtendWith(MockitoExtension.class)
class R2dbcConfigTest {

    @InjectMocks
    private R2dbcConfig r2dbcConfig;

    @Test
    void shouldGenerateIdForNewEntityWithoutId() {
        BeforeConvertCallback<AbstractDataObject> callback = r2dbcConfig.generateIdForNewEntities();
        TestDataObject entity = new TestDataObject();
        entity.setNew(true);
        entity.setId(null);

        var result = callback.onBeforeConvert(entity, SqlIdentifier.EMPTY);

        StepVerifier.create(result)
            .assertNext(savedEntity -> {
                assertThat(savedEntity.getId()).isNotNull();
                assertThat(savedEntity.getId().version()).isEqualTo(7);
            })
            .verifyComplete();
    }

    @Test
    void shouldNotGenerateIdForNewEntityWithId() {
        BeforeConvertCallback<AbstractDataObject> callback = r2dbcConfig.generateIdForNewEntities();
        TestDataObject entity = new TestDataObject();
        UUID existingId = UUID.fromString("019de950-1d21-7148-925f-aefdbf03b130");
        entity.setNew(true);
        entity.setId(existingId);

        var result = callback.onBeforeConvert(entity, SqlIdentifier.EMPTY);

        StepVerifier.create(result)
            .assertNext(savedEntity ->
                assertThat(savedEntity.getId()).isEqualTo(existingId))
            .verifyComplete();
    }

    @Test
    void shouldNotGenerateIdForNonNewEntity() {
        BeforeConvertCallback<AbstractDataObject> callback = r2dbcConfig.generateIdForNewEntities();
        TestDataObject entity = new TestDataObject();
        entity.setNew(false);
        entity.setId(null);

        var result = callback.onBeforeConvert(entity, SqlIdentifier.EMPTY);

        StepVerifier.create(result)
            .assertNext(savedEntity ->
                assertThat(savedEntity.getId()).isNull())
            .verifyComplete();
    }

    @Test
    void shouldMarkLoadedEntityAsNotNew() {
        AfterConvertCallback<AbstractDataObject> callback = r2dbcConfig.markLoadedEntitiesAsNotNew();
        TestDataObject entity = new TestDataObject();
        entity.setNew(true);

        var result = callback.onAfterConvert(entity, SqlIdentifier.EMPTY);

        StepVerifier.create(result)
            .assertNext(savedEntity ->
                assertThat(savedEntity.isNew()).isFalse())
            .verifyComplete();
    }

    private static class TestDataObject extends AbstractDataObject {

    }
}
