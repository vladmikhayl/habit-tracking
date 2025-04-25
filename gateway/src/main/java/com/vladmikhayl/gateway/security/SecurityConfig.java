package com.vladmikhayl.gateway.security;

import com.vladmikhayl.gateway.security.internal.InternalTokenFilter;
import com.vladmikhayl.gateway.security.jwt.JwtTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;

    private final InternalTokenFilter internalTokenFilter;

    private final AuthEntryPoint authEntryPoint;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/swagger-resources/**").permitAll()
                        .pathMatchers("/habit/v3/api-docs/**", "/auth/v3/api-docs/**", "/report/v3/api-docs/**", "/subscription/v3/api-docs/**").permitAll()
                        .pathMatchers("/internal/**").authenticated()
                        .pathMatchers("/api/**").authenticated()
                )
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPoint));

        http.addFilterAt(jwtTokenFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        http.addFilterAt(internalTokenFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

}
