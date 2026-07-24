package com.merchant.orchestration.controller.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTransactionRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation with valid CreateTransactionRequest data")
    void shouldPassValidationWithValidPayload() {
        final CreateTransactionRequest request = new CreateTransactionRequest(
                "250.00",
                "T-Shirt",
                "credit_card",
                "2222111144443486",
                "Simplenube Store",
                "04/28",
                "222"
        );

        final Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when required fields are blank or invalid")
    void shouldFailValidationWhenFieldsAreBlank() {
        final CreateTransactionRequest request = new CreateTransactionRequest(
                "",
                "",
                "invalid_method",
                "12",
                "",
                "invalid_date",
                "ab"
        );

        final Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
}
