package com.vladmikhayl.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vladmikhayl.e2e.helper.AuthHelper;
import com.vladmikhayl.e2e.helper.HabitHelper;
import com.vladmikhayl.e2e.helper.ReportHelper;
import com.vladmikhayl.e2e.helper.SubscriptionHelper;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;


public abstract class BaseE2ETest {

    protected static final String TODAY_DATE_STR = LocalDate.now().toString();
    protected static final String TOMORROW_DATE_STR = LocalDate.now().plusDays(1).toString();
    protected static final String YESTERDAY_DATE_STR = LocalDate.now().minusDays(1).toString();

    protected final String gatewayUrl = "http://localhost:8080/api/v1";

    protected final RestTemplate restTemplate = new RestTemplate();
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final AuthHelper authHelper = new AuthHelper(restTemplate, gatewayUrl, objectMapper);
    protected final HabitHelper habitHelper = new HabitHelper(restTemplate, gatewayUrl, objectMapper);
    protected final ReportHelper reportHelper = new ReportHelper(restTemplate, gatewayUrl, objectMapper);
    protected final SubscriptionHelper subscriptionHelper = new SubscriptionHelper(restTemplate, gatewayUrl, objectMapper);

    {
        objectMapper.registerModule(new JavaTimeModule());
    }

}
