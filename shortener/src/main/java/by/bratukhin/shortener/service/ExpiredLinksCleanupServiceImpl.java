package by.bratukhin.shortener.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bratukhin.shortener.model.ShortLink;
import reactor.core.publisher.Mono;

///
/// Implementation of [ExpiredLinksCleanupService].
///
@Service
class ExpiredLinksCleanupServiceImpl implements ExpiredLinksCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredLinksCleanupServiceImpl.class);

    private final R2dbcEntityTemplate template;

    ExpiredLinksCleanupServiceImpl(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    @Transactional
    public Mono<Long> cleanupExpired() {
        Criteria criteria = Criteria.where(ShortLink.Fields.expiresAt).lessThan(Instant.now());

        return template.delete(Query.query(criteria), ShortLink.class)
            .doOnSuccess(count -> {
                if (count != null && count > 0) {
                    LOGGER.info("Deleted '{}' short links.", count);
                }
            });
    }

}
