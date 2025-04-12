package com.vladmikhayl.habit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    // Этот бин указывает, что по умолчанию при вызове LocalDate.now(clock) в методах сервиса нужно возвращать текущую дату.
    // В тестах этот бин будет мокаться, чтобы зафиксировать "текущую" дату
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

}
