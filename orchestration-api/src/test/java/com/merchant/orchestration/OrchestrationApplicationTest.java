package com.merchant.orchestration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThatCode;

class OrchestrationApplicationTest {

    @Test
    @DisplayName("Should instantiate main class cleanly")
    void shouldInstantiateMainClass() {
        assertThatCode(() -> new OrchestrationApplication()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should execute main method with empty arguments")
    void shouldExecuteMainMethod() {
        try (var mockedSpringApp = org.mockito.Mockito.mockStatic(SpringApplication.class)) {
            mockedSpringApp.when(() -> SpringApplication.run(OrchestrationApplication.class, new String[]{}))
                    .thenReturn(null);

            assertThatCode(() -> OrchestrationApplication.main(new String[]{}))
                    .doesNotThrowAnyException();
        }
    }
}
