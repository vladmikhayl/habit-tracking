package com.vladmikhayl.auth.controller;

import com.vladmikhayl.auth.dto.AuthResponse;
import com.vladmikhayl.auth.dto.LoginRequest;
import com.vladmikhayl.auth.dto.RegisterRequest;
import com.vladmikhayl.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Эндпоинты для работы с пользователями")
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(
            summary = "Аутентифицироваться",
            description = "Проверяется введенный логин и пароль, и при верных данных выдается JWT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело", content = @Content),
            @ApiResponse(responseCode = "401", description = "Такой пользователь не существует", content = @Content)
    })
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest loginRequest
    ) {
        return ResponseEntity.ok(userService.login(loginRequest));
    }

    @PostMapping("/register")
    @Operation(summary = "Зарегистрироваться")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Успешная регистрация"),
            @ApiResponse(responseCode = "400", description = "Переданы некорректные параметры или тело"),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким логином уже существует")
    })
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        userService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
