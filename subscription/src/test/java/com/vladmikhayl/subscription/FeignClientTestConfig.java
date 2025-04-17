package com.vladmikhayl.subscription;

import com.vladmikhayl.subscription.service.feign.AuthClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class FeignClientTestConfig {

    // Это бин, который создает замоканный AuthClient, который будет использоваться в интеграционных тестах
    // (вместо бина с реальным Feign-клиентом)
    @Bean
    public AuthClient habitClient() {
        return Mockito.mock(AuthClient.class);
    }

}
