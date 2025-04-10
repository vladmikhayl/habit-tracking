package com.vladmikhayl.habit;

import com.vladmikhayl.habit.service.feign.ReportClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class FeignClientTestConfig {

    // Это бин, который создает замоканный ReportClient, который будет использоваться в интеграционных тестах
    // (вместо бина с реальным Feign-клиентом)
    @Bean
    public ReportClient reportClient() {
        return Mockito.mock(ReportClient.class);
    }

}
