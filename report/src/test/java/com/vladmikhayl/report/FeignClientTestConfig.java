package com.vladmikhayl.report;

import com.vladmikhayl.report.service.feign.HabitClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class FeignClientTestConfig {

    // Это бин, который создан замоканный HabitClient, который будет использоваться в интеграционных тестах
    // (вместо бина с реальным Feign-клиентом)
    @Bean
    public HabitClient habitClient() {
        return Mockito.mock(HabitClient.class);
    }

}
