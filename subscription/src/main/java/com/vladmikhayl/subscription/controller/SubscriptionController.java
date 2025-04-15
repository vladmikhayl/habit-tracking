package com.vladmikhayl.subscription.controller;

import com.vladmikhayl.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{habitId}/send-subscription-request")
    public ResponseEntity<Void> sendSubscriptionRequest(
            @PathVariable Long habitId,
            @RequestHeader("X-User-Id") String userId
    ) {
        subscriptionService.sendSubscriptionRequest(habitId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
