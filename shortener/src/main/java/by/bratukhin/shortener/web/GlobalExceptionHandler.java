package by.bratukhin.shortener.web;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import by.bratukhin.api.model.ErrorResponseDto;
import by.bratukhin.api.model.FieldErrorDto;
import by.bratukhin.api.model.ValidationErrorResponseDto;
import by.bratukhin.shortener.service.DuplicateShortCodeException;
import by.bratukhin.shortener.service.ObjectNotFoundException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<ErrorResponseDto> handleEntityNotFound(ObjectNotFoundException ex) {
        return Mono.just(new ErrorResponseDto("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateShortCodeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Mono<ErrorResponseDto> handleDuplicateShortCode(DuplicateShortCodeException ex) {
        return Mono.just(new ErrorResponseDto("SHORT_CODE_TAKEN", ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<ErrorResponseDto> handleResourceNotFound(NoResourceFoundException ex) {
        return Mono.just(new ErrorResponseDto("RESOURCE_NOT_FOUND", "Resource not found"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(new ErrorResponseDto("INVALID_ARGUMENT", ex.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    Mono<ValidationErrorResponseDto> handleValidationFailure(WebExchangeBindException ex) {
        List<FieldErrorDto> errors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::toFieldErrorDto)
            .forEach(errors::add);

        ex.getBindingResult().getGlobalErrors().stream()
            .map(GlobalExceptionHandler::toFieldErrorDto)
            .forEach(errors::add);

        return Mono.just(new ValidationErrorResponseDto("VALIDATION_ERROR",
            "Validation failed. Check 'errors' for details.", errors));
    }

    private static FieldErrorDto toFieldErrorDto(FieldError fieldError) {
        return new FieldErrorDto(fieldError.getField(), fieldError.getDefaultMessage())
            .code(fieldError.getCode());
    }

    private static FieldErrorDto toFieldErrorDto(ObjectError objectError) {
        return new FieldErrorDto(objectError.getObjectName(), objectError.getDefaultMessage())
            .code(objectError.getCode());
    }

    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ErrorResponseDto> handleWebInputFailure(ServerWebInputException ex) {
        return Mono.just(new ErrorResponseDto("BAD_REQUEST", "Invalid request"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Mono<ErrorResponseDto> handleGeneral(Exception ex, ServerWebExchange exchange) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();

        LOGGER.error("Unexpected exception [{} {}]", method, path, ex);

        return Mono.just(new ErrorResponseDto("INTERNAL_ERROR", "Internal server error"));
    }
}
