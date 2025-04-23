package com.vladmikhayl.e2e.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.e2e.dto.habit.HabitGeneralInfoResponse;
import com.vladmikhayl.e2e.dto.report.ReportCreationRequest;
import com.vladmikhayl.e2e.dto.report.ReportPhotoEditingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class ReportHelper {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final ObjectMapper objectMapper;

    public void createReport(
            String token,
            ReportCreationRequest request
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/reports/create";

        HttpEntity<String> entity = buildEntity(request, headers);
        restTemplate.postForEntity(url, entity, Void.class);
    }

    public void changeReportPhoto(
            String token,
            Long reportId,
            ReportPhotoEditingRequest request
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/reports/" + reportId + "/change-photo";

        HttpEntity<String> entity = buildEntity(request, headers);

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Void.class
        );
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
