package com.vladmikhayl.habit.controller;

import com.vladmikhayl.habit.dto.HabitCreationRequest;
import com.vladmikhayl.habit.dto.HabitEditingRequest;
import com.vladmikhayl.habit.service.HabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/habits")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth") // показываем, что для этих эндпоинтов нужен JWT (для Сваггера)
@Tag(name = "Привычки", description = "Эндпоинты для работы привычками")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело"),
        @ApiResponse(responseCode = "401", description = "Передан некорректный JWT")
})
public class HabitController {

    private final HabitService habitService;

    @PostMapping("/create")
    @Operation(summary = "Создать привычку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Успешное создание"),
            @ApiResponse(responseCode = "409", description = "У пользователя уже есть привычка с таким названием")
    })
    public ResponseEntity<Void> createHabit(
            @Valid @RequestBody HabitCreationRequest habitCreationRequest,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        habitService.createHabit(habitCreationRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{habitId}/edit")
    @Operation(summary = "Изменить существующую привычку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное изменение"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке"),
            @ApiResponse(responseCode = "409", description = "У пользователя уже есть привычка с таким названием")
    })
    public ResponseEntity<Void> editHabit(
            @PathVariable @Parameter(description = "ID изменяемой привычки", example = "1") Long habitId,
            @Valid @RequestBody HabitEditingRequest habitEditingRequest,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        habitService.editHabit(habitId, habitEditingRequest, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{habitId}/delete")
    @Operation(summary = "Удалить существующую привычку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное удаление"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке")
    })
    public ResponseEntity<Void> deleteHabit(
            @PathVariable @Parameter(description = "ID удаляемой привычки", example = "1") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        habitService.deleteHabit(habitId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
