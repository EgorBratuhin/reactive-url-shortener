package by.bratukhin.shortener.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;

import by.bratukhin.shortener.service.ObjectNotFoundException;
import reactor.core.publisher.Mono;

///
/// Глобальный обработчик исключений.
///
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<ErrorResponse> handleEntityNotFound(ObjectNotFoundException ex) {
        return Mono.just(ErrorResponse.builder(ex,
                HttpStatus.NOT_FOUND,
                ex.getMessage())
            .build());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<ErrorResponse> handleEntityNotFound(NoResourceFoundException ex) {
        return Mono.just(ErrorResponse.builder(ex,
                HttpStatus.NOT_FOUND,
                ex.getMessage())
            .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(ErrorResponse.builder(ex,
                HttpStatus.BAD_REQUEST,
                ex.getMessage())
            .build());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ErrorResponse> handleValidationFailure(WebExchangeBindException ex) {
        return Mono.just(ErrorResponse.builder(ex,
                HttpStatus.BAD_REQUEST,
                ex.getMessage())
            .build());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Mono<ErrorResponse> handleGeneral(Exception ex, ServerWebExchange exchange) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();

        LOGGER.error("Unexpected exception [{} {}]", method, path, ex);

        return Mono.just(ErrorResponse.builder(ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error")
            .build());
    }
}
