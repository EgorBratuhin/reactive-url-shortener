package by.bratukhin.shortener.configuration;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

///
/// Configuration properties for the short link service.
///
@Component
@ConfigurationProperties(prefix = "short-link")
@Validated
public class ShortLinkConfigurationProperties {

    ///
    /// Cache default timeout; must be at least 1 minute.
    ///
    @DurationMin(minutes = 1)
    @NotNull
    private Duration cacheDefaultTimeout;

    ///
    /// Base domain used for constructing short links.
    ///
    @NotBlank
    private String baseDomain;

    public Duration getCacheDefaultTimeout() {
        return cacheDefaultTimeout;
    }

    public void setCacheDefaultTimeout(Duration cacheDefaultTimeout) {
        this.cacheDefaultTimeout = cacheDefaultTimeout;
    }

    public String getBaseDomain() {
        return baseDomain;
    }

    public void setBaseDomain(String baseDomain) {
        this.baseDomain = baseDomain;
    }
}
