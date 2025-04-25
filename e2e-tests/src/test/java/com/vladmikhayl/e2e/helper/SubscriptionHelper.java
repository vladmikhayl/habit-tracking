package com.vladmikhayl.e2e.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.e2e.dto.subscription.AcceptedSubscriptionForCreatorResponse;
import com.vladmikhayl.e2e.dto.subscription.AcceptedSubscriptionForSubscriberResponse;
import com.vladmikhayl.e2e.dto.subscription.UnprocessedRequestForCreatorResponse;
import com.vladmikhayl.e2e.dto.subscription.UnprocessedRequestForSubscriberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RequiredArgsConstructor
public class SubscriptionHelper {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final ObjectMapper objectMapper;

    public void sendSubscriptionRequest(
            String token,
            Long habitId
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/subscriptions/" + habitId + "/send-subscription-request";

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class
        );
    }

    public void denySubscriptionRequest(
            String token,
            Long subscriptionId
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/subscriptions/" + subscriptionId + "/deny";

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );
    }

    public void acceptSubscriptionRequest(
            String token,
            Long subscriptionId
    ) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/subscriptions/" + subscriptionId + "/accept";

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(headers),
                Void.class
        );

        Thread.sleep(500);
    }

    public void unsubscribe(
            String token,
            Long habitId
    ) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/subscriptions/" + habitId + "/unsubscribe";

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );

        Thread.sleep(500);
    }

    public List<UnprocessedRequestForSubscriberResponse> getUserUnprocessedRequests(
            String token
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/subscriptions/get-user-unprocessed-requests";

        ResponseEntity<List<UnprocessedRequestForSubscriberResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public List<UnprocessedRequestForCreatorResponse> getHabitUnprocessedRequests(
            String token,
            Long habitId
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/subscriptions/" + habitId + "/get-habit-unprocessed-requests";

        ResponseEntity<List<UnprocessedRequestForCreatorResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public List<AcceptedSubscriptionForSubscriberResponse> getUserAcceptedSubscriptions(
            String token
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/subscriptions/get-user-accepted-subscriptions";

        ResponseEntity<List<AcceptedSubscriptionForSubscriberResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public List<AcceptedSubscriptionForCreatorResponse> getHabitAcceptedSubscriptions(
            String token,
            Long habitId
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = gatewayUrl + "/subscriptions/" + habitId + "/get-habit-accepted-subscriptions";

        ResponseEntity<List<AcceptedSubscriptionForCreatorResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
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
