package by.bratukhin.shortener.model;

import java.time.Instant;

import org.springframework.data.relational.core.mapping.Table;

import lombok.experimental.FieldNameConstants;

///
/// Represents a short link mapping a unique short code to an original URL.
///
@Table("short_links")
@FieldNameConstants
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

    public ShortLink setShortCode(String shortCode) {
        this.shortCode = shortCode;
        return this;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public ShortLink setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
        return this;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ShortLink setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }
}
