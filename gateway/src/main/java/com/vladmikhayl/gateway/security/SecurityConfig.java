package com.vladmikhayl.gateway.security;

import com.vladmikhayl.gateway.security.jwt.AuthEntryPointJwt;
import com.vladmikhayl.gateway.security.jwt.AuthTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;

    private final AuthEntryPointJwt authEntryPointJwt;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPointJwt));

        http.addFilterAt(authTokenFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

}
