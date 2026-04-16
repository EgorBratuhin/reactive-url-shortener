package by.bratukhin.shortener.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;

import com.fasterxml.uuid.Generators;

import by.bratukhin.shortener.model.AbstractDataObject;
import reactor.core.publisher.Mono;

///
/// R2DBC configuration.
///
@Configuration
@EnableR2dbcAuditing
class R2dbcConfig {

    @Bean
    BeforeConvertCallback<AbstractDataObject> generateIdForNewEntities() {
        return (entity, _) -> {
            if (entity.isNew() && entity.getId() == null) {
                entity.setId(Generators.timeBasedEpochGenerator().generate());
            }
            return Mono.just(entity);
        };
    }

    @Bean
    AfterConvertCallback<AbstractDataObject> markLoadedEntitiesAsNotNew() {
        return (entity, _) -> {
            entity.setNew(false);
            return Mono.just(entity);
        };
    }
}
