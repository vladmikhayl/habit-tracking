package com.vladmikhayl.habit.controller;

import com.vladmikhayl.habit.dto.request.HabitCreationRequest;
import com.vladmikhayl.habit.dto.request.HabitEditingRequest;
import com.vladmikhayl.habit.dto.response.*;
import com.vladmikhayl.habit.service.HabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/habits")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth") // показываем, что для этих эндпоинтов нужен JWT (для Сваггера)
@Tag(name = "Привычки", description = "Эндпоинты для работы привычками")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело", content = @Content),
        @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
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
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке " +
                    "(он не является ее создателем)"),
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
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке " +
                    "(он не является ее создателем)")
    })
    public ResponseEntity<Void> deleteHabit(
            @PathVariable @Parameter(description = "ID удаляемой привычки", example = "1") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        habitService.deleteHabit(habitId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{habitId}/general-info")
    @Operation(summary = "Просмотреть подробную информацию об общем состоянии конкретной привычки")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке " +
                    "(он не является ее создателем или принятым подписчиком на нее)", content = @Content)
    })
    public ResponseEntity<HabitGeneralInfoResponse> getGeneralInfo(
            @PathVariable @Parameter(description = "ID привычки", example = "1") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        HabitGeneralInfoResponse response = habitService.getGeneralInfo(habitId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{habitId}/reports-info")
    @Operation(summary = "Просмотреть о конкретной привычке подробную информацию, связанную с выполнением этой привычки и отчетами о ней")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке " +
                    "(он не является ее создателем или принятым подписчиком на нее)", content = @Content)
    })
    public ResponseEntity<HabitReportsInfoResponse> getReportsInfo(
            @PathVariable @Parameter(description = "ID привычки", example = "1") Long habitId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        HabitReportsInfoResponse response = habitService.getReportsInfo(habitId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("{habitId}/get-report/at-day/{date}")
    @Operation(summary = "Просмотреть отчет о выполнении указанной привычки за конкретный день")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этой привычке " +
                    "(он не является ее создателем или принятым подписчиком на нее)", content = @Content)
    })
    public ResponseEntity<ReportFullInfoResponse> getReportAtDay(
            @PathVariable @Parameter(description = "ID привычки", example = "1") Long habitId,
            @PathVariable @Parameter(description = "За какую дату нужно вернуть отчет", example = "2025-04-11")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        ReportFullInfoResponse response = habitService.getReportAtDay(habitId, date, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("all-user-habits/at-day/{date}")
    @Operation(summary = "Просмотреть список всех привычек пользователя (сделавшего этот запрос), " +
            "которые являются текущими в конкретный день")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация")
    })
    public ResponseEntity<List<HabitShortInfoResponse>> getAllUserHabitsAtDay(
            @PathVariable @Parameter(description = "За какую дату нужно вернуть привычки", example = "2025-04-11")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        List<HabitShortInfoResponse> response = habitService.getAllUserHabitsAtDay(date, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("all-user-subscribed-habits/at-day/{date}")
    @Operation(summary = "Просмотреть список всех привычек, на которые подписан и принят пользователь " +
            "(сделавший этот запрос) и которые являются текущими в конкретный день")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно получена информация")
    })
    public ResponseEntity<List<SubscribedHabitShortInfoResponse>> getAllUserSubscribedHabitsAtDay(
            @PathVariable @Parameter(description = "За какую дату нужно вернуть привычки", example = "2025-04-11")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        List<SubscribedHabitShortInfoResponse> response = habitService.getAllUserSubscribedHabitsAtDay(date, userId);
        return ResponseEntity.ok(response);
    }

}
