package com.vladmikhayl.subscription.controller;

import com.vladmikhayl.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth") // показываем, что для этих эндпоинтов нужен JWT (для Сваггера)
@Tag(name = "Подписки на привычки", description = "Эндпоинты для работы с подписками на привычки")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело"),
        @ApiResponse(responseCode = "401", description = "Передан некорректный JWT")
})
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{habitId}/send-subscription-request")
    @Operation(summary = "Отправить заявку на подписку на указанную привычку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заявка создана"),
            @ApiResponse(responseCode = "404", description = "Такой привычки не существует"),
            @ApiResponse(responseCode = "409", description = "У пользователя уже есть подписка/заявка на эту привычку")
    })
    public ResponseEntity<Void> sendSubscriptionRequest(
            @PathVariable @Parameter(description = "ID привычки", example = "7") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        subscriptionService.sendSubscriptionRequest(habitId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{subscriptionId}/accept")
    @Operation(summary = "Принять заявку на подписку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заявка принята"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой заявке " +
                    "(он не является создателем привычки, на которую отправлена эта заявка)"),
            @ApiResponse(responseCode = "404", description = "Либо такой заявки не существует, " +
                    "либо не найден создавший привычку пользователь"),
            @ApiResponse(responseCode = "409", description = "Эта заявка уже принята")
    })
    public ResponseEntity<Void> acceptSubscriptionRequest(
            @PathVariable @Parameter(description = "ID заявки", example = "10") Long subscriptionId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        subscriptionService.acceptSubscriptionRequest(subscriptionId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
