package com.tiendanube.orchestration.controller;

import com.tiendanube.orchestration.domain.exception.BusinessException;
import com.tiendanube.orchestration.domain.exception.NumeratorConflictException;
import com.tiendanube.orchestration.domain.exception.OrchestrationRollbackException;
import com.tiendanube.orchestration.domain.exception.TransactionNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Controller Advice translating internal exceptions into RFC 7807 ProblemDetails responses
 * with structured WARN/ERROR logging.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(final BusinessException ex) {
        log.warn("Business domain exception: {}", ex.getMessage());
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("https://tiendanube.com/errors/business-error"));
        pd.setTitle("CARD_VALIDATION_ERROR");
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(final TransactionNotFoundException ex) {
        log.warn("Resource not found exception: {}", ex.getMessage());
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://tiendanube.com/errors/not-found"));
        pd.setTitle("TRANSACTION_NOT_FOUND");
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(final NoResourceFoundException ex) {
        log.warn("Static resource not found: {}", ex.getMessage());
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://tiendanube.com/errors/not-found"));
        pd.setTitle("RESOURCE_NOT_FOUND");
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(final MethodArgumentNotValidException ex) {
        final String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error on request body: {}", errors);

        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        pd.setType(URI.create("https://tiendanube.com/errors/validation-error"));
        pd.setTitle("VALIDATION_ERROR");
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCircuitBreakerOpenException(final CallNotPermittedException ex) {
        log.error("Circuit breaker is OPEN: {}", ex.getMessage());
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Downstream service is temporarily unavailable due to open circuit breaker");
        pd.setType(URI.create("https://tiendanube.com/errors/service-unavailable"));
        pd.setTitle("CIRCUIT_BREAKER_OPEN");
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
    }

    @ExceptionHandler({NumeratorConflictException.class, OrchestrationRollbackException.class})
    public ResponseEntity<ProblemDetail> handleServerErrorException(final Exception ex) {
        log.error("Orchestration server error exception: {}", ex.getMessage(), ex);
        final String title = ex instanceof NumeratorConflictException ? "NUMERATOR_CONFLICT" : "ORCHESTRATION_ROLLBACK";
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setType(URI.create("https://tiendanube.com/errors/internal-server-error"));
        pd.setTitle(title);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(final Exception ex) {
        log.error("Unhandled unexpected exception: {}", ex.getMessage(), ex);
        final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred");
        pd.setType(URI.create("https://tiendanube.com/errors/internal-server-error"));
        pd.setTitle("INTERNAL_SERVER_ERROR");
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }
}
