package com.example.bankcards.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        try {
            ClassPathResource resource = new ClassPathResource("docs/openapi.yaml");
            String spec = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
            return new OpenAPIV3Parser().readContents(spec).getOpenAPI();
        } catch (Exception e) {
            // Fallback: attempt path-based read during development
            return new OpenAPIV3Parser().read("src/main/resources/docs/openapi.yaml");
        }
    }
}

