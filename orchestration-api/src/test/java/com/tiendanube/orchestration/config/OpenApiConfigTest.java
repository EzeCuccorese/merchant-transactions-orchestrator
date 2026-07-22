package com.tiendanube.orchestration.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    @DisplayName("Should create custom OpenAPI bean with Title and Version info")
    void shouldCreateCustomOpenAPI() {
        final OpenApiConfig config = new OpenApiConfig();
        final OpenAPI openAPI = config.customOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Merchant Transactions Orchestration API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
    }
}
