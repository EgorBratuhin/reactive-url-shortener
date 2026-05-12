package by.bratukhin.shortener.web;

import java.net.URI;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import by.bratukhin.api.RedirectApi;
import by.bratukhin.shortener.service.UrlShortenerService;
import io.micrometer.observation.annotation.Observed;
import reactor.core.publisher.Mono;

///
/// REST controller that handles redirecting from short URLs to original URLs.
///
/// This controller is the entry point for resolving short URL codes and redirecting
/// clients to the original destination URL. It implements the [RedirectApi]
/// interface and returns HTTP 302 (Found) responses with the location header set
/// to the original URL.
///
@RestController
@RequestMapping("/")
class RedirectController implements RedirectApi {

    private final UrlShortenerService urlShortenerService;

    RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @Override
    @Observed(name = "links.resolved", contextualName = "resolving-short-link")
    public Mono<ResponseEntity<Void>> resolveAndRedirect(String shortCode, ServerWebExchange exchange) {
        return urlShortenerService.getOriginalUrlByShortCode(shortCode)
            .map(originalUrl -> ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .cacheControl(CacheControl.noCache())
                .build()
            );
    }

}
