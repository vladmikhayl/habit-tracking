package com.vladmikhayl.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.e2e.helper.AuthHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;


public abstract class BaseE2ETest {

    protected final String gatewayUrl = "http://localhost:8080/api/v1";

    protected final RestTemplate restTemplate = new RestTemplate();
    protected final HttpHeaders headers = new HttpHeaders();
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final AuthHelper authHelper = new AuthHelper(restTemplate, headers, gatewayUrl, objectMapper);

    {
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

}
