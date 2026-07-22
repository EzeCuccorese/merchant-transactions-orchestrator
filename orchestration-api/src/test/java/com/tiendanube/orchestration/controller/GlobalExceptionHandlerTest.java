package com.tiendanube.orchestration.controller;

import com.tiendanube.orchestration.domain.exception.CardValidationException;
import com.tiendanube.orchestration.domain.exception.NumeratorConflictException;
import com.tiendanube.orchestration.domain.exception.OrchestrationRollbackException;
import com.tiendanube.orchestration.domain.exception.TransactionNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should map CardValidationException to 400 Bad Request ProblemDetail")
    void shouldHandleCardValidationException() {
        final CardValidationException ex = new CardValidationException("CVV invalid");
        final ResponseEntity<ProblemDetail> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("CARD_VALIDATION_ERROR");
        assertThat(response.getBody().getDetail()).isEqualTo("CVV invalid");
    }

    @Test
    @DisplayName("Should map TransactionNotFoundException to 404 Not Found ProblemDetail")
    void shouldHandleTransactionNotFoundException() {
        final TransactionNotFoundException ex = new TransactionNotFoundException("999");
        final ResponseEntity<ProblemDetail> response = handler.handleNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("TRANSACTION_NOT_FOUND");
    }

    @Test
    @DisplayName("Should map NumeratorConflictException and OrchestrationRollbackException to 500 Internal Server Error")
    void shouldHandleServerErrorExceptions() {
        final NumeratorConflictException ex1 = new NumeratorConflictException("Numerator exhausted");
        final ResponseEntity<ProblemDetail> response1 = handler.handleServerErrorException(ex1);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response1.getBody().getTitle()).isEqualTo("NUMERATOR_CONFLICT");

        final OrchestrationRollbackException ex2 = new OrchestrationRollbackException("Rollback failed", new RuntimeException());
        final ResponseEntity<ProblemDetail> response2 = handler.handleServerErrorException(ex2);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response2.getBody().getTitle()).isEqualTo("ORCHESTRATION_ROLLBACK");
    }

    @Test
    @DisplayName("Should map MethodArgumentNotValidException to 400 Bad Request ProblemDetail with field error details")
    void shouldHandleMethodArgumentNotValidException() {
        final MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        final BindingResult bindingResult = Mockito.mock(BindingResult.class);
        final FieldError fieldError = new FieldError("objectName", "cardNumber", "cardNumber cannot be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        final ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getDetail()).isEqualTo("cardNumber: cardNumber cannot be blank");
    }

    @Test
    @DisplayName("Should map generic Exception to 500 Internal Server Error")
    void shouldHandleGenericException() {
        final Exception ex = new RuntimeException("Unexpected db crash");
        final ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
