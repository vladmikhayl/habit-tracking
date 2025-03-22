package com.vladmikhayl.gateway.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Collections;

@Component
public class JwtUtils {

    @Value("${spring.app.jwtSecret}")
    private String secretKey;

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Authentication getAuthenticationFromJwtToken(String token) {
        Claims claims = getClaimsFromJwtToken(token);
        String username = claims.getSubject();
        return new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) { // TODO: добавить чтобы в логи писалась какая конкретно ошибка
            return false;
        }
    }

    public String getJwtTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
