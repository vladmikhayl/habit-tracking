package com.vladmikhayl.gateway.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter implements WebFilter {

    private final JwtUtils jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Пропускаем, если это не внешний запрос
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        String token = jwtUtils.getJwtTokenFromRequest(request);

        if (token != null && jwtUtils.validateJwtToken(token)) {
            Authentication authentication = jwtUtils.getAuthenticationFromJwtToken(token);
            SecurityContext securityContext = new SecurityContextImpl(authentication);
            String userId = jwtUtils.getClaimsFromJwtToken(token).getId(); // Получаем ID юзера
            exchange = exchange.mutate()
                    .request(r -> r.header("X-User-Id", userId))
                    .build(); // Добавляем ID юзера в заголовок запроса
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
        }

        return chain.filter(exchange);
    }
}
