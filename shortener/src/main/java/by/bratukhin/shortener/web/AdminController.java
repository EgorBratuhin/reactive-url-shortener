package by.bratukhin.shortener.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import by.bratukhin.api.AdminApi;
import by.bratukhin.api.model.CleanupResponseDto;
import by.bratukhin.shortener.service.ExpiredLinksCleanupService;
import reactor.core.publisher.Mono;

///
/// REST controller handling URL shortener administration operations.
///
@RestController
@RequestMapping("/api/v1")
class AdminController implements AdminApi {

    private final ExpiredLinksCleanupService expiredLinksCleanupService;

    AdminController(ExpiredLinksCleanupService expiredLinksCleanupService) {
        this.expiredLinksCleanupService = expiredLinksCleanupService;
    }

    @Override
    public Mono<ResponseEntity<CleanupResponseDto>> cleanupExpiredLinks(ServerWebExchange exchange) {
        return expiredLinksCleanupService.cleanupExpired()
            .map(count -> ResponseEntity.ok(new CleanupResponseDto().deletedRows(count)));
    }

}
