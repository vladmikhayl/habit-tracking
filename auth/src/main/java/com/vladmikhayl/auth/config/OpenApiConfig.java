package com.vladmikhayl.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    // Бин для настройки Сваггера
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("auth").version("v1")) // указываем информацию о микросервисе
                .addServersItem(new Server().url("http://localhost:8080")); // показываем, что обращаться к API нужно через gateway
    }
}
