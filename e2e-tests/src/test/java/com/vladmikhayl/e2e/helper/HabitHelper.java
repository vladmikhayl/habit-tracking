package com.vladmikhayl.e2e.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.e2e.dto.habit.HabitCreationRequest;
import com.vladmikhayl.e2e.dto.habit.HabitEditingRequest;
import com.vladmikhayl.e2e.dto.habit.HabitGeneralInfoResponse;
import com.vladmikhayl.e2e.dto.habit.HabitShortInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RequiredArgsConstructor
public class HabitHelper {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final ObjectMapper objectMapper;

    public void createHabit(
            String token,
            HabitCreationRequest request
    ) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/habits/create";

        HttpEntity<String> entity = buildEntity(request, headers);
        restTemplate.postForEntity(url, entity, Void.class);

        Thread.sleep(500);
    }

    public void editHabit(
            String token,
            Long habitId,
            HabitEditingRequest request
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/habits/" + habitId + "/edit";

        HttpEntity<String> entity = buildEntity(request, headers);

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

    public void deleteHabit(
            String token,
            Long habitId
    ) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = gatewayUrl + "/habits/" + habitId + "/delete";

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                HabitGeneralInfoResponse.class
        );

        Thread.sleep(500);
    }

    public HabitGeneralInfoResponse getGeneralInfo(
            String token,
            Long habitId
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = gatewayUrl + "/habits/" + habitId + "/general-info";

        ResponseEntity<HabitGeneralInfoResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                HabitGeneralInfoResponse.class
        );

        return response.getBody();
    }

    public List<HabitShortInfoResponse> getAllUserHabitsAtDay(
            String token,
            String dateStr
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = gatewayUrl + "/habits/all-user-habits/at-day/" + dateStr;

        ResponseEntity<List<HabitShortInfoResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

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
