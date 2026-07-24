package com.merchant.orchestration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration bean for OpenAPI 3.0 / Swagger UI documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Merchant Transactions Orchestration API")
                        .version("1.0.0")
                        .description("API for orchestrating transaction creation, fee computation, and merchant receivables with unique ID generation.")
                        .contact(new Contact().name("Merchant Engineering"))
                        .license(new License().name("Apache 2.0")));
    }
}
