package com.vladmikhayl.subscription.controller;

import com.vladmikhayl.subscription.dto.response.AcceptedSubscriptionForCreatorResponse;
import com.vladmikhayl.subscription.dto.response.AcceptedSubscriptionForSubscriberResponse;
import com.vladmikhayl.subscription.dto.response.UnprocessedRequestForCreatorResponse;
import com.vladmikhayl.subscription.dto.response.UnprocessedRequestForSubscriberResponse;
import com.vladmikhayl.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth") // показываем, что для этих эндпоинтов нужен JWT (для Сваггера)
@Tag(name = "Подписки на привычки", description = "Эндпоинты для работы с подписками на привычки")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело", content = @Content),
        @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
})
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{habitId}/send-subscription-request")
    @Operation(summary = "Отправить заявку на подписку на указанную привычку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заявка создана"),
            @ApiResponse(responseCode = "404", description = "Не найдена какая-либо сущность"),
            @ApiResponse(responseCode = "409", description = "У пользователя уже есть подписка/заявка на эту привычку")
    })
    public ResponseEntity<Void> sendSubscriptionRequest(
            @PathVariable @Parameter(description = "ID привычки", example = "7") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        subscriptionService.sendSubscriptionRequest(habitId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{subscriptionId}/accept")
    @Operation(summary = "Принять заявку на подписку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заявка принята"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой заявке " +
                    "(он не является создателем привычки, на которую отправлена эта заявка)"),
            @ApiResponse(responseCode = "404", description = "Не найдена какая-либо сущность"),
            @ApiResponse(responseCode = "409", description = "Эта заявка уже принята"),
            @ApiResponse(responseCode = "502", description = "Микросервис, на который делается внутренний запрос, вернул ошибку", content = @Content),
            @ApiResponse(responseCode = "503", description = "Недоступен микросервис, на который делается внутренний запрос", content = @Content)
    })
    public ResponseEntity<Void> acceptSubscriptionRequest(
            @PathVariable @Parameter(description = "ID заявки", example = "10") Long subscriptionId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        subscriptionService.acceptSubscriptionRequest(subscriptionId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{subscriptionId}/deny")
    @Operation(summary = "Отклонить заявку на подписку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заявка отклонена"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой заявке " +
                    "(он не является создателем привычки, на которую отправлена эта заявка)"),
            @ApiResponse(responseCode = "404", description = "Не найдена какая-либо сущность")
    })
    public ResponseEntity<Void> denySubscriptionRequest(
            @PathVariable @Parameter(description = "ID заявки", example = "10") Long subscriptionId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        subscriptionService.denySubscriptionRequest(subscriptionId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{habitId}/unsubscribe")
    @Operation(summary = "Отписаться от указанной привычки (или удалить заявку на нее)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Подписка/заявка удалена"),
            @ApiResponse(responseCode = "404", description = "Не найдена какая-либо сущность")
    })
    public ResponseEntity<Void> unsubscribe(
            @PathVariable @Parameter(description = "ID привычки", example = "7") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        subscriptionService.unsubscribe(habitId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/get-user-unprocessed-requests")
    @Operation(summary = "Просмотреть заявки пользователя (сделавшего этот запрос), " +
            "которые еще не были обработаны (приняты или отклонены)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация")
    })
    public ResponseEntity<List<UnprocessedRequestForSubscriberResponse>> getUserUnprocessedRequests(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        List<UnprocessedRequestForSubscriberResponse> response = subscriptionService.getUserUnprocessedRequests(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{habitId}/get-habit-unprocessed-requests")
    @Operation(summary = "Просмотреть заявки на указанную привычку, которые еще не были обработаны (приняты или отклонены)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке " +
                    "(он не является ее создателем)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Не найдена какая-либо сущность", content = @Content),
            @ApiResponse(responseCode = "502", description = "Микросервис, на который делается внутренний запрос, вернул ошибку", content = @Content),
            @ApiResponse(responseCode = "503", description = "Недоступен микросервис, на который делается внутренний запрос", content = @Content)
    })
    public ResponseEntity<List<UnprocessedRequestForCreatorResponse>> getHabitUnprocessedRequests(
            @PathVariable @Parameter(description = "ID привычки", example = "7") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        List<UnprocessedRequestForCreatorResponse> response = subscriptionService.getHabitUnprocessedRequests(habitId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-user-accepted-subscriptions")
    @Operation(summary = "Просмотреть принятые подписки пользователя (сделавшего этот запрос)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация")
    })
    public ResponseEntity<List<AcceptedSubscriptionForSubscriberResponse>> getUserAcceptedSubscriptions(
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        List<AcceptedSubscriptionForSubscriberResponse> response = subscriptionService.getUserAcceptedSubscriptions(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{habitId}/get-habit-accepted-subscriptions")
    @Operation(summary = "Просмотреть принятых подписчиков на указанную привычку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке " +
                    "(он не является ее создателем)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Не найдена какая-либо сущность", content = @Content),
            @ApiResponse(responseCode = "502", description = "Микросервис, на который делается внутренний запрос, вернул ошибку", content = @Content),
            @ApiResponse(responseCode = "503", description = "Недоступен микросервис, на который делается внутренний запрос", content = @Content)
    })
    public ResponseEntity<List<AcceptedSubscriptionForCreatorResponse>> getHabitAcceptedSubscriptions(
            @PathVariable @Parameter(description = "ID привычки", example = "7") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        List<AcceptedSubscriptionForCreatorResponse> response = subscriptionService.getHabitAcceptedSubscriptions(habitId, userId);
        return ResponseEntity.ok(response);
    }

}
