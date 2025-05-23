package com.vladmikhayl.report.controller;

import com.vladmikhayl.report.dto.request.ReportCreationRequest;
import com.vladmikhayl.report.dto.request.ReportPhotoEditingRequest;
import com.vladmikhayl.report.service.FileUploadService;
import com.vladmikhayl.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth") // показываем, что для этих эндпоинтов нужен JWT (для Сваггера)
@Tag(name = "Отчеты о выполнении привычек", description = "Эндпоинты для работы с отчетами о выполнении привычек")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело", content = @Content),
        @ApiResponse(responseCode = "401", description = "Передан некорректный JWT", content = @Content)
})
public class ReportController {

    private final ReportService reportService;

    private final FileUploadService fileUploadService;

    @PostMapping("/upload-file")
    @Operation(
            summary = "Загрузить файл",
            description = "Загружает переданный файл в хранилище и возвращает ссылку на сохраненный объект"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная загрузка файла"),
    })
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        String fileUrl = fileUploadService.upload(file, UUID.randomUUID() + "-" + file.getOriginalFilename());
        return ResponseEntity.ok(fileUrl);
    }

    @PostMapping("/create")
    @Operation(
            summary = "Создать отчет",
            description = "Помечает переданную привычку в переданную дату выполненной"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Успешное создание"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к созданию отчетов к этой привычке в этот день " +
                    "(либо он не является создателем этой привычки, либо эта привычка не является текущей в переданный день)"),
            @ApiResponse(responseCode = "409", description = "Эта привычка уже отмечена выполненной в этот день"),
            @ApiResponse(responseCode = "502", description = "Микросервис, на который делается внутренний запрос, вернул ошибку", content = @Content),
            @ApiResponse(responseCode = "503", description = "Недоступен микросервис, на который делается внутренний запрос", content = @Content)
    })
    public ResponseEntity<Void> createReport(
            @Valid @RequestBody ReportCreationRequest request,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        reportService.createReport(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{reportId}/change-photo")
    @Operation(
            summary = "Изменить фото в отчете",
            description = "Меняет фото в уже существующем отчете на новое, или полностью удаляет прикрепленное " +
                    "к уже существующему отчету фото (для этого нужно передать пустую строку)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное изменение"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этому отчету " +
                    "(он не является его создателем)")
    })
    public ResponseEntity<Void> changeReportPhoto(
            @PathVariable @Parameter(description = "ID изменяемого отчета", example = "1") Long reportId,
            @RequestBody ReportPhotoEditingRequest request,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        reportService.changeReportPhoto(reportId, request, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{reportId}/delete")
    @Operation(
            summary = "Удалить отчет",
            description = "Удаляет поставленную отметку о выполнении привычки"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное удаление"),
            @ApiResponse(responseCode = "403", description = "Пользователь не имеет доступа к этому отчету " +
                    "(он не является его создателем)")
    })
    public ResponseEntity<Void> deleteReport(
            @PathVariable @Parameter(description = "ID удаляемого отчета", example = "1") Long reportId,
            @RequestHeader("X-User-Id") @Parameter(hidden = true) String userId
    ) {
        reportService.deleteReport(reportId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
