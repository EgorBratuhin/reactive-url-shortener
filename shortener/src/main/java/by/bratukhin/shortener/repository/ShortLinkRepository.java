package by.bratukhin.shortener.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import by.bratukhin.shortener.model.ShortLink;
import reactor.core.publisher.Mono;

///
/// Reactive repository for persistent storage and retrieval of [ShortLink] entities.
///
/// @see ShortLink
///
@Repository
public interface ShortLinkRepository extends R2dbcRepository<ShortLink, UUID> {

    ///
    /// Finds a [ShortLink] by its unique short code.
    ///
    /// @param shortCode the short code identifier; must not be null
    /// @return a [Mono] emitting the found [ShortLink]
    ///
    Mono<ShortLink> findByShortCode(String shortCode);

    ///
    /// Deletes a [ShortLink] by its short code.
    ///
    /// @param shortCode the short code identifier of the link to delete; must not be null
    /// @return a [Mono#empty()] that signals completion when the deletion is finished
    ///
    Mono<Void> deleteByShortCode(String shortCode);

    ///
    /// Finds the original URL by its short code without loading the full entity.
    ///
    /// @param shortCode the short code identifier; must not be null
    /// @param now       current timestamp; must not be null
    /// @return a [Mono] emitting the original URL string
    ///
    @Query("SELECT original_url FROM short_links WHERE short_code = :shortCode AND expires_at > :now")
    Mono<String> findOriginalUrlByShortCode(String shortCode, Instant now);

}
