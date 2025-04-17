package com.vladmikhayl.subscription.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth")
@Profile("!test") // чтобы в интеграционных тестах бин этого Feign-клиента не поднимался (там будет свой бин с замоканным ответом)
public interface AuthClient {

    @GetMapping("/internal/auth/{userId}/get-login")
    ResponseEntity<String> getUserLogin(
            @PathVariable Long userId
    );

}
