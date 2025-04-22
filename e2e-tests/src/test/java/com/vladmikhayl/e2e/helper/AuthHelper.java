package com.vladmikhayl.e2e.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.e2e.dto.auth.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RequiredArgsConstructor
public class AuthHelper {

    private final RestTemplate restTemplate;
    private final HttpHeaders headers;
    private final String gatewayUrl;
    private final ObjectMapper objectMapper;

    public void register(String username, String password) {
        String url = gatewayUrl + "/auth/register";

        Map<String, String> requestBody = Map.of(
                "username", username,
                "password", password
        );

        HttpEntity<String> entity = buildEntity(requestBody);
        restTemplate.postForEntity(url, entity, Void.class);
    }

    public AuthResponse login(String username, String password) {
        String url = gatewayUrl + "/auth/login";

        Map<String, String> requestBody = Map.of(
                "username", username,
                "password", password
        );

        HttpEntity<String> entity = buildEntity(requestBody);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url, entity, AuthResponse.class);

        return response.getBody();
    }

    private HttpEntity<String> buildEntity(Map<String, String> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            return new HttpEntity<>(json, headers);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request body");
        }
    }

}
