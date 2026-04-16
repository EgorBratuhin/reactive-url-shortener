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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
class RedirectController implements RedirectApi {

    private final UrlShortenerService urlShortenerService;

    RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @Override
    public Mono<ResponseEntity<Void>> resolveAndRedirect(String shortCode, ServerWebExchange exchange) {
        return urlShortenerService.getUrlMetadataByShortCode(shortCode)
            .map(shortLink -> ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(shortLink.getOriginalUrl()))
                .cacheControl(CacheControl.noCache())
                .build()
            );
    }

}
