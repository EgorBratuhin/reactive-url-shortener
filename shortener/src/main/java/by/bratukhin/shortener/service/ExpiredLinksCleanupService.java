package by.bratukhin.shortener.service;

import reactor.core.publisher.Mono;

///
/// Service responsible for cleaning up expired short links from the database.
///
public interface ExpiredLinksCleanupService {

    ///
    /// Removes all expired short links from the database.
    ///
    /// @return a [Mono] emitting the number of deleted links
    ///
    Mono<Long> cleanupExpired();
}
