package com.tiendanube.orchestration.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class WebMvcConfigTest {

    @Test
    @DisplayName("Should instantiate WebMvcConfig and configure AntPathMatcher without error")
    void shouldConfigurePathMatch() {
        final WebMvcConfig config = new WebMvcConfig();
        assertThatCode(() -> config.configurePathMatch(new org.springframework.web.servlet.config.annotation.PathMatchConfigurer()))
                .doesNotThrowAnyException();
    }
}
