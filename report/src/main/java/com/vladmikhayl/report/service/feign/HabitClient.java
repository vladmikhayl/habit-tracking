package com.vladmikhayl.report.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "habit")
@Profile("!test") // чтобы в интеграционных тестах бин этого Feign-клиента не поднимался (там будет свой бин с замоканным ответом)
public interface HabitClient {

    @GetMapping("/internal/habits/{habitId}/is-current")
    ResponseEntity<Boolean> isCurrent(
            @PathVariable Long habitId,
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );

}
