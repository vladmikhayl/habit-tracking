package com.vladmikhayl.subscription.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    // Бин для настройки Сваггера
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("subscription").version("v1")) // указываем информацию о микросервисе
                .addServersItem(new Server().url("http://localhost:8080")) // показываем, что обращаться к API нужно через gateway
                .components(new Components().addSecuritySchemes("BearerAuth", // показываем, что можно передавать JWT
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
