package by.bratukhin.shortener.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import by.bratukhin.api.UrlsApi;
import by.bratukhin.api.model.CreateUrlRequestDto;
import by.bratukhin.api.model.UrlListResponseDto;
import by.bratukhin.api.model.UrlMetadataDto;
import by.bratukhin.shortener.configuration.ShortLinkConfigurationProperties;
import by.bratukhin.shortener.model.ShortLink;
import by.bratukhin.shortener.service.UrlShortenerService;
import by.bratukhin.shortener.support.ItemsPage;
import by.bratukhin.shortener.web.conversion.DateConverter;
import reactor.core.publisher.Mono;

///
/// REST controller handling URL shortener management operations.
///
@RestController
@RequestMapping("/api/v1")
class UrlsController implements UrlsApi {

    private final ShortLinkConfigurationProperties shortLinkConfigurationProperties;

    private final UrlShortenerService urlShortenerService;

    UrlsController(ShortLinkConfigurationProperties shortLinkConfigurationProperties, UrlShortenerService urlShortenerService) {
        this.shortLinkConfigurationProperties = shortLinkConfigurationProperties;
        this.urlShortenerService = urlShortenerService;
    }

    @Override
    public Mono<ResponseEntity<UrlMetadataDto>> createShortUrl(Mono<CreateUrlRequestDto> createUrlRequestDto,
        ServerWebExchange exchange) {

        return createUrlRequestDto
            .flatMap(this::create)
            .map(this::toUrlMetadataDto)
            .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    private Mono<ShortLink> create(CreateUrlRequestDto request) {
        return urlShortenerService.create(request.getUrl(), request.getTtlSeconds(), request.getShortCode());
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteShortUrl(String shortCode, ServerWebExchange exchange) {
        return urlShortenerService.deleteByShortCode(shortCode)
            .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).body(null));
    }

    @Override
    public Mono<ResponseEntity<UrlMetadataDto>> getUrlMetadata(String shortCode, ServerWebExchange exchange) {
        return urlShortenerService.getUrlMetadataByShortCode(shortCode)
            .map(this::toUrlMetadataDto)
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<UrlListResponseDto>> listShortUrls(String nextCursor, Integer size, ServerWebExchange exchange) {
        return urlShortenerService.getShortLinks(nextCursor, size)
            .map(this::toUrlListResponseDto)
            .map(ResponseEntity::ok);
    }

    private UrlListResponseDto toUrlListResponseDto(ItemsPage<ShortLink> page) {
        return new UrlListResponseDto()
            .hasNext(page.hasNext())
            .nextCursor(page.nextCursor())
            .items(page.items().stream()
                .map(this::toUrlMetadataDto)
                .toList());
    }

    private URI buildShortUri(ShortLink shortLink) {
        return UriComponentsBuilder.fromUriString(shortLinkConfigurationProperties.getBaseDomain())
            .path(shortLink.getShortCode())
            .build()
            .toUri();
    }

    private UrlMetadataDto toUrlMetadataDto(ShortLink shortLink) {
        return new UrlMetadataDto()
            .shortCode(shortLink.getShortCode())
            .shortUrl(buildShortUri(shortLink))
            .originalUrl(URI.create(shortLink.getOriginalUrl()))
            .expiresAt(DateConverter.convert(shortLink.getExpiresAt()))
            .createdAt(DateConverter.convert(shortLink.getCreatedAt()));
    }

}
