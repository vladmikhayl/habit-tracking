package com.vladmikhayl.subscription.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gateway")
@Profile("!test") // чтобы в интеграционных тестах бин этого Feign-клиента не поднимался (там будет свой бин с замоканным ответом)
public interface AuthClient {

    @GetMapping("/internal/auth/{userId}/get-login")
    ResponseEntity<String> getUserLogin(
            @RequestHeader("X-Internal-Token") String internalToken,
            @PathVariable Long userId
    );

}
