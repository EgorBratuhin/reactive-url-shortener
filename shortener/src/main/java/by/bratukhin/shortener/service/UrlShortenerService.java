package by.bratukhin.shortener.service;

import java.net.URI;

import by.bratukhin.shortener.model.ShortLink;
import by.bratukhin.shortener.support.ItemsPage;
import reactor.core.publisher.Mono;

///
/// Service for managing shortened URLs.
///
public interface UrlShortenerService {

    ///
    /// Creates a new shortened link.
    ///
    /// @param uri        the original URI to shorten; must not be null
    /// @param ttlSeconds the time-to-live duration in seconds
    /// @return a [Mono] emitting the created [ShortLink]
    ///
    Mono<ShortLink> create(URI uri, Integer ttlSeconds);

    ///
    /// Retrieves a paginated list of shortened links.
    ///
    /// @param lastShortCode the short code of the last item from the previous page (cursor)
    /// @param pageSize      the maximum number of items to return
    /// @return a [Mono] emitting the [ItemsPage] containing [ShortLink] items
    ///
    Mono<ItemsPage<ShortLink>> getShortLinks(String lastShortCode, int pageSize);

    ///
    /// Retrieves metadata for a shortened link by its short code.
    ///
    /// @param shortCode the short code to look up; must not be null
    /// @return a [Mono] emitting the [ShortLink] metadata
    ///
    Mono<ShortLink> getUrlMetadataByShortCode(String shortCode);

    ///
    /// Retrieves the original URL for a shortened link by its short code.
    /// Optimized for redirect lookups.
    ///
    /// @param shortCode the short code to look up; must not be null
    /// @return a [Mono] emitting the original URL string
    ///
    Mono<String> getOriginalUrlByShortCode(String shortCode);

    ///
    /// Deletes a shortened link by its short code.
    ///
    /// @param shortCode the short code to delete; must not be null
    /// @return a [Mono#empty()] completing the deletion operation
    ///
    Mono<Void> deleteByShortCode(String shortCode);

}
