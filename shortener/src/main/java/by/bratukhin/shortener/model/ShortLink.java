package by.bratukhin.shortener.model;

import java.time.Instant;

import org.springframework.data.relational.core.mapping.Table;

import by.bratukhin.meta.GenerateFieldNames;

///
/// Represents a short link mapping a unique short code to an original URL.
///
@Table("short_links")
@GenerateFieldNames
public class ShortLink extends AbstractDataObject {

    ///
    /// Unique short code used to identify and resolve the link.
    ///
    private String shortCode;

    ///
    /// The original URL that the short link redirects to.
    ///
    private String originalUrl;

    ///
    /// The expiration timestamp after which the link is considered invalid.
    /// A `null` value indicates the link never expires.
    ///
    private Instant expiresAt;

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    ///
    /// Checks whether the link is active.
    ///
    public boolean isActive() {
        return getExpiresAt() == null ||
               Instant.now().isBefore(getExpiresAt());
    }
}
