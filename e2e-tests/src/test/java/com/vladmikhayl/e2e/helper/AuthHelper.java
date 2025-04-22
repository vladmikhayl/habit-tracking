package com.vladmikhayl.e2e.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.e2e.dto.auth.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RequiredArgsConstructor
public class AuthHelper {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final ObjectMapper objectMapper;

    public void register(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = gatewayUrl + "/auth/register";

        Map<String, String> requestBody = Map.of(
                "username", username,
                "password", password
        );

        HttpEntity<String> entity = buildEntity(requestBody, headers);
        restTemplate.postForEntity(url, entity, Void.class);
    }

    public AuthResponse login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = gatewayUrl + "/auth/login";

        Map<String, String> requestBody = Map.of(
                "username", username,
                "password", password
        );

        HttpEntity<String> entity = buildEntity(requestBody, headers);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(url, entity, AuthResponse.class);

        return response.getBody();
    }

    private <T> HttpEntity<String> buildEntity(T body, HttpHeaders headers) {
        try {
            String json = objectMapper.writeValueAsString(body);
            return new HttpEntity<>(json, headers);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request body");
        }
    }

}
